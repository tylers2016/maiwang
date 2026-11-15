package com.example.maiwang.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class HistoryItem(
  val isbn: String,
  val title: String,
  val subtitle: String?,
  val timestamp: Long,
  val success: Boolean,
  val source: String = "中國 Z39.50"
)

class SearchHistoryRepository(context: Context) {
  private val file = File(context.filesDir, "search_history.json")
  private val json = Json { 
    ignoreUnknownKeys = true
    prettyPrint = true 
  }

  suspend fun getHistory(): List<HistoryItem> = withContext(Dispatchers.IO) {
    if (!file.exists()) return@withContext emptyList()
    try {
      val content = file.readText()
      json.decodeFromString<List<HistoryItem>>(content)
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  suspend fun addHistory(item: HistoryItem) = withContext(Dispatchers.IO) {
    val currentHistory = getHistory().toMutableList()
    currentHistory.removeAll { it.isbn == item.isbn }
    currentHistory.add(0, item)
    val trimmed = if (currentHistory.size > 24) currentHistory.take(24) else currentHistory
    try {
      val serialized = json.encodeToString(trimmed)
      file.writeText(serialized)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  suspend fun clearHistory() = withContext(Dispatchers.IO) {
    try {
      if (file.exists()) {
        file.delete()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

