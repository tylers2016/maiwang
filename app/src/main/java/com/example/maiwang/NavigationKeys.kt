package com.example.maiwang

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Collation : NavKey
@Serializable data object Gallery : NavKey
@Serializable data object Scanner : NavKey
@Serializable data object History : NavKey
@Serializable data object Settings : NavKey
@Serializable data object ManualInput : NavKey

@Serializable 
data class SearchResult(val isbn: String, val source: String) : NavKey

@Serializable
data class CreateHighlight(
  val title: String,
  val author: String?,
  val imageUrl: String?,
  val sourceUrl: String?,
  val isbn: String?
) : NavKey




