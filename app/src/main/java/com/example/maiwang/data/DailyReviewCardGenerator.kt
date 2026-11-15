package com.example.maiwang.data

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Serializable
data class ReadwiseReviewHighlight(
  val text: String,
  val category: String? = null,
  val source_type: String? = null
)

@Serializable
data class ReadwiseReviewResponse(
  val highlights: List<ReadwiseReviewHighlight>? = null
)

object DailyReviewCardGenerator {

  private val json = Json { ignoreUnknownKeys = true }

  // Fetch lunar date from Sou-yun recommendation API
  suspend fun fetchLunarDate(dateStr: String): String = withContext(Dispatchers.IO) {
    val url = "https://api.sou-yun.cn/api/RecommendedReading?type=poem&needHtml=true&index=-1&date=$dateStr"
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.connectTimeout = 5000
      connection.readTimeout = 5000

      if (connection.responseCode == 200) {
        val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val response = json.decodeFromString<PoemResponse>(responseText)
        val dateField = response.Date ?: ""
        val parts = dateField.split(Regex("\\s+"))
        if (parts.size >= 2) {
          val year = parts[0]
          val monthDay = parts[1]
          "${year}年${monthDay}"
        } else {
          "未知农历"
        }
      } else {
        "未知农历"
      }
    } catch (e: Exception) {
      e.printStackTrace()
      "未知农历"
    }
  }

  // Fetch Readwise review highlight
  suspend fun fetchReadwiseHighlight(token: String): Pair<String, String> = withContext(Dispatchers.IO) {
    val url = "https://readwise.io/api/v2/review/"
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("Authorization", "Token $token")
      connection.connectTimeout = 8000
      connection.readTimeout = 8000

      if (connection.responseCode == 200) {
        val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val response = json.decodeFromString<ReadwiseReviewResponse>(responseText)
        val list = response.highlights
        if (list != null && list.isNotEmpty()) {
          val highlight = list[0]
          val category = highlight.category ?: highlight.source_type ?: "quotes"
          val text = highlight.text
          val catMap = mapOf(
            "books" to "书籍",
            "book" to "书籍",
            "articles" to "文章",
            "article" to "文章",
            "tweets" to "网帖",
            "tweet" to "网帖",
            "podcasts" to "播客",
            "podcast" to "播客",
            "quotes" to "摘录"
          )
          val categoryZh = catMap[category.lowercase()] ?: "摘录"
          Pair(categoryZh, text)
        } else {
          Pair("书籍", "今日没有发现回顾数据...")
        }
      } else {
        Pair("书籍", "无法连接 Readwise API 获取数据...")
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Pair("书籍", "网络请求失败，请检查网络...")
    }
  }

  // Helper to parse CSV line respecting quotes
  private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var inQuotes = false
    var currentField = StringBuilder()
    for (char in line) {
      when (char) {
        '"' -> inQuotes = !inQuotes
        ',' -> {
          if (inQuotes) {
            currentField.append(char)
          } else {
            result.add(currentField.toString().trim())
            currentField = StringBuilder()
          }
        }
        else -> currentField.append(char)
      }
    }
    result.add(currentField.toString().trim())
    return result
  }

  // Pick random fortune from assets CSV
  private fun getRandomFortune(context: Context): Pair<String, String> {
    val rows = mutableListOf<Pair<String, String>>()
    try {
      context.assets.open("daily-horoscope.csv").use { inputStream ->
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
          val headerLine = reader.readLine() // skip header
          var line: String? = reader.readLine()
          while (line != null) {
            val parts = parseCsvLine(line)
            if (parts.size >= 2) {
              val content = parts[0].removeSurrounding("\"").trim()
              val title = parts[1].removeSurrounding("\"").trim()
              if (content.isNotEmpty() && title.isNotEmpty()) {
                rows.add(Pair(title, content))
              }
            }
            line = reader.readLine()
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    if (rows.isEmpty()) {
      return Pair("宜学习", "开卷有益，温故知新")
    }

    val choice = rows[Random.nextInt(rows.size)]
    var content = choice.second
    if (content.length > 16) {
      content = content.substring(0, 15) + "…"
    }
    return Pair(choice.first, content)
  }

  // Pick random image from assets downloaded_images and get dimensions
  private fun getRandomImage(context: Context): Triple<String, Int, Int> {
    val imageNames = try {
      context.assets.list("downloaded_images")?.filter {
        it.lowercase().endsWith(".jpg") || it.lowercase().endsWith(".png") || it.lowercase().endsWith(".jpeg") || it.lowercase().endsWith(".webp")
      } ?: emptyList()
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }

    if (imageNames.isEmpty()) {
      return Triple("", 0, 0)
    }

    val chosenImage = imageNames[Random.nextInt(imageNames.size)]
    val assetPath = "downloaded_images/$chosenImage"
    var width = 0
    var height = 0

    try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.assets.open(assetPath).use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, options)
      }
      width = options.outWidth
      height = options.outHeight
    } catch (e: Exception) {
      e.printStackTrace()
    }

    val imageUri = "file:///android_asset/$assetPath"
    return Triple(imageUri, width, height)
  }

  // Generate complete HTML string
  suspend fun generateHtml(context: Context, token: String): String = withContext(Dispatchers.IO) {
    val (category, text) = fetchReadwiseHighlight(token)
    val truncatedText = if (text.length > 150) text.substring(0, 150) + "..." else text

    val now = Calendar.getInstance()
    val months = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")
    val weekdays = listOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")

    val gMonth = months[now.get(Calendar.MONTH)]
    val gWeekday = weekdays[now.get(Calendar.DAY_OF_WEEK) - 1]
    val gDay = now.get(Calendar.DAY_OF_MONTH).toString()
    val sysDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val lunarDate = fetchLunarDate(sysDateStr)
    val (fTitle, fContent) = getRandomFortune(context)
    val (imgUri, imgWidth, imgHeight) = getRandomImage(context)

    val template = context.assets.open("template.html").use { inputStream ->
      inputStream.bufferedReader(Charsets.UTF_8).readText()
    }

    val chars = category.trim().filter { !it.isWhitespace() }
    val categorySpans = if (chars.isEmpty()) {
      "<span>摘</span><span>录</span>"
    } else {
      chars.map { "<span>$it</span>" }.joinToString("")
    }

    var html = template
      .replace("__IMAGE_PATH__", imgUri)
      .replace("__IMAGE_WIDTH__", imgWidth.toString())
      .replace("__IMAGE_HEIGHT__", imgHeight.toString())
      .replace("__GREGORIAN_MONTH__", gMonth)
      .replace("__GREGORIAN_WEEKDAY__", gWeekday)
      .replace("__LUNAR_DATE__", lunarDate)
      .replace("__GREGORIAN_DAY__", gDay)
      .replace("__FORTUNE_TITLE__", fTitle)
      .replace("__FORTUNE_CONTENT__", fContent)
      .replace("__HIGHLIGHT_CATEGORY__", categorySpans)
      .replace("__HIGHLIGHT_TEXT__", truncatedText)

    html
  }
}
