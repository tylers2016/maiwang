package com.example.maiwang.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.example.maiwang.CreateHighlight
import com.example.maiwang.data.HistoryItem
import com.example.maiwang.data.MetadataFetcher
import com.example.maiwang.data.MetadataRecord
import com.example.maiwang.data.SearchHistoryRepository
import com.example.maiwang.data.UnifiedMetadata
import com.example.maiwang.theme.CeladonGreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperShadow
import com.example.maiwang.theme.PaperWhite

sealed interface SearchState {
  object Loading : SearchState
  data class Success(val data: UnifiedMetadata) : SearchState
  data class Error(val message: String) : SearchState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
  isbn: String,
  source: String,
  onBack: () -> Unit,
  onNavigate: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchState by remember { mutableStateOf<SearchState>(SearchState.Loading) }
  val historyRepository = remember { SearchHistoryRepository(context) }

  val stablePlaceholder = remember(isbn) {
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
    val index = (isbn.filter { it.isDigit() }.hashCode() % placeholders.size).let { if (it < 0) it + placeholders.size else it }
    placeholders[index]
  }
  var coverUrl by remember(isbn) { mutableStateOf(stablePlaceholder) }

  LaunchedEffect(isbn) {
    try {
      val resolvedCover = MetadataFetcher.fetchCover(isbn)
      if (isActive) {
        coverUrl = resolvedCover
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      e.printStackTrace()
    }
  }

  // Perform search on launch
  LaunchedEffect(isbn, source) {
    searchState = SearchState.Loading
    try {
      val sourceKey = when (source) {
        "中國 Z39.50" -> "custom_socket"
        "英國 Z39.50" -> "global_marc"
        else -> "sru_mods"
      }
      val unified = MetadataFetcher.fetchUnifiedMetadata(isbn, listOf(sourceKey))
      if (isActive) {
        searchState = SearchState.Success(unified)

        // Add to search history
        try {
          if (unified.metadata_records.isNotEmpty()) {
            val repRecord = unified.metadata_records.first()
            historyRepository.addHistory(
              HistoryItem(
                isbn = isbn,
                title = repRecord.title,
                subtitle = repRecord.author ?: "未知責任者",
                timestamp = System.currentTimeMillis(),
                success = true,
                source = source
              )
            )
          } else {
            historyRepository.addHistory(
              HistoryItem(
                isbn = isbn,
                title = "未知書名",
                subtitle = "點擊重新檢索",
                timestamp = System.currentTimeMillis(),
                success = false,
                source = source
              )
            )
          }
        } catch (he: Exception) {
          if (he is CancellationException) throw he
          he.printStackTrace()
        }
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      e.printStackTrace()
      if (isActive) {
        searchState = SearchState.Error(e.message ?: "網絡請求失敗")
        try {
          historyRepository.addHistory(
            HistoryItem(
              isbn = isbn,
              title = "未知書名",
              subtitle = "點擊重新檢索",
              timestamp = System.currentTimeMillis(),
              success = false,
              source = source
            )
          )
        } catch (he: Exception) {
          if (he is CancellationException) throw he
          he.printStackTrace()
        }
      }
    }
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "脈 望",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = InkBlack,
            letterSpacing = 4.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "返回",
              tint = InkBlack
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = PaperWhite
        ),
        modifier = Modifier.border(width = 0.5.dp, color = PaperShadow)
      )
    },
    containerColor = PaperWhite
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (val state = searchState) {
        SearchState.Loading -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(color = CinnabarRed)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "正在檢索文獻資料，請稍候...",
              fontFamily = FontFamily.Serif,
              fontSize = 16.sp,
              color = InkBlack.copy(alpha = 0.6f)
            )
          }
        }
        is SearchState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "檢索失敗",
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = CinnabarRed,
              modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
              text = state.message,
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.7f),
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(
              onClick = { onBack() },
              colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.border(width = 1.dp, color = InkBlack)
            ) {
              Text("返回上頁", fontFamily = FontFamily.Serif, color = Color.White)
            }
          }
        }
        is SearchState.Success -> {
          val metadata = state.data
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 24.dp)
          ) {
            // Search Header Context
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "Search Results for ISBN ($source)",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = InkBlack.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = isbn,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = InkBlack
              )
              Spacer(modifier = Modifier.height(8.dp))
              Divider(
                color = CinnabarRed,
                thickness = 1.dp,
                modifier = Modifier.width(64.dp)
              )
            }

            if (metadata.metadata_records.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "未檢索到相關文獻",
                  fontFamily = FontFamily.Serif,
                  fontSize = 16.sp,
                  color = InkBlack.copy(alpha = 0.4f)
                )
              }
            } else {
              LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
              ) {
                itemsIndexed(metadata.metadata_records) { index, record ->
                  ResultCard(
                    record = record,
                    imageUrl = coverUrl,
                    colorBarIndex = index,
                    onSelect = {
                      onNavigate(
                        CreateHighlight(
                          title = record.title,
                          author = record.author,
                          imageUrl = coverUrl,
                          sourceUrl = record.source_url,
                          isbn = isbn
                        )
                      )
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ResultCard(
  record: MetadataRecord,
  imageUrl: String,
  colorBarIndex: Int,
  onSelect: () -> Unit
) {
  // Left border bar colors from stitch code.html (vermilion/gray/etc)
  val indicatorColor = when (record.source_type) {
    "custom_socket" -> CeladonGreen
    "global_marc" -> CinnabarRed
    else -> InkBlack.copy(alpha = 0.4f)
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
    shape = MaterialTheme.shapes.small,
    modifier = Modifier
      .fillMaxWidth()
      .border(width = 0.5.dp, color = PaperShadow)
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      // Left vertical book tab marker (Yuer)
      Box(
        modifier = Modifier
          .width(4.dp)
          .height(32.dp)
          .align(Alignment.TopStart)
          .offset(y = 16.dp)
          .background(indicatorColor)
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top
      ) {
        // Book Cover (2:3 aspect ratio)
        Box(
          modifier = Modifier
            .width(88.dp)
            .height(132.dp)
            .border(width = 0.5.dp, color = InkBlack)
            .background(Color.White)
        ) {
          AsyncImage(
            model = imageUrl,
            contentDescription = "Cover of ${record.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 132.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = when (record.source_type) {
                "custom_socket" -> "中國 Z39.50"
                "global_marc" -> "英國 Z39.50"
                else -> "全球 SRU"
              },
              fontFamily = FontFamily.SansSerif,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = indicatorColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = record.title,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = InkBlack,
              lineHeight = 24.sp
            )
            record.author?.let {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = it,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = InkBlack.copy(alpha = 0.6f)
              )
            }
          }

          // Select button
          Box(
            modifier = Modifier
              .border(width = 0.5.dp, color = InkBlack)
              .background(Color(0xFFFFF9E7))
              .clickable { onSelect() }
              .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "選取",
              fontFamily = FontFamily.Serif,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = InkBlack
            )
          }
        }
      }
    }
  }
}
