package com.example.maiwang.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.example.maiwang.data.*
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperShadow
import com.example.maiwang.theme.PaperWhite
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.DialogProperties

// Stable placeholder resolver based on book ID
fun getStablePlaceholder(bookId: Int): String {
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
  val index = Math.abs(bookId) % placeholders.size
  return placeholders[index]
}

@Composable
fun XiaochouTab(
  categoryFilter: String?,
  searchQuery: String,
  step: Int,
  onStepChange: (Int) -> Unit,
  selectedBook: ReadwiseBook?,
  onSelectedBookChange: (ReadwiseBook?) -> Unit,
  onLoadingChange: (Boolean) -> Unit,
  deleteMode: Boolean = false,
  onDeleteModeChange: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val settingsRepository = remember { SettingsRepository(context) }
  val readwiseRepository = remember { ReadwiseRepository(context) }

  var token by remember { mutableStateOf(settingsRepository.getReadwiseToken().trim()) }
  var isLoadingBooks by remember { mutableStateOf(false) }
  var booksError by remember { mutableStateOf<String?>(null) }
  var booksList by remember { mutableStateOf<List<ReadwiseBook>>(emptyList()) }

  var isLoadingHighlights by remember { mutableStateOf(false) }
  var highlightsError by remember { mutableStateOf<String?>(null) }
  var highlightsList by remember { mutableStateOf<List<ReadwiseHighlightItem>>(emptyList()) }
  var editingHighlight by remember { mutableStateOf<ReadwiseHighlightItem?>(null) }
  var deletingHighlight by remember { mutableStateOf<ReadwiseHighlightItem?>(null) }

  LaunchedEffect(isLoadingBooks, isLoadingHighlights) {
    onLoadingChange(isLoadingBooks || isLoadingHighlights)
  }

  // Sync token value from settings when screen launches
  LaunchedEffect(Unit) {
    token = settingsRepository.getReadwiseToken().trim()
  }

  // Load books when token changes (load all locally)
  LaunchedEffect(token) {
    if (token.isEmpty()) return@LaunchedEffect
    isLoadingBooks = true
    booksError = null
    // 1. Load from local cache immediately (no network delay)
    val result = readwiseRepository.getBooks(token, category = null, forceRefresh = false)
    if (result.isSuccess) {
      booksList = result.getOrNull() ?: emptyList()
    } else {
      booksError = result.exceptionOrNull()?.message ?: "獲取數據失敗"
    }
    isLoadingBooks = false

    // 2. Silent background incremental sync to get latest changes
    if (booksList.isNotEmpty()) {
      isLoadingBooks = true
      val syncResult = readwiseRepository.getBooks(token, category = null, forceRefresh = true)
      if (syncResult.isSuccess) {
        booksList = syncResult.getOrNull() ?: emptyList()
      }
      isLoadingBooks = false
    }
  }

  // Load highlights when selectedBook changes
  LaunchedEffect(token, selectedBook) {
    if (token.isEmpty() || selectedBook == null) return@LaunchedEffect
    isLoadingHighlights = true
    highlightsError = null
    // 1. Load from local cache immediately
    val result = readwiseRepository.getHighlights(token, selectedBook.id, forceRefresh = false)
    if (result.isSuccess) {
      highlightsList = result.getOrNull() ?: emptyList()
    } else {
      highlightsError = result.exceptionOrNull()?.message ?: "獲取書摘失敗"
    }
    isLoadingHighlights = false

    // 2. Silent background incremental sync for selected book highlights
    if (highlightsList.isNotEmpty()) {
      isLoadingHighlights = true
      val syncResult = readwiseRepository.getHighlights(token, selectedBook.id, forceRefresh = true)
      if (syncResult.isSuccess) {
        highlightsList = syncResult.getOrNull() ?: emptyList()
      }
      isLoadingHighlights = false
    }
  }

  val triggerRefreshBooks = {
    if (token.isNotEmpty()) {
      isLoadingBooks = true
      booksError = null
      coroutineScope.launch {
        val result = readwiseRepository.getBooks(token, category = null, forceRefresh = true)
        if (result.isSuccess) {
          booksList = result.getOrNull() ?: emptyList()
          Toast.makeText(context, "同步成功", Toast.LENGTH_SHORT).show()
        } else {
          booksError = result.exceptionOrNull()?.message ?: "同步失敗"
        }
        isLoadingBooks = false
      }
    }
  }

  val triggerRefreshHighlights = {
    if (token.isNotEmpty() && selectedBook != null) {
      isLoadingHighlights = true
      highlightsError = null
      coroutineScope.launch {
        val result = readwiseRepository.getHighlights(token, selectedBook.id, forceRefresh = true)
        if (result.isSuccess) {
          highlightsList = result.getOrNull() ?: emptyList()
          Toast.makeText(context, "書摘同步成功", Toast.LENGTH_SHORT).show()
        } else {
          highlightsError = result.exceptionOrNull()?.message ?: "同步失敗"
        }
        isLoadingHighlights = false
      }
    }
  }

  if (token.isEmpty()) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(PaperWhite)
        .padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        tint = CinnabarRed,
        modifier = Modifier.size(48.dp)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "尚未配置 Readwise Token\n請前往「設置」界面配置以同步您的書摘與筆記。",
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        color = InkBlack,
        textAlign = TextAlign.Center
      )
    }
  } else {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(PaperWhite)
    ) {
      when (step) {
        1 -> {
          // Grid View
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 24.dp, vertical = 16.dp)
          ) {
            // Header
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Sync Button
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clickable(enabled = !isLoadingBooks) { triggerRefreshBooks() }
                  .padding(vertical = 6.dp, horizontal = 8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Sync,
                  contentDescription = "同步最新",
                  tint = CinnabarRed,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isLoadingBooks) "同步中..." else "同步最新",
                  fontFamily = FontFamily.Serif,
                  fontSize = 12.sp,
                  color = CinnabarRed
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = PaperShadow, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingBooks && booksList.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(color = CinnabarRed)
              }
            } else if (booksError != null && booksList.isEmpty()) {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = CinnabarRed, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = booksError!!,
                  fontFamily = FontFamily.Serif,
                  fontSize = 15.sp,
                  color = InkBlack,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                  onClick = { triggerRefreshBooks() },
                  colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
                  shape = ShapeDefaults.Small
                ) {
                  Text("重試", fontFamily = FontFamily.Serif, color = Color.White)
                }
              }
            } else {
              val filteredBooks = remember(booksList, categoryFilter, searchQuery) {
                var list = booksList
                if (!categoryFilter.isNullOrEmpty()) {
                  list = list.filter { it.category.equals(categoryFilter, ignoreCase = true) }
                }
                if (searchQuery.trim().isNotEmpty()) {
                  list = list.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                      (it.author?.contains(searchQuery, ignoreCase = true) == true)
                  }
                }
                list
              }

              if (filteredBooks.isEmpty()) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (searchQuery.isNotEmpty()) "未找到匹配書籍" else "暫無典籍記錄",
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    color = InkBlack.copy(alpha = 0.4f)
                  )
                }
              } else {
                LazyVerticalGrid(
                  columns = GridCells.Fixed(3),
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                  verticalArrangement = Arrangement.spacedBy(20.dp),
                  modifier = Modifier.fillMaxSize()
                ) {
                  items(filteredBooks) { book ->
                    BookGridItem(
                      book = book,
                      readwiseRepository = readwiseRepository,
                      onClick = {
                        onSelectedBookChange(book)
                        onStepChange(2)
                      }
                    )
                  }
                }
              }
            }
          }
        }

        2 -> {
          // Details view
          val currentBook = selectedBook
          if (currentBook == null) {
            onStepChange(1)
            return
          }

          val svgImageLoader = remember {
            ImageLoader.Builder(context)
              .components {
                add(SvgDecoder.Factory())
              }
              .build()
          }

          Box(modifier = Modifier.fillMaxSize()) {
            // Watermark Stamp in detail view
            AsyncImage(
              model = "file:///android_asset/stamp.svg",
              contentDescription = null,
              imageLoader = svgImageLoader,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .size(64.dp)
                .alpha(0.06f)
            )

            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
              // Header Details Card
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Book cover with lazy-loaded cache
                var coverModel by remember(currentBook) {
                  val coversDir = File(context.filesDir, "readwise_covers")
                  val coverFile = File(coversDir, "${currentBook.id}.jpg")
                  mutableStateOf(
                    if (coverFile.exists()) {
                      coverFile.absolutePath
                    } else if (!currentBook.cover_image_url.isNullOrEmpty()) {
                      currentBook.cover_image_url
                    } else {
                      getStablePlaceholder(currentBook.id)
                    }
                  )
                }

                LaunchedEffect(currentBook) {
                  val coversDir = File(context.filesDir, "readwise_covers")
                  val coverFile = File(coversDir, "${currentBook.id}.jpg")
                  if (!coverFile.exists() && !currentBook.cover_image_url.isNullOrEmpty()) {
                    val localPath = readwiseRepository.getOrFetchLocalCover(currentBook.id, currentBook.cover_image_url)
                    if (localPath != null) {
                      coverModel = localPath
                    }
                  }
                }

                Box(
                  modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .border(width = 0.5.dp, color = InkBlack)
                ) {
                  AsyncImage(
                    model = coverModel,
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = currentBook.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = InkBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = currentBook.author ?: "未知作者",
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = InkBlack.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = when (currentBook.category) {
                        "books" -> "書籍"
                        "podcasts" -> "播客"
                        "articles" -> "文章"
                        "tweets" -> "推特"
                        else -> currentBook.category
                      },
                      fontFamily = FontFamily.Serif,
                      fontSize = 11.sp,
                      color = CinnabarRed,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier
                        .border(width = 0.5.dp, color = CinnabarRed)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "共 ${currentBook.num_highlights} 條記錄",
                      fontFamily = FontFamily.Serif,
                      fontSize = 11.sp,
                      color = InkBlack.copy(alpha = 0.5f)
                    )
                  }
                }
              }

              Divider(color = PaperShadow, thickness = 0.5.dp)

              if (isLoadingHighlights && highlightsList.isEmpty()) {
                Box(
                  modifier = Modifier.fillMaxSize().weight(1f),
                  contentAlignment = Alignment.Center
                ) {
                  CircularProgressIndicator(color = CinnabarRed)
                }
              } else if (highlightsError != null && highlightsList.isEmpty()) {
                Column(
                  modifier = Modifier.fillMaxSize().weight(1f),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = CinnabarRed, modifier = Modifier.size(36.dp))
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = highlightsError!!,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    color = InkBlack,
                    textAlign = TextAlign.Center
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  Button(
                    onClick = { triggerRefreshHighlights() },
                    colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
                    shape = ShapeDefaults.Small
                  ) {
                    Text("同步更新", fontFamily = FontFamily.Serif, color = Color.White)
                  }
                }
              } else {
                LazyColumn(
                  verticalArrangement = Arrangement.spacedBy(16.dp),
                  contentPadding = PaddingValues(vertical = 16.dp),
                  modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                ) {
                  items(highlightsList) { highlight ->
                    HighlightCardItem(
                      item = highlight,
                      deleteMode = deleteMode,
                      onEditClick = { editingHighlight = it },
                      onDeleteClick = { deletingHighlight = it }
                    )
                  }

                  item {
                    // Sync Trigger at bottom
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clickable { triggerRefreshHighlights() },
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Sync, contentDescription = null, tint = CinnabarRed, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = if (isLoadingHighlights) "正在同步書摘..." else "同步最新書摘",
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = CinnabarRed,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (editingHighlight != null) {
        EditHighlightDialog(
          highlight = editingHighlight!!,
          token = token,
          readwiseRepository = readwiseRepository,
          onDismiss = { editingHighlight = null },
          onSaveSuccess = { updatedItem ->
            highlightsList = highlightsList.map { item ->
              if (item.id == updatedItem.id) updatedItem else item
            }
            editingHighlight = null
          }
        )
      }

      if (deletingHighlight != null) {
        DeleteHighlightDialog(
          highlight = deletingHighlight!!,
          token = token,
          readwiseRepository = readwiseRepository,
          onDismiss = { deletingHighlight = null },
          onDeleteSuccess = { deletedId ->
            highlightsList = highlightsList.filter { it.id != deletedId }
            deletingHighlight = null
            onDeleteModeChange(false)
          }
        )
      }
    }
  }
}

