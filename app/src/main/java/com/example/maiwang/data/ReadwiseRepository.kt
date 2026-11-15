package com.example.maiwang.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class ReadwiseHighlight(
  val text: String,
  val title: String? = null,
  val author: String? = null,
  val image_url: String? = null,
  val source_url: String? = null,
  val source_type: String? = "maiwang",
  val category: String? = "books",
  val note: String? = null,
  val highlighted_at: String? = null,
  val location: Int? = null,
  val location_type: String? = null
)

@Serializable
data class ReadwiseBook(
  val id: Int,
  val title: String,
  val author: String? = null,
  val category: String,
  val source: String? = null,
  val num_highlights: Int,
  val cover_image_url: String? = null,
  val last_highlight_at: String? = null,
  val updated: String? = null,
  // Helper to store local cover path if cached
  var local_cover_path: String? = null
)

@Serializable
data class ReadwiseBooksResponse(
  val count: Int,
  val next: String? = null,
  val previous: String? = null,
  val results: List<ReadwiseBook>
)

@Serializable
data class ReadwiseHighlightItem(
  val id: Int,
  val text: String,
  val note: String? = null,
  val location: Int? = null,
  val location_type: String? = null,
  val book_id: Int,
  val highlighted_at: String? = null,
  val updated: String? = null
)

@Serializable
data class ReadwiseHighlightsResponse(
  val count: Int,
  val next: String? = null,
  val previous: String? = null,
  val results: List<ReadwiseHighlightItem>
)

@Serializable
data class ReadwiseSyncMetadata(
  val last_sync_time: String? = null
)

// --- Export API Specific Response Models ---
@Serializable
data class ReadwiseExportHighlight(
  val id: Int,
  val text: String,
  val note: String? = null,
  val location: Int? = null,
  val location_type: String? = null,
  val highlighted_at: String? = null,
  val updated: String? = null,
  val is_deleted: Boolean? = null
)

@Serializable
data class ReadwiseExportBook(
  val user_book_id: Int,
  val title: String,
  val author: String? = null,
  val category: String,
  val source: String? = null,
  val cover_image_url: String? = null,
  val last_highlight_at: String? = null,
  val updated: String? = null,
  val highlights: List<ReadwiseExportHighlight> = emptyList()
)

@Serializable
data class ReadwiseExportResponse(
  val count: Int,
  val nextPageCursor: JsonElement? = null,
  val results: List<ReadwiseExportBook>
)

