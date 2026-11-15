package com.example.maiwang.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class BaiduTokenResponse(
  val access_token: String
)

@Serializable
data class BaiduOcrLocation(
  val left: Int,
  val top: Int,
  val width: Int,
  val height: Int
)

@Serializable
data class BaiduOcrWord(
  val words: String,
  val location: BaiduOcrLocation? = null
)

@Serializable
data class BaiduOcrResponse(
  val words_result: List<BaiduOcrWord>? = null,
  val error_msg: String? = null
)

object BaiduOcrHelper {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun getAccessToken(apiKey: String, secretKey: String): String? = withContext(Dispatchers.IO) {
    try {
      val url = URL("https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=$apiKey&client_secret=$secretKey")
      val conn = url.openConnection() as HttpURLConnection
      conn.requestMethod = "POST"
      conn.connectTimeout = 10000
      conn.readTimeout = 10000
      
      val code = conn.responseCode
      if (code == 200) {
        val res = conn.inputStream.bufferedReader().use { it.readText() }
        val response = json.decodeFromString<BaiduTokenResponse>(res)
        response.access_token
      } else {
        null
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  suspend fun performOcr(accessToken: String, imageBytes: ByteArray): Result<List<BaiduOcrWord>> = withContext(Dispatchers.IO) {
    try {
      val url = URL("https://aip.baidubce.com/rest/2.0/ocr/v1/accurate?access_token=$accessToken")
      val conn = url.openConnection() as HttpURLConnection
      conn.requestMethod = "POST"
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      conn.doOutput = true
      conn.connectTimeout = 20000
      conn.readTimeout = 20000

      val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
      val requestBody = "image=" + java.net.URLEncoder.encode(base64Image, "UTF-8")

      conn.outputStream.use { os ->
        os.write(requestBody.toByteArray(Charsets.UTF_8))
      }

      val code = conn.responseCode
      if (code == 200) {
        val res = conn.inputStream.bufferedReader().use { it.readText() }
        val response = json.decodeFromString<BaiduOcrResponse>(res)
        if (response.error_msg != null) {
          Result.failure(Exception(response.error_msg))
        } else {
          val wordsList = response.words_result ?: emptyList()
          Result.success(wordsList)
        }
      } else {
        val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP 錯誤代碼: $code"
        Result.failure(Exception(errorMsg))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
