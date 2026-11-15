package com.example.maiwang.data

import android.content.Context

class SettingsRepository(context: Context) {
  private val prefs = context.getSharedPreferences("maiwang_settings", Context.MODE_PRIVATE)

  fun getReadwiseToken(): String {
    return prefs.getString("readwise_token", "") ?: ""
  }

  fun saveReadwiseToken(token: String) {
    prefs.edit().putString("readwise_token", token).apply()
  }

  fun getBaiduApiKey(): String {
    return prefs.getString("baidu_api_key", "") ?: ""
  }

  fun saveBaiduApiKey(key: String) {
    prefs.edit().putString("baidu_api_key", key).apply()
  }

  fun getBaiduSecretKey(): String {
    return prefs.getString("baidu_secret_key", "") ?: ""
  }

  fun saveBaiduSecretKey(key: String) {
    prefs.edit().putString("baidu_secret_key", key).apply()
  }
}
