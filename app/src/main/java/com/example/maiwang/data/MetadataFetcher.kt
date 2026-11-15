package com.example.maiwang.data

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import android.util.Log


@Serializable
data class MetadataRecord(
  val source_type: String,
  val title: String,
  val author: String?,
  val source_url: String,
  val category: String = "books"
)

@Serializable
data class UnifiedMetadata(
  val isbn: String,
  val image_url: String,
  val metadata_records: List<MetadataRecord>
)

@Serializable
data class PoemTitle(
  val Content: String? = null
)

@Serializable
data class PoemClause(
  val Content: String? = null
)

@Serializable
data class PoemDetail(
  val Id: Int? = null,
  val Dynasty: String? = null,
  val Author: String? = null,
  val Title: PoemTitle? = null,
  val Clauses: List<PoemClause>? = null
)

@Serializable
data class PoemContent(
  val Poem: PoemDetail? = null
)

@Serializable
data class PoemResponse(
  val Date: String? = null,
  val ContentType: String? = null,
  val Content: PoemContent? = null
)

object MetadataFetcher {

  private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

  suspend fun fetchRecommendedPoem(dateStr: String): Pair<String, String>? = withContext(Dispatchers.IO) {
    val url = "https://api.sou-yun.cn/api/RecommendedReading?type=poem&needHtml=true&index=-1&date=$dateStr"
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.connectTimeout = 3000
      connection.readTimeout = 3000

      if (connection.responseCode == 200) {
        val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val response = json.decodeFromString<PoemResponse>(responseText)
        val poem = response.Content?.Poem
        val clauses = poem?.Clauses
        if (clauses != null && clauses.size >= 2) {
          val clause1 = clauses[0].Content?.trim()?.removeSuffix("，")?.removeSuffix("。") ?: ""
          val clause2 = clauses[1].Content?.trim()?.removeSuffix("，")?.removeSuffix("。") ?: ""
          val title = poem.Title?.Content ?: ""
          if (clause1.isNotEmpty() && clause2.isNotEmpty()) {
            val text = "「$clause1，$clause2。」"
            val formattedTitle = "—— 《$title》"
            return@withContext Pair(text, formattedTitle)
          }
        }
      }
      null
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  private fun hexToBytes(hex: String): ByteArray {
    val len = hex.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
      data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
      i += 2
    }
    return data
  }

  fun getRandomPlaceholder(): String {
    val placeholders = listOf(
      "android.resource://com.example.maiwang/drawable/edition_placeholder",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_1",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_2",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_3",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_4",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_5",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_6",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_7",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_8",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_9",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_10",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_11",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_12",
      "android.resource://com.example.maiwang/drawable/edition_placeholder_13"
    )
    return placeholders.random()
  }

