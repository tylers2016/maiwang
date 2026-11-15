package com.example.maiwang

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.maiwang.ui.main.MainScreen
import com.example.maiwang.ui.main.ScannerScreen
import com.example.maiwang.ui.main.HistoryScreen
import com.example.maiwang.ui.main.SettingsScreen
import com.example.maiwang.ui.main.ManualInputScreen
import com.example.maiwang.ui.main.SearchResultScreen
import com.example.maiwang.ui.main.CreateHighlightScreen

@Composable
fun MainNavigation() {
  var showSplash by rememberSaveable { mutableStateOf(true) }

  if (showSplash) {
    SplashScreen(onTimeout = { showSplash = false })
  } else {
    val backStack = rememberNavBackStack(Main)
    var scannedIsbn by rememberSaveable { mutableStateOf("") }

    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Main> {
            MainScreen(
              onItemClick = { navKey -> backStack.add(navKey) },
              scannedIsbn = scannedIsbn,
              onScannedIsbnClear = { scannedIsbn = "" },
              modifier = Modifier.safeDrawingPadding()
            )
          }
          entry<Scanner> {
            ScannerScreen(
              onBack = { backStack.removeLastOrNull() },
              onIsbnScanned = { isbn -> scannedIsbn = isbn }
            )
          }
          entry<History> {
            HistoryScreen(
              onBack = { backStack.removeLastOrNull() },
              onNavigate = { navKey -> backStack.add(navKey) }
            )
          }
          entry<Settings> {
            SettingsScreen(
              onBack = { backStack.removeLastOrNull() }
            )
          }
          entry<ManualInput> {
            ManualInputScreen(
              onBack = { backStack.removeLastOrNull() },
              onNavigate = { navKey -> backStack.add(navKey) }
            )
          }
          entry<SearchResult> { key ->
            SearchResultScreen(
              isbn = key.isbn,
              source = key.source,
              onBack = { backStack.removeLastOrNull() },
              onNavigate = { navKey -> backStack.add(navKey) }
            )
          }
          entry<CreateHighlight> { key ->
            CreateHighlightScreen(
              title = key.title,
              author = key.author,
              imageUrl = key.imageUrl,
              sourceUrl = key.sourceUrl,
              isbn = key.isbn,
              onBack = { backStack.removeLastOrNull() },
              onSuccess = {
                // Pop back to main screen or pop two screens
                backStack.removeLastOrNull() // pop CreateHighlight
                // If SearchResult is also on stack, pop it too
                if (backStack.lastOrNull() is SearchResult || backStack.lastOrNull() is ManualInput) {
                  backStack.removeLastOrNull()
                }
              }
            )
          }
        },
    )
  }
}