class ReadwiseRepository(private val context: Context) {
  private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
  }
  private val syncMutex = Mutex()

  // --- Metadata helpers ---
  fun getSyncMetadata(): ReadwiseSyncMetadata {
    val file = File(context.filesDir, "readwise_sync_metadata.json")
    if (!file.exists()) return ReadwiseSyncMetadata()
    return try {
      json.decodeFromString<ReadwiseSyncMetadata>(file.readText())
    } catch (e: Exception) {
      e.printStackTrace()
      ReadwiseSyncMetadata()
    }
  }

  fun saveSyncMetadata(metadata: ReadwiseSyncMetadata) {
    val file = File(context.filesDir, "readwise_sync_metadata.json")
    try {
      file.writeText(json.encodeToString(metadata))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // --- Local cached data readers ---
  fun getBooksLocal(): List<ReadwiseBook> {
    val file = File(context.filesDir, "readwise_all_books.json")
    if (!file.exists()) return emptyList()
    return try {
      json.decodeFromString<List<ReadwiseBook>>(file.readText())
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  fun getHighlightsLocal(): List<ReadwiseHighlightItem> {
    val file = File(context.filesDir, "readwise_all_highlights.json")
    if (!file.exists()) return emptyList()
    return try {
      json.decodeFromString<List<ReadwiseHighlightItem>>(file.readText())
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  // --- Export API Sync Engine ---
  suspend fun syncExportData(token: String, forceAll: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
    syncMutex.withLock {
      try {
        val metadata = getSyncMetadata()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
          timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val lastSyncTime = if (forceAll) null else {
          metadata.last_sync_time?.let { timeStr ->
            try {
              val date = sdf.parse(timeStr)
              if (date != null) {
                val bufferedTime = date.time - 10 * 60 * 1000 // subtract 10 minutes buffer
                sdf.format(java.util.Date(bufferedTime))
              } else {
                timeStr
              }
            } catch (e: Exception) {
              timeStr
            }
          }
        }

        val fetchedBooks = mutableListOf<ReadwiseBook>()
        val fetchedHighlights = mutableListOf<ReadwiseHighlightItem>()
        val deletedHighlightIds = mutableSetOf<Int>()

        var pageCursor: String? = null
        var hasMore = true
        var pageCount = 0
        val maxPages = 250 // High safeguard limit: 25,000 books with nested highlights

        while (hasMore && pageCount < maxPages) {
          val builder = java.lang.StringBuilder("https://readwise.io/api/v2/export/?page_size=100&includeDeleted=true")
          if (!lastSyncTime.isNullOrEmpty()) {
            builder.append("&updatedAfter=").append(java.net.URLEncoder.encode(lastSyncTime, "UTF-8"))
          }
          if (!pageCursor.isNullOrEmpty()) {
            builder.append("&pageCursor=").append(java.net.URLEncoder.encode(pageCursor, "UTF-8"))
          }

          val url = URL(builder.toString())
          val conn = url.openConnection() as HttpURLConnection
          conn.requestMethod = "GET"
          conn.setRequestProperty("Authorization", "Token $token")
          conn.connectTimeout = 15000
          conn.readTimeout = 15000

          if (conn.responseCode == 200) {
            val resText = conn.inputStream.bufferedReader().use { it.readText() }
            val response = json.decodeFromString<ReadwiseExportResponse>(resText)

            response.results.forEach { exportBook ->
              val book = ReadwiseBook(
                id = exportBook.user_book_id,
                title = exportBook.title,
                author = exportBook.author,
                category = exportBook.category,
                source = exportBook.source,
                num_highlights = exportBook.highlights.size,
                cover_image_url = exportBook.cover_image_url,
                last_highlight_at = exportBook.last_highlight_at,
                updated = exportBook.updated
              )
              fetchedBooks.add(book)

              exportBook.highlights.forEach { exportHl ->
                if (exportHl.is_deleted == true) {
                  deletedHighlightIds.add(exportHl.id)
                } else {
                  val hl = ReadwiseHighlightItem(
                    id = exportHl.id,
                    text = exportHl.text,
                    note = exportHl.note,
                    location = exportHl.location,
                    location_type = exportHl.location_type,
                    book_id = exportBook.user_book_id,
                    highlighted_at = exportHl.highlighted_at,
                    updated = exportHl.updated
                  )
                  fetchedHighlights.add(hl)
                }
              }
            }

            pageCursor = response.nextPageCursor?.jsonPrimitive?.contentOrNull
            hasMore = !pageCursor.isNullOrEmpty()
            pageCount++

            if (hasMore) {
              kotlinx.coroutines.delay(250) // Polite rate-limiting delay
            }
          } else {
            val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP 錯誤: ${conn.responseCode}"
            return@withContext Result.failure(Exception(errorMsg))
          }
        }

        // Merge Books
        val localBooks = getBooksLocal()
        val mergedBooksMap = localBooks.associateBy { it.id }.toMutableMap()
        if (forceAll) {
          val fetchedBookIds = fetchedBooks.map { it.id }.toSet()
          mergedBooksMap.keys.retainAll(fetchedBookIds)
        }
        fetchedBooks.forEach { book ->
          val existing = mergedBooksMap[book.id]
          if (existing != null) {
            val mergedBook = book.copy(
              local_cover_path = existing.local_cover_path,
              num_highlights = if (book.num_highlights > 0) book.num_highlights else existing.num_highlights
            )
            mergedBooksMap[book.id] = mergedBook
          } else {
            mergedBooksMap[book.id] = book
          }
        }
        val finalBooksList = mergedBooksMap.values.sortedByDescending { it.last_highlight_at ?: it.updated ?: "" }

        // Merge Highlights
        val localHighlights = getHighlightsLocal()
        val mergedHighlightsMap = localHighlights.associateBy { it.id }.toMutableMap()
        if (forceAll) {
          val fetchedHighlightIds = fetchedHighlights.map { it.id }.toSet()
          mergedHighlightsMap.keys.retainAll(fetchedHighlightIds)
        }
        // Remove any that are marked deleted in the fetched payload
        deletedHighlightIds.forEach { id ->
          mergedHighlightsMap.remove(id)
        }
        fetchedHighlights.forEach { item ->
          mergedHighlightsMap[item.id] = item
        }
        val finalHighlightsList = mergedHighlightsMap.values.sortedByDescending { it.highlighted_at ?: it.updated ?: "" }

        // Recount highlights to keep book.num_highlights perfectly accurate
        val highlightsCountByBook = finalHighlightsList.groupBy { it.book_id }.mapValues { it.value.size }
        val updatedBooksList = finalBooksList.map { book ->
          val actualCount = highlightsCountByBook[book.id] ?: 0
          book.copy(num_highlights = actualCount)
        }.filter { it.num_highlights > 0 }

        // Save books to JSON
        val booksFile = File(context.filesDir, "readwise_all_books.json")
        booksFile.writeText(json.encodeToString(updatedBooksList))

        // Save highlights to JSON
        val highlightsFile = File(context.filesDir, "readwise_all_highlights.json")
        highlightsFile.writeText(json.encodeToString(finalHighlightsList))

        // Only update sync time if we fetched all available pages
        if (!hasMore) {
          val nowStr = sdf.format(java.util.Date())
          saveSyncMetadata(ReadwiseSyncMetadata(last_sync_time = nowStr))
        }

        Result.success(Unit)
      } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
      }
    }
  }

  // --- Public APIs matching previous signatures but using offline-first + sync ---
  suspend fun getBooks(token: String, category: String? = null, forceRefresh: Boolean = false): Result<List<ReadwiseBook>> = withContext(Dispatchers.IO) {
    val localBooks = getBooksLocal()

    if (!forceRefresh && localBooks.isNotEmpty()) {
      val filtered = if (category.isNullOrEmpty()) {
        localBooks
      } else {
        localBooks.filter { it.category.equals(category, ignoreCase = true) }
      }
      return@withContext Result.success(filtered)
    }

    val forceAll = localBooks.isEmpty()
    val syncResult = syncExportData(token, forceAll = forceAll)
    if (syncResult.isSuccess) {
      val allBooks = getBooksLocal()
      val filtered = if (category.isNullOrEmpty()) {
        allBooks
      } else {
        allBooks.filter { it.category.equals(category, ignoreCase = true) }
      }
      Result.success(filtered)
    } else {
      if (localBooks.isNotEmpty()) {
        val filtered = if (category.isNullOrEmpty()) {
          localBooks
        } else {
          localBooks.filter { it.category.equals(category, ignoreCase = true) }
        }
        Result.success(filtered)
      } else {
        Result.failure(syncResult.exceptionOrNull() ?: Exception("Sync failed"))
      }
    }
  }

  suspend fun syncBookHighlights(token: String, bookId: Int): Result<Unit> = withContext(Dispatchers.IO) {
    syncMutex.withLock {
      try {
        val fetchedHighlights = mutableListOf<ReadwiseHighlightItem>()
        var nextPageUrl: String? = "https://readwise.io/api/v2/highlights/?book_id=$bookId&page_size=100"
        var pageCount = 0

        while (!nextPageUrl.isNullOrEmpty() && pageCount < 50) {
          val url = URL(nextPageUrl)
          val conn = url.openConnection() as HttpURLConnection
          conn.requestMethod = "GET"
          conn.setRequestProperty("Authorization", "Token $token")
          conn.connectTimeout = 15000
          conn.readTimeout = 15000

          if (conn.responseCode == 200) {
            val resText = conn.inputStream.bufferedReader().use { it.readText() }
            val response = json.decodeFromString<ReadwiseHighlightsResponse>(resText)
            fetchedHighlights.addAll(response.results)
            nextPageUrl = response.next
            pageCount++
            if (!nextPageUrl.isNullOrEmpty()) {
              kotlinx.coroutines.delay(250)
            }
          } else {
            val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP 錯誤: ${conn.responseCode}"
            return@withContext Result.failure(Exception(errorMsg))
          }
        }

        val localHighlights = getHighlightsLocal()
        val mergedHighlightsMap = localHighlights.associateBy { it.id }.toMutableMap()
        
        val fetchedIds = fetchedHighlights.map { it.id }.toSet()
        val iterator = mergedHighlightsMap.entries.iterator()
        while (iterator.hasNext()) {
          val entry = iterator.next()
          if (entry.value.book_id == bookId && !fetchedIds.contains(entry.key)) {
            iterator.remove()
          }
        }

        fetchedHighlights.forEach { item ->
          mergedHighlightsMap[item.id] = item
        }

        val finalHighlightsList = mergedHighlightsMap.values.sortedByDescending { it.highlighted_at ?: it.updated ?: "" }

        val highlightsCountByBook = finalHighlightsList.groupBy { it.book_id }.mapValues { it.value.size }
        val localBooks = getBooksLocal()
        val updatedBooksList = localBooks.map { book ->
          if (book.id == bookId) {
            val actualCount = highlightsCountByBook[bookId] ?: 0
            book.copy(num_highlights = actualCount)
          } else {
            book
          }
        }.filter { it.num_highlights > 0 }

        val booksFile = File(context.filesDir, "readwise_all_books.json")
        booksFile.writeText(json.encodeToString(updatedBooksList))

        val highlightsFile = File(context.filesDir, "readwise_all_highlights.json")
        highlightsFile.writeText(json.encodeToString(finalHighlightsList))

        Result.success(Unit)
      } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
      }
    }
  }

  suspend fun getHighlights(token: String, bookId: Int, forceRefresh: Boolean = false): Result<List<ReadwiseHighlightItem>> = withContext(Dispatchers.IO) {
    val localHighlights = getHighlightsLocal()
    val bookHighlights = localHighlights.filter { it.book_id == bookId }

    if (!forceRefresh && bookHighlights.isNotEmpty()) {
      return@withContext Result.success(bookHighlights)
    }

    val syncResult = syncBookHighlights(token, bookId)
    if (syncResult.isSuccess) {
      val allHighlights = getHighlightsLocal()
      val filtered = allHighlights.filter { it.book_id == bookId }
      Result.success(filtered)
    } else {
      if (bookHighlights.isNotEmpty()) {
        Result.success(bookHighlights)
      } else {
        Result.failure(syncResult.exceptionOrNull() ?: Exception("Sync failed"))
      }
    }
  }

  // --- Highlight Creation Client POST with local-first cache update ---
  suspend fun createHighlight(token: String, highlight: ReadwiseHighlight): Result<ReadwiseHighlightItem> = withContext(Dispatchers.IO) {
    syncMutex.withLock {
      try {
        val url = URL("https://readwise.io/api/v2/highlights/")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Token $token")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        @Serializable
        data class ReadwisePostRequest(
          val highlights: List<ReadwiseHighlight>
        )

        val request = ReadwisePostRequest(listOf(highlight))
        val jsonBody = json.encodeToString(request)

        conn.outputStream.use { os ->
          val input = jsonBody.toByteArray(Charsets.UTF_8)
          os.write(input, 0, input.size)
        }

        val code = conn.responseCode
        if (code in 200..299) {
          val resText = conn.inputStream.bufferedReader().use { it.readText() }
          val createdList = json.decodeFromString<List<ReadwiseHighlightItem>>(resText)
          if (createdList.isEmpty()) {
            return@withContext Result.failure(Exception("Created highlight response was empty"))
          }
          val createdItem = createdList[0]

          // 1. Prepend to local highlights cache
          val localHighlights = getHighlightsLocal().toMutableList()
          localHighlights.add(0, createdItem)
          val highlightsFile = File(context.filesDir, "readwise_all_highlights.json")
          highlightsFile.writeText(json.encodeToString(localHighlights))

          // 2. Increment book highlights count in local books cache
          val localBooks = getBooksLocal().toMutableList()
          val bookIndex = localBooks.indexOfFirst { it.id == createdItem.book_id }
          if (bookIndex != -1) {
            val book = localBooks[bookIndex]
            localBooks[bookIndex] = book.copy(
              num_highlights = book.num_highlights + 1,
              last_highlight_at = createdItem.highlighted_at ?: book.last_highlight_at,
              updated = createdItem.updated ?: book.updated
            )
          } else {
            // Create a placeholder book if it doesn't exist locally
            val newBook = ReadwiseBook(
              id = createdItem.book_id,
              title = highlight.title ?: "未知書名",
              author = highlight.author,
              category = highlight.category ?: "books",
              source = highlight.source_type ?: "maiwang",
              num_highlights = 1,
              cover_image_url = highlight.image_url,
              last_highlight_at = createdItem.highlighted_at,
              updated = createdItem.updated
            )
            localBooks.add(0, newBook)
          }
          val sortedBooks = localBooks.sortedByDescending { it.last_highlight_at ?: it.updated ?: "" }
          val booksFile = File(context.filesDir, "readwise_all_books.json")
          booksFile.writeText(json.encodeToString(sortedBooks))

          Result.success(createdItem)
        } else {
          val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP 錯誤代碼: $code"
          Result.failure(Exception(errorMsg))
        }
      } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
      }
    }
  }

  // --- Highlight Update Client PATCH ---
  @Serializable
  private data class UpdateHighlightRequest(
    val text: String,
    val note: String? = null
  )

  suspend fun updateHighlight(token: String, highlightId: Int, text: String, note: String?): Result<ReadwiseHighlightItem> = withContext(Dispatchers.IO) {
    syncMutex.withLock {
      try {
        val url = URL("https://readwise.io/api/v2/highlights/$highlightId/")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PATCH"
        conn.setRequestProperty("Authorization", "Token $token")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val requestBody = UpdateHighlightRequest(text = text, note = note)
        val jsonBody = json.encodeToString(requestBody)

        conn.outputStream.use { os ->
          val input = jsonBody.toByteArray(Charsets.UTF_8)
          os.write(input, 0, input.size)
        }

        val code = conn.responseCode
        if (code == 200) {
          val resText = conn.inputStream.bufferedReader().use { it.readText() }
          val updatedItem = json.decodeFromString<ReadwiseHighlightItem>(resText)

          // Update local cache
          val localHighlights = getHighlightsLocal()
          val updatedList = localHighlights.map { item ->
            if (item.id == highlightId) {
              updatedItem
            } else {
              item
            }
          }
          val file = File(context.filesDir, "readwise_all_highlights.json")
          file.writeText(json.encodeToString(updatedList))

          Result.success(updatedItem)
        } else {
          val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP 錯誤: $code"
          Result.failure(Exception(errorMsg))
        }
      } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
      }
    }
  }

  suspend fun deleteHighlight(token: String, highlightId: Int): Result<Unit> = withContext(Dispatchers.IO) {
    syncMutex.withLock {
      try {
        val url = URL("https://readwise.io/api/v2/highlights/$highlightId/")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.setRequestProperty("Authorization", "Token $token")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val code = conn.responseCode
        if (code == 204 || code == 200) {
          // Remove from local cache
          val localHighlights = getHighlightsLocal()
          val updatedList = localHighlights.filter { it.id != highlightId }
          val file = File(context.filesDir, "readwise_all_highlights.json")
          file.writeText(json.encodeToString(updatedList))

          // Recount book highlights and update book
          val booksFile = File(context.filesDir, "readwise_all_books.json")
          if (booksFile.exists()) {
            val localBooks = getBooksLocal()
            val highlightsCountByBook = updatedList.groupBy { it.book_id }.mapValues { it.value.size }
            val updatedBooks = localBooks.map { book ->
              val actualCount = highlightsCountByBook[book.id] ?: 0
              book.copy(num_highlights = actualCount)
            }.filter { it.num_highlights > 0 }
            booksFile.writeText(json.encodeToString(updatedBooks))
          }

          Result.success(Unit)
        } else {
          val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP 錯誤: $code"
          Result.failure(Exception(errorMsg))
        }
      } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
      }
    }
  }

  // --- LRU Cover helper ---
  suspend fun getOrFetchLocalCover(bookId: Int, imageUrl: String?): String? = withContext(Dispatchers.IO) {
    if (imageUrl.isNullOrEmpty()) return@withContext null
    // If not a remote URL, return as is
    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
      return@withContext imageUrl
    }

    val coversDir = File(context.filesDir, "readwise_covers").apply { mkdirs() }
    val coverFile = File(coversDir, "$bookId.jpg")

    if (coverFile.exists()) {
      coverFile.setLastModified(System.currentTimeMillis())
      return@withContext coverFile.absolutePath
    }

    try {
      val url = URL(imageUrl)
      val conn = url.openConnection() as HttpURLConnection
      conn.connectTimeout = 5000
      conn.readTimeout = 5000
      if (conn.responseCode == 200) {
        conn.inputStream.use { input ->
          coverFile.outputStream().use { output ->
            input.copyTo(output)
          }
        }
        trimCoverCache(coversDir, 50)
        return@withContext coverFile.absolutePath
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return@withContext imageUrl
  }

  private fun trimCoverCache(coversDir: File, limit: Int) {
    val files = coversDir.listFiles() ?: return
    if (files.size > limit) {
      files.sortBy { it.lastModified() }
      val toDelete = files.size - limit
      for (i in 0 until toDelete) {
        files[i].delete()
      }
    }
  }
}