  private suspend fun fetchOpenLibraryCover(isbn: String): String? = withContext(Dispatchers.IO) {
    val url = "https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.connectTimeout = 3000
      connection.readTimeout = 3000
      connection.instanceFollowRedirects = true
      val code = connection.responseCode
      if (code == 200) {
        val contentType = connection.contentType ?: ""
        if (contentType.startsWith("image")) {
          Log.d("MetadataFetcher", "Open Library cover found for ISBN: $isbn")
          return@withContext url
        }
      }
      Log.d("MetadataFetcher", "Open Library cover not found for ISBN: $isbn (Code: $code)")
      null
    } catch (e: Exception) {
      Log.e("MetadataFetcher", "Open Library fetch error for ISBN $isbn: ${e.message}")
      null
    }
  }

  private suspend fun fetchGoogleBooksCover(isbn: String): String? = withContext(Dispatchers.IO) {
    val url = "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn"
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
      connection.connectTimeout = 3000
      connection.readTimeout = 3000
      val code = connection.responseCode
      if (code == 200) {
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val thumbnailRegex = Regex("\"(?:thumbnail|smallThumbnail)\"\\s*:\\s*\"([^\"]+)\"")
        val match = thumbnailRegex.find(responseText)
        if (match != null) {
          var thumbUrl = match.groupValues[1].replace("\\/", "/")
          if (thumbUrl.startsWith("http://")) {
            thumbUrl = "https://" + thumbUrl.substring(7)
          }
          Log.d("MetadataFetcher", "Google Books cover found for ISBN: $isbn -> $thumbUrl")
          return@withContext thumbUrl
        }
      }
      Log.d("MetadataFetcher", "Google Books cover not found for ISBN: $isbn (Code: $code)")
      null
    } catch (e: Exception) {
      Log.e("MetadataFetcher", "Google Books fetch error for ISBN $isbn: ${e.message}")
      null
    }
  }

  private suspend fun fetchIsbnSearchCover(isbn: String): String? = withContext(Dispatchers.IO) {
    val url = "https://isbnsearch.org/isbn/$isbn"
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
      )
      connection.connectTimeout = 4000
      connection.readTimeout = 4000
      val code = connection.responseCode
      if (code == 200) {
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        
        // 1. Try preload link
        val preloadRegex = Regex("<link rel=\"preload\" as=\"image\" href=\"([^\"]+)\"")
        val preloadMatch = preloadRegex.find(responseText)
        if (preloadMatch != null) {
          val imgUrl = preloadMatch.groupValues[1]
          Log.d("MetadataFetcher", "isbnsearch.org cover found via preload for ISBN: $isbn -> $imgUrl")
          return@withContext imgUrl
        }
        
        // 2. Try looser img tag regex
        val coverRegex = Regex("<div class=\"image\">\\s*<img src=\"([^\"]+)\"")
        val coverMatch = coverRegex.find(responseText)
        if (coverMatch != null) {
          val imgUrl = coverMatch.groupValues[1]
          Log.d("MetadataFetcher", "isbnsearch.org cover found via image div for ISBN: $isbn -> $imgUrl")
          return@withContext imgUrl
        }
      }
      Log.w("MetadataFetcher", "isbnsearch.org cover not found for ISBN: $isbn (Code: $code)")
      null
    } catch (e: Exception) {
      Log.e("MetadataFetcher", "isbnsearch.org fetch error for ISBN $isbn: ${e.message}")
      null
    }
  }

  suspend fun fetchCover(isbn: String): String = withContext(Dispatchers.IO) {
    val cleanIsbn = isbn.replace("-", "").replace(" ", "").trim()
    val coverUrl = fetchIsbnSearchCover(cleanIsbn)
      ?: fetchOpenLibraryCover(cleanIsbn)
      ?: fetchGoogleBooksCover(cleanIsbn)
      ?: getRandomPlaceholder()
    Log.i("MetadataFetcher", "Final cover resolved for ISBN $isbn: $coverUrl")
    coverUrl
  }

  suspend fun fetchGlobalMarc(isbn: String): MetadataRecord? = withContext(Dispatchers.IO) {
    if (isbn.length != 13 || !isbn.all { it.isDigit() }) return@withContext null
    val host = "54.78.43.234"
    val port = 210
    val initHex = "b43c830205e0840402e1a220850310000086031000009f6e0750795a333935309f6f1550795a3339353020312e302f5a4f4f4d2076312e349f7003312e30"
    val searchBase = "b64d8d01008e01018f01009001019103727331b20b9f6908444953434f564552b52da12b06072a8648ce130301a020bf661dbf2c0a30089f7801019f7901079f2d0d39373830313331313033363237"
    val presentHex = "b81c9f1f037273319e01019d0106b3038001469f68082a8648ce13056d0a"

    val isbnBytes = isbn.toByteArray(Charsets.US_ASCII)
    val isbnHex = isbnBytes.joinToString("") { "%02x".format(it) }
    val searchPacketHex = searchBase.replace("39373830313331313033363237", isbnHex)

    val socket = Socket()
    val job = coroutineContext[Job]
    val registration = job?.invokeOnCompletion {
      try {
        socket.close()
      } catch (e: Exception) {
        // ignore
      }
    }

    try {
      socket.connect(java.net.InetSocketAddress(host, port), 3000)
      socket.soTimeout = 5000
      if (!isActive) throw CancellationException("Coroutine was cancelled before Z39.50 interaction")
      socket.use { _ ->
        val outputStream = socket.getOutputStream()
        val inputStream = socket.getInputStream()

        val buf = ByteArray(1024)
        try {
          // 1. Send INIT
          if (!isActive) throw CancellationException()
          outputStream.write(hexToBytes(initHex))
          outputStream.flush()
          inputStream.read(buf)
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          // Proceed anyway
        }

        try {
          // 2. Send SEARCH
          if (!isActive) throw CancellationException()
          outputStream.write(hexToBytes(searchPacketHex))
          outputStream.flush()
          inputStream.read(buf)
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          // Proceed anyway
        }

        try {
          // 3. Send PRESENT
          if (!isActive) throw CancellationException()
          outputStream.write(hexToBytes(presentHex))
          outputStream.flush()
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          // Proceed anyway
        }

        val rawStream = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        try {
          while (true) {
            if (!isActive) throw CancellationException()
            val len = inputStream.read(chunk)
            if (len == -1) break
            rawStream.write(chunk, 0, len)
            val contentStr = rawStream.toString("UTF-8")
            if (contentStr.contains("</mods>") || contentStr.contains("\u001D")) {
              break
            }
            if (contentStr.contains("record number out of range") ||
                contentStr.contains("Record number out of range") ||
                contentStr.contains("diagnostic") ||
                contentStr.contains("Diagnostic")) {
              break
            }
          }
        } catch (e: java.net.SocketTimeoutException) {
          // Proceed with partial data
        }

        if (!isActive) throw CancellationException()
        val responseStr = rawStream.toString("UTF-8")
        val modsStart = responseStr.indexOf("<mods")
        if (modsStart != -1) {
          var xmlData = responseStr.substring(modsStart)
          val modsEnd = xmlData.lastIndexOf("</mods>")
          if (modsEnd != -1) {
            xmlData = xmlData.substring(0, modsEnd + 7)
          }

          val titleRegex = Regex("<(?:\\w+:)?title[^>]*>(.*?)</(?:\\w+:)?title>", RegexOption.DOT_MATCHES_ALL)
          val authorRegex = Regex("<(?:\\w+:)?namePart[^>]*>(.*?)</(?:\\w+:)?namePart>", RegexOption.DOT_MATCHES_ALL)

          val titleMatch = titleRegex.find(xmlData)
          val authorMatch = authorRegex.find(xmlData)

          if (titleMatch != null) {
            val cleanTitle = titleMatch.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
            val cleanAuthor = authorMatch?.groupValues[1]?.replace(Regex("<[^>]+>"), "")?.trim()
            MetadataRecord(
              source_type = "global_marc",
              title = cleanTitle,
              author = cleanAuthor,
              source_url = "Z39.50://$host:$port?isbn=$isbn",
              category = "books"
            )
          } else null
        } else null
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (!isActive) throw CancellationException("fetchGlobalMarc cancelled", e)
      e.printStackTrace()
      null
    } finally {
      registration?.dispose()
      try {
        socket.close()
      } catch (e: Exception) {
        // ignore
      }
    }
  }

  suspend fun fetchCustomSocket(isbn: String): MetadataRecord? = withContext(Dispatchers.IO) {
    val host = "106.13.235.115"
    val port = 210
    val header = hexToBytes("00ff17db0000")

    val isbnClean = isbn.replace("-", "").trim()
    val isbnQuery = if (isbnClean.length == 13) {
      "${isbnClean.substring(0, 6)} ${isbnClean.substring(6)}"
    } else {
      isbnClean
    }

    val query = "REQU_BEGININIT1@10@ ma_isbn='$isbnQuery'BAG_END"
    val queryBytes = query.toByteArray(Charset.forName("GBK"))
    val payload = ByteArray(header.size + queryBytes.size)
    System.arraycopy(header, 0, payload, 0, header.size)
    System.arraycopy(queryBytes, 0, payload, header.size, queryBytes.size)

    val socket = Socket()
    val job = coroutineContext[Job]
    val registration = job?.invokeOnCompletion {
      try {
        socket.close()
      } catch (e: Exception) {
        // ignore
      }
    }

    try {
      socket.connect(java.net.InetSocketAddress(host, port), 3000)
      socket.soTimeout = 10000
      if (!isActive) throw CancellationException("Coroutine was cancelled before Custom Socket interaction")
      socket.use { _ ->
        val outputStream = socket.getOutputStream()
        val inputStream = socket.getInputStream()

        try {
          if (!isActive) throw CancellationException()
          outputStream.write(payload)
          outputStream.flush()
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          // Proceed anyway
        }

        val rawStream = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        try {
          while (true) {
            if (!isActive) throw CancellationException()
            val len = inputStream.read(chunk)
            if (len == -1) break
            rawStream.write(chunk, 0, len)
            val contentStr = rawStream.toString("GBK")
            if (contentStr.contains("BAG_END")) {
              break
            }
            if (contentStr.contains("ERR_") || contentStr.contains("error") || contentStr.contains("Error")) {
              break
            }
          }
        } catch (e: java.net.SocketTimeoutException) {
          // Proceed with partial data
        }

        if (!isActive) throw CancellationException()
        val responseBytes = rawStream.toByteArray()
        parseCustomSocketRecord(responseBytes, host, port, isbnQuery)
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (!isActive) throw CancellationException("fetchCustomSocket cancelled", e)
      e.printStackTrace()
      null
    } finally {
      registration?.dispose()
      try {
        socket.close()
      } catch (e: Exception) {
        // ignore
      }
    }
  }

  private fun parseCustomSocketRecord(responseBytes: ByteArray, host: String, port: Int, isbnQuery: String): MetadataRecord? {
    try {
      val beginMarker = "REC_BEGIN".toByteArray(Charset.forName("GBK"))
      val endMarker = "REC_END".toByteArray(Charset.forName("GBK"))

      val beginIndex = indexOf(responseBytes, beginMarker)
      if (beginIndex == -1) return null
      val recordStart = beginIndex + beginMarker.size

      val endIndex = indexOf(responseBytes, endMarker, recordStart)
      if (endIndex == -1) return null

      val recordLen = endIndex - recordStart
      val recordBytes = ByteArray(recordLen) { responseBytes[recordStart + it] }

      // Leader is 24 bytes, directory starts at 24. Find directory terminator 0x1E
      if (recordBytes.size < 24) return null
      var dirEnd = -1
      for (i in 24 until recordBytes.size) {
        if (recordBytes[i] == 0x1E.toByte()) {
          dirEnd = i
          break
        }
      }
      if (dirEnd == -1) return null

      val baseAddress = dirEnd + 1
      var title: String? = null
      var subtitle: String? = null
      var author: String? = null

      // Iterate through directory (12 bytes per entry)
      var i = 24
      while (i < dirEnd) {
        if (i + 12 > dirEnd) break
        val tag = String(recordBytes, i, 3, Charsets.US_ASCII)
        val len = String(recordBytes, i + 3, 4, Charsets.US_ASCII).toIntOrNull() ?: 0
        val start = String(recordBytes, i + 7, 5, Charsets.US_ASCII).toIntOrNull() ?: 0

        if (tag == "200") {
          val fieldStart = baseAddress + start
          if (fieldStart + len <= recordBytes.size) {
            val fieldBytes = ByteArray(len) { recordBytes[fieldStart + it] }
            val fieldStr = String(fieldBytes, Charset.forName("GBK"))
            // Subfields are split by 0x1F byte (ASCII Unit Separator)
            val subfields = fieldStr.split(0x1F.toChar())
            for (sub in subfields) {
              if (sub.isEmpty()) continue
              val code = sub[0]
              val valStr = sub.substring(1).trim().replace(Regex("\\s+"), " ")
              when (code) {
                'a' -> title = valStr
                'e' -> subtitle = valStr
                'f' -> author = valStr
              }
            }
          }
          break
        }
        i += 12
      }

      if (title != null) {
        val fullTitle = if (subtitle != null) "$title: $subtitle" else title
        val cleanTitle = fullTitle.trim().removeSuffix("/").trim()
        val cleanAuthor = author?.trim()?.removeSuffix("/")?.trim()
        return MetadataRecord(
          source_type = "custom_socket",
          title = cleanTitle,
          author = cleanAuthor,
          source_url = "socket://$host:$port?query=${isbnQuery.replace(" ", "%20")}",
          category = "books"
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  private fun indexOf(bytes: ByteArray, target: ByteArray, startIndex: Int = 0): Int {
    if (target.isEmpty()) return 0
    for (i in startIndex .. bytes.size - target.size) {
      var match = true
      for (j in target.indices) {
        if (bytes[i + j] != target[j]) {
          match = false
          break
        }
      }
      if (match) return i
    }
    return -1
  }

  private suspend fun kotlinx.coroutines.CoroutineScope.fetchSruSingle(baseUrl: String, isbn: String): MetadataRecord? {
    val queryStr = "alma.isbn=$isbn"
    var url = baseUrl.replace(Regex("query=[^&]*"), "query=$queryStr")
    if (!url.contains("recordSchema")) {
      url += "&recordSchema=mods"
    } else {
      url = url.replace(Regex("recordSchema=[^&]*"), "recordSchema=mods")
    }

    try {
      if (!isActive) return null
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("User-Agent", "Mozilla/5.0")
      connection.connectTimeout = 3000
      connection.readTimeout = 3000

      if (connection.responseCode == 200) {
        if (!isActive) return null
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val titleRegex = Regex("<(?:\\w+:)?title[^>]*>(.*?)</(?:\\w+:)?title>", RegexOption.DOT_MATCHES_ALL)
        val authorRegex = Regex("<(?:\\w+:)?namePart[^>]*>(.*?)</(?:\\w+:)?namePart>", RegexOption.DOT_MATCHES_ALL)

        val titleMatch = titleRegex.find(responseText)
        val authorMatch = authorRegex.find(responseText)

        if (titleMatch != null) {
          val title = titleMatch.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
          val author = authorMatch?.groupValues[1]?.replace(Regex("<[^>]+>"), "")?.trim()

          return MetadataRecord(
            source_type = "sru_mods",
            title = title,
            author = author,
            source_url = url,
            category = "books"
          )
        }
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
    }
    return null
  }

  suspend fun fetchSru(isbn: String): MetadataRecord? = withContext(Dispatchers.IO) {
    val sruUrls = listOf(
      "https://hvd.alma.exlibrisgroup.com/view/sru/01HVD_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://csu-sjsu.primo.exlibrisgroup.com/view/sru/01CALS_SJO?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://yale.alma.exlibrisgroup.com/view/sru/01YALE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://anu.primo.exlibrisgroup.com/view/sru/61ANU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://kbdk-aub.alma.exlibrisgroup.com/view/sru/45KBDK_AUB?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://www.alexandria.ch/view/sru/41BIG_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://mit.alma.exlibrisgroup.com/view/sru/01MIT_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://eu06.alma.exlibrisgroup.com/view/sru/44BL_MAIN?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://bne.alma.exlibrisgroup.com/view/sru/34BNE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://princeton.alma.exlibrisgroup.com/view/sru/01PRI_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://oxford.alma.exlibrisgroup.com/view/sru/44OXF_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5",
      "https://ca01.alma.exlibrisgroup.com/view/sru/1UTORONTO_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5"
    )

    val channel = Channel<MetadataRecord?>(sruUrls.size)
    val jobs = sruUrls.map { url ->
      launch {
        try {
          val record = fetchSruSingle(url, isbn)
          channel.send(record)
        } catch (e: Exception) {
          if (e is CancellationException) {
            throw e
          }
          channel.send(null)
        }
      }
    }

    var result: MetadataRecord? = null
    var receivedCount = 0
    try {
      while (receivedCount < sruUrls.size) {
        val record = channel.receive()
        receivedCount++
        if (record != null) {
          result = record
          break
        }
      }
    } catch (e: Exception) {
      if (e is CancellationException) {
        throw e
      }
    } finally {
      jobs.forEach { it.cancel() }
    }
    result
  }

  suspend fun fetchUnifiedMetadata(
    isbn: String,
    sources: List<String> = listOf("custom_socket", "global_marc", "sru_mods")
  ): UnifiedMetadata = withContext(Dispatchers.IO) {
    val records = mutableListOf<MetadataRecord>()

    if (sources.contains("custom_socket")) {
      fetchCustomSocket(isbn)?.let { records.add(it) }
    }
    if (sources.contains("global_marc")) {
      fetchGlobalMarc(isbn)?.let { records.add(it) }
    }
    if (sources.contains("sru_mods")) {
      fetchSru(isbn)?.let { records.add(it) }
    }

    UnifiedMetadata(
      isbn = isbn,
      image_url = "", // Decoupled and fetched asynchronously in the UI
      metadata_records = records
    )
  }
}