@Composable
fun BookGridItem(
  book: ReadwiseBook,
  readwiseRepository: ReadwiseRepository,
  onClick: () -> Unit
) {
  val context = LocalContext.current
  var coverModel by remember(book) {
    val coversDir = File(context.filesDir, "readwise_covers")
    val coverFile = File(coversDir, "${book.id}.jpg")
    mutableStateOf(
      if (coverFile.exists()) {
        coverFile.absolutePath
      } else if (!book.cover_image_url.isNullOrEmpty()) {
        book.cover_image_url
      } else {
        getStablePlaceholder(book.id)
      }
    )
  }

  LaunchedEffect(book) {
    val coversDir = File(context.filesDir, "readwise_covers")
    val coverFile = File(coversDir, "${book.id}.jpg")
    if (!coverFile.exists() && !book.cover_image_url.isNullOrEmpty()) {
      val localPath = readwiseRepository.getOrFetchLocalCover(book.id, book.cover_image_url)
      if (localPath != null) {
        coverModel = localPath
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2f / 3f)
        .border(width = 0.5.dp, color = PaperShadow)
        .background(Color(0xFFEAE4CC).copy(alpha = 0.1f))
    ) {
      AsyncImage(
        model = coverModel,
        contentDescription = book.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = book.title,
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      color = InkBlack,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      lineHeight = 18.sp,
      modifier = Modifier.fillMaxWidth()
    )

    if (!book.author.isNullOrEmpty()) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = book.author,
        fontFamily = FontFamily.Serif,
        fontSize = 10.sp,
        color = InkBlack.copy(alpha = 0.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
fun HighlightCardItem(
  item: ReadwiseHighlightItem,
  deleteMode: Boolean,
  onEditClick: (ReadwiseHighlightItem) -> Unit,
  onDeleteClick: (ReadwiseHighlightItem) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    // Quote Box
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
      shape = ShapeDefaults.ExtraSmall,
      modifier = Modifier
        .fillMaxWidth()
        .border(width = 0.5.dp, color = PaperShadow)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.FormatQuote,
            contentDescription = null,
            tint = CinnabarRed.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
          )
          
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            val locationText = remember(item) {
              if (item.location != null) {
                when (item.location_type) {
                  "page" -> "第 ${item.location} 頁"
                  "location" -> "位置 ${item.location}"
                  "time_offset" -> {
                    val minutes = item.location / 60
                    val seconds = item.location % 60
                    String.format("%02d:%02d", minutes, seconds)
                  }
                  else -> "第 ${item.location} 頁"
                }
              } else ""
            }
            if (locationText.isNotEmpty()) {
              Text(
                text = locationText,
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                color = InkBlack.copy(alpha = 0.5f)
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            
            if (deleteMode) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "刪除",
                tint = CinnabarRed,
                modifier = Modifier
                  .size(16.dp)
                  .clickable { onDeleteClick(item) }
              )
            } else {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "修訂",
                tint = CinnabarRed.copy(alpha = 0.6f),
                modifier = Modifier
                  .size(16.dp)
                  .clickable { onEditClick(item) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Left bordered line with highlight text
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
        ) {
          Box(
            modifier = Modifier
              .width(2.5.dp)
              .fillMaxHeight()
              .background(CinnabarRed)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = item.text,
            fontFamily = FontFamily.Serif,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = InkBlack,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Optional Note Box
    if (!item.note.isNullOrEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp),
        verticalAlignment = Alignment.Top
      ) {
        Icon(
          imageVector = Icons.Default.EditNote,
          contentDescription = null,
          tint = InkBlack.copy(alpha = 0.5f),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "校讎批註",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = CinnabarRed,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = item.note,
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = InkBlack.copy(alpha = 0.8f)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHighlightDialog(
  highlight: ReadwiseHighlightItem,
  token: String,
  readwiseRepository: ReadwiseRepository,
  onDismiss: () -> Unit,
  onSaveSuccess: (ReadwiseHighlightItem) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var text by remember { mutableStateOf(highlight.text) }
  var note by remember { mutableStateOf(highlight.note ?: "") }
  var isSaving by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(24.dp)
      .border(width = 0.5.dp, color = PaperShadow)
      .background(PaperWhite),
    content = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(PaperWhite)
          .padding(20.dp)
      ) {
        Text(
          text = "修訂校讎",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = CinnabarRed,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        val locationText = remember(highlight) {
          if (highlight.location != null) {
            when (highlight.location_type) {
              "page" -> "第 ${highlight.location} 頁"
              "location" -> "位置 ${highlight.location}"
              "time_offset" -> {
                val minutes = highlight.location / 60
                val seconds = highlight.location % 60
                String.format("%02d:%02d", minutes, seconds)
              }
              else -> "第 ${highlight.location} 頁"
            }
          } else ""
        }
        if (locationText.isNotEmpty()) {
          Text(
            text = "位置：$locationText",
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            color = InkBlack.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }

        Text(
          text = "引文內文",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = InkBlack.copy(alpha = 0.6f),
          modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, color = InkBlack),
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CinnabarRed,
            unfocusedBorderColor = PaperShadow,
            cursorColor = CinnabarRed
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "校讎批註",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = InkBlack.copy(alpha = 0.6f),
          modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp, color = InkBlack),
          modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CinnabarRed,
            unfocusedBorderColor = PaperShadow,
            cursorColor = CinnabarRed
          )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onDismiss,
            enabled = !isSaving,
            colors = ButtonDefaults.textButtonColors(contentColor = InkBlack.copy(alpha = 0.6f))
          ) {
            Text("取消", fontFamily = FontFamily.Serif)
          }
          Spacer(modifier = Modifier.width(16.dp))
          Button(
            onClick = {
              if (text.trim().isEmpty()) {
                Toast.makeText(context, "引文內文不能為空", Toast.LENGTH_SHORT).show()
                return@Button
              }
              isSaving = true
              scope.launch {
                val result = readwiseRepository.updateHighlight(token, highlight.id, text, note)
                if (result.isSuccess) {
                  Toast.makeText(context, "修訂成功", Toast.LENGTH_SHORT).show()
                  onSaveSuccess(result.getOrThrow())
                } else {
                  val error = result.exceptionOrNull()?.message ?: "修訂失敗"
                  Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
                isSaving = false
              }
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
            shape = ShapeDefaults.Small
          ) {
            if (isSaving) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Text("保存修訂", fontFamily = FontFamily.Serif, color = Color.White)
            }
          }
        }
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteHighlightDialog(
  highlight: ReadwiseHighlightItem,
  token: String,
  readwiseRepository: ReadwiseRepository,
  onDismiss: () -> Unit,
  onDeleteSuccess: (Int) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isDeleting by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isDeleting) onDismiss() },
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(24.dp)
      .border(width = 0.5.dp, color = PaperShadow)
      .background(PaperWhite),
    content = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(PaperWhite)
          .padding(20.dp)
      ) {
        Text(
          text = "確認刪除",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = CinnabarRed,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
          text = "確定要從 Readwise 刪除此條書摘嗎？此操作不可撤銷。",
          fontFamily = FontFamily.Serif,
          fontSize = 14.sp,
          color = InkBlack,
          modifier = Modifier.padding(bottom = 16.dp),
          lineHeight = 22.sp
        )

        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
          shape = ShapeDefaults.ExtraSmall,
          modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = PaperShadow)
            .padding(12.dp)
        ) {
          Text(
            text = highlight.text,
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
            color = InkBlack.copy(alpha = 0.7f)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onDismiss,
            enabled = !isDeleting,
            colors = ButtonDefaults.textButtonColors(contentColor = InkBlack.copy(alpha = 0.6f))
          ) {
            Text("取消", fontFamily = FontFamily.Serif)
          }
          Spacer(modifier = Modifier.width(16.dp))
          Button(
            onClick = {
              isDeleting = true
              scope.launch {
                val result = readwiseRepository.deleteHighlight(token, highlight.id)
                if (result.isSuccess) {
                  Toast.makeText(context, "刪除成功", Toast.LENGTH_SHORT).show()
                  onDeleteSuccess(highlight.id)
                } else {
                  val error = result.exceptionOrNull()?.message ?: "刪除失敗"
                  Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
                isDeleting = false
              }
            },
            enabled = !isDeleting,
            colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
            shape = ShapeDefaults.Small
          ) {
            if (isDeleting) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Text("確認刪除", fontFamily = FontFamily.Serif, color = Color.White)
            }
          }
        }
      }
    }
  )
}
