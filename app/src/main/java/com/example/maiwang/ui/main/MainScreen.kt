package com.example.maiwang.ui.main

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.maiwang.Main
import com.example.maiwang.Scanner
import com.example.maiwang.History
import com.example.maiwang.ManualInput
import com.example.maiwang.SearchResult
import com.example.maiwang.data.DefaultDataRepository
import com.example.maiwang.data.ReadwiseBook
import com.example.maiwang.theme.Theme
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperWhite
import com.example.maiwang.theme.PaperShadow
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.CeladonGreen
import com.example.maiwang.theme.QuoteTextStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.maiwang.data.DailyReviewCardGenerator
import com.example.maiwang.data.SettingsRepository
import android.graphics.Bitmap
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  scannedIsbn: String,
  onScannedIsbnClear: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  // State
  var currentTab by rememberSaveable { mutableStateOf("藝文") }
  var selectedSource by rememberSaveable { mutableStateOf("中國 Z39.50") }
  var isbnInput by rememberSaveable { mutableStateOf("") }
  var isbnError by remember { mutableStateOf<String?>(null) }
  var poemText by remember { mutableStateOf("「芸香三味，脉望其真。」") }
  var poemTitle by remember { mutableStateOf("於浩瀚書海中捕捉靈感之光，將書香化作永恆。") }
  var tupuRefreshTrigger by remember { mutableStateOf(0) }
  var tupuWebViewRef by remember { mutableStateOf<WebView?>(null) }

  // Xiaochou specific states
  var xiaochouStep by rememberSaveable { mutableStateOf(1) } // 1: BOOK_GRID, 2: BOOK_DETAILS
  var selectedBookForDetails by remember { mutableStateOf<ReadwiseBook?>(null) }
  var xiaochouCategory by remember { mutableStateOf<String?>(null) }
  var xiaochouSearchQuery by remember { mutableStateOf("") }
  var xiaochouSearchMode by remember { mutableStateOf(false) }
  var showCategoryMenu by remember { mutableStateOf(false) }
  var xiaochouDeleteMode by remember { mutableStateOf(false) }

  // Hoisted loading states for top bar centered status display
  var isYiwenLoading by remember { mutableStateOf(false) }
  var isXiaochouLoading by remember { mutableStateOf(false) }
  var isTupuLoading by remember { mutableStateOf(false) }
  var showShareDialog by remember { mutableStateOf(false) }

  LaunchedEffect(scannedIsbn) {
    if (scannedIsbn.isNotEmpty()) {
      isbnInput = scannedIsbn
      isbnError = null
      val targetIsbn = scannedIsbn.trim()
      onScannedIsbnClear()
      if (targetIsbn.isNotEmpty()) {
        onItemClick(SearchResult(isbn = targetIsbn, source = selectedSource))
      }
    }
  }

  LaunchedEffect(Unit) {
    isYiwenLoading = true
    try {
      val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
      val res = com.example.maiwang.data.MetadataFetcher.fetchRecommendedPoem(dateStr)
      if (res != null && isActive) {
        poemText = res.first
        poemTitle = res.second
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      e.printStackTrace()
    } finally {
      if (isActive) {
        isYiwenLoading = false
      }
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        drawerContainerColor = PaperWhite,
        drawerContentColor = InkBlack,
        modifier = Modifier
          .width(280.dp)
          .fillMaxHeight()
          .border(width = 1.dp, color = PaperShadow)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            // Drawer Header
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (currentTab == "校讎") "類別篩選" else "文獻來源",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = CinnabarRed
              )
              IconButton(onClick = { scope.launch { drawerState.close() } }) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "關閉",
                  tint = InkBlack
                )
              }
            }

            HorizontalDivider(color = CinnabarRed, thickness = 1.dp, modifier = Modifier.padding(bottom = 24.dp))

            if (currentTab == "校讎") {
              val categories = listOf(
                null to "全部 (All)",
                "books" to "書籍 (Books)",
                "podcasts" to "播客 (Podcasts)",
                "articles" to "文章 (Articles)",
                "tweets" to "推特 (Tweets)"
              )
              categories.forEach { (cat, name) ->
                val isSelected = xiaochouCategory == cat
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(if (isSelected) Color(0xFFFAF4DB) else Color.Transparent)
                    .clickable {
                      xiaochouCategory = cat
                      scope.launch { drawerState.close() }
                      Toast.makeText(context, "篩選類別: ${name.substringBefore(" (")}", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(CinnabarRed)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                  } else {
                    Spacer(modifier = Modifier.width(16.dp))
                  }

                  Icon(
                    imageVector = when (cat) {
                      "books" -> Icons.Default.Book
                      "podcasts" -> Icons.Default.MusicNote
                      "articles" -> Icons.Default.Edit
                      "tweets" -> Icons.Default.Public
                      else -> Icons.Default.MenuBook
                    },
                    contentDescription = null,
                    tint = if (isSelected) CinnabarRed else InkBlack.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                  )

                  Spacer(modifier = Modifier.width(16.dp))

                  Text(
                    text = name,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp,
                    color = if (isSelected) CinnabarRed else InkBlack
                  )
                }
              }
            } else {
              // Source list
              val sources = listOf("中國 Z39.50", "英國 Z39.50", "全球 SRU")
              sources.forEach { source ->
                val isSelected = source == selectedSource
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(if (isSelected) Color(0xFFFAF4DB) else Color.Transparent)
                    .clickable {
                      selectedSource = source
                      scope.launch { drawerState.close() }
                      Toast.makeText(context, "切換源至: $source", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(CinnabarRed)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                  } else {
                    Spacer(modifier = Modifier.width(16.dp))
                  }

                  val sourceEmoji = when (source) {
                    "中國 Z39.50" -> "⛩️"
                    "英國 Z39.50" -> "🏰"
                    else -> "🌐"
                  }
                  
                  val filter = remember(isSelected) {
                    if (isSelected) {
                      ColorFilter.tint(CinnabarRed)
                    } else {
                      ColorFilter.tint(InkBlack.copy(alpha = 0.7f))
                    }
                  }
                  
                  Box(
                    modifier = Modifier.width(28.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = sourceEmoji,
                      fontSize = 18.sp,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.graphicsLayer {
                        colorFilter = filter
                      }
                    )
                  }

                  Spacer(modifier = Modifier.width(16.dp))

                  Text(
                    text = source,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp,
                    color = if (isSelected) CinnabarRed else InkBlack
                  )
                }
              }
            }
          }

          // Bottom stamp in Drawer
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "脈望",
              fontFamily = FontFamily.Serif,
              fontSize = 20.sp,
              fontWeight = FontWeight.Light,
              color = InkBlack.copy(alpha = 0.3f),
              letterSpacing = 8.sp
            )
          }
        }
      }
    }
  ) {
    val isBackgroundBlurred = drawerState.isOpen || showShareDialog

    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
        modifier = Modifier.blur(if (isBackgroundBlurred) 12.dp else 0.dp),
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            if (currentTab == "校讎" && xiaochouSearchMode && xiaochouStep == 1) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(40.dp)
                  .background(Color.White)
                  .border(width = 0.5.dp, color = InkBlack)
                  .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
              ) {
                BasicTextField(
                  value = xiaochouSearchQuery,
                  onValueChange = { xiaochouSearchQuery = it },
                  textStyle = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = InkBlack
                  ),
                  cursorBrush = SolidColor(CinnabarRed),
                  modifier = Modifier.fillMaxWidth(),
                  decorationBox = { innerTextField ->
                    if (xiaochouSearchQuery.isEmpty()) {
                      Text(
                        text = "搜尋書名或作者...",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = InkBlack.copy(alpha = 0.3f)
                      )
                    }
                    innerTextField()
                  }
                )
              }
            } else {
              EmojiStatusMachine(
                currentTab = currentTab,
                isYiwenLoading = isYiwenLoading,
                isXiaochouLoading = isXiaochouLoading,
                isTupuLoading = isTupuLoading,
                xiaochouStep = xiaochouStep
              )
            }
          },
          navigationIcon = {
            if (currentTab == "圖譜") {
              IconButton(onClick = {
                showShareDialog = true
              }) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = "分享回顧卡片",
                  tint = InkBlack
                )
              }
            } else if (currentTab == "校讎") {
              if (xiaochouStep == 2) {
                IconButton(onClick = {
                  xiaochouStep = 1
                  selectedBookForDetails = null
                  xiaochouDeleteMode = false
                }) {
                  Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = InkBlack
                  )
                }
              } else {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                  Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "類別篩選",
                    tint = InkBlack
                  )
                }
              }
            } else {
              IconButton(onClick = { scope.launch { drawerState.open() } }) {
                Icon(
                  imageVector = Icons.Default.Menu,
                  contentDescription = "文献来源",
                  tint = InkBlack
                )
              }
            }
          },
          actions = {
            if (currentTab == "圖譜") {
              IconButton(onClick = {
                tupuRefreshTrigger++
              }) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "重新整理",
                  tint = InkBlack
                )
              }
            } else if (currentTab == "校讎") {
              if (xiaochouStep == 2) {
                var showMoreMenu by remember { mutableStateOf(false) }
                Box {
                  IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                      imageVector = Icons.Default.MoreVert,
                      contentDescription = "更多選項",
                      tint = InkBlack
                    )
                  }
                  DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier
                      .background(PaperWhite)
                      .border(width = 0.5.dp, color = PaperShadow)
                  ) {
                    DropdownMenuItem(
                      text = { Text("增加記錄", fontFamily = FontFamily.Serif, color = InkBlack) },
                      leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = InkBlack) },
                      onClick = {
                        showMoreMenu = false
                        selectedBookForDetails?.let { book ->
                          onItemClick(
                            com.example.maiwang.CreateHighlight(
                              title = book.title,
                              author = book.author,
                              imageUrl = book.cover_image_url,
                              sourceUrl = book.source,
                              isbn = ""
                            )
                          )
                        }
                      }
                    )
                    DropdownMenuItem(
                      text = { 
                        Text(
                          text = if (xiaochouDeleteMode) "退出刪除" else "刪除條目", 
                          fontFamily = FontFamily.Serif, 
                          color = if (xiaochouDeleteMode) CinnabarRed else InkBlack
                        ) 
                      },
                      leadingIcon = { 
                        Icon(
                          imageVector = if (xiaochouDeleteMode) Icons.Default.Close else Icons.Default.Delete, 
                          contentDescription = null, 
                          tint = if (xiaochouDeleteMode) CinnabarRed else InkBlack
                        ) 
                      },
                      onClick = {
                        showMoreMenu = false
                        xiaochouDeleteMode = !xiaochouDeleteMode
                      }
                    )
                  }
                }
              } else {
                IconButton(onClick = {
                  xiaochouSearchMode = !xiaochouSearchMode
                  if (!xiaochouSearchMode) {
                    xiaochouSearchQuery = ""
                  }
                }) {
                  Icon(
                    imageVector = if (xiaochouSearchMode) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "搜尋",
                    tint = InkBlack
                  )
                }
              }
            } else {
              IconButton(onClick = {
                onItemClick(History)
              }) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = "檢索歷史",
                  tint = InkBlack
                )
              }
            }
          },
          colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = PaperWhite
          ),
          modifier = Modifier.border(width = 0.5.dp, color = PaperShadow)
        )
      },
      bottomBar = {
        // Bottom Navigation Bar
        Column {
          Divider(color = PaperShadow, thickness = 0.5.dp)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(64.dp)
              .background(PaperWhite),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val tabs = listOf("藝文", "校讎", "圖譜")
            tabs.forEach { tab ->
              val isSelected = currentTab == tab
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .clickable { 
                    currentTab = tab 
                    xiaochouDeleteMode = false
                  }
                  .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = when (tab) {
                    "藝文" -> Icons.Default.Edit
                    "校讎" -> Icons.Default.MenuBook
                    else -> Icons.Default.DateRange
                  },
                  contentDescription = tab,
                  tint = if (isSelected) CinnabarRed else InkBlack.copy(alpha = 0.6f),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = tab,
                  fontFamily = FontFamily.Serif,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 12.sp,
                  color = if (isSelected) CinnabarRed else InkBlack
                )
              }
            }
          }
        }
      },
      containerColor = PaperWhite
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (currentTab) {
          "藝文" -> YiwenTab(
            isbnInput = isbnInput,
            onIsbnChange = {
              isbnInput = it
              if (it.isNotEmpty()) isbnError = null
            },
            isbnError = isbnError,
            onSearchClick = {
              if (isbnInput.trim().isEmpty()) {
                isbnError = "請輸入書籍 ISBN 號碼"
              } else {
                onItemClick(SearchResult(isbn = isbnInput.trim(), source = selectedSource))
              }
            },
            onScanClick = {
              onItemClick(Scanner)
            },
            onManualInputClick = {
              onItemClick(ManualInput)
            },
            poemText = poemText,
            poemTitle = poemTitle
          )
          "校讎" -> XiaochouTab(
            categoryFilter = xiaochouCategory,
            searchQuery = xiaochouSearchQuery,
            step = xiaochouStep,
            onStepChange = { xiaochouStep = it },
            selectedBook = selectedBookForDetails,
            onSelectedBookChange = { selectedBookForDetails = it },
            onLoadingChange = { isXiaochouLoading = it },
            deleteMode = xiaochouDeleteMode,
            onDeleteModeChange = { xiaochouDeleteMode = it }
          )
          "圖譜" -> TupuTab(
            refreshTrigger = tupuRefreshTrigger,
            onRefresh = { tupuRefreshTrigger++ },
            onWebViewCreated = { tupuWebViewRef = it },
            onLoadingChange = { isTupuLoading = it }
          )
        }
      }
    }

      // Frosted glass overlay (scrim)
      if (isBackgroundBlurred) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.25f))
        )
      }

      // Share Dialog
      if (showShareDialog) {
        AlertDialog(
          onDismissRequest = { showShareDialog = false },
          properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
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
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "分享回顧卡片",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CinnabarRed,
                letterSpacing = 2.sp
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "您可以將每日回顧卡片保存至系統相冊，或直接分享給好友。",
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = InkBlack.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(24.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(CinnabarRed)
                    .border(width = 1.dp, color = InkBlack)
                    .clickable {
                      showShareDialog = false
                      tupuWebViewRef?.let { webView ->
                        saveCardOnly(context, webView)
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "保存相冊",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                  )
                }
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(Color(0xFFFAF7EE))
                    .border(width = 1.dp, color = InkBlack)
                    .clickable {
                      showShareDialog = false
                      tupuWebViewRef?.let { webView ->
                        saveAndShareCard(context, webView)
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "直接分享",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = InkBlack
                  )
                }
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "取消",
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = InkBlack.copy(alpha = 0.5f),
                modifier = Modifier
                  .clickable { showShareDialog = false }
                  .padding(8.dp)
              )
            }
          }
        )
      }
    }
  }
}

@Composable
fun YiwenTab(
  isbnInput: String,
  onIsbnChange: (String) -> Unit,
  isbnError: String?,
  onSearchClick: () -> Unit,
  onScanClick: () -> Unit,
  onManualInputClick: () -> Unit,
  poemText: String,
  poemTitle: String
) {
  val context = LocalContext.current
  val svgImageLoader = remember {
    ImageLoader.Builder(context)
      .components {
        add(SvgDecoder.Factory())
      }
      .build()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 32.dp),
    contentAlignment = Alignment.Center
  ) {
    // Watermark Background SVG
    AsyncImage(
      model = "file:///android_asset/background.svg",
      contentDescription = null,
      imageLoader = svgImageLoader,
      modifier = Modifier
        .fillMaxSize()
        .alpha(0.08f),
      contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
    )

    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Quote Section
      Text(
        text = poemText,
        style = QuoteTextStyle,
        color = InkBlack,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = poemTitle,
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        color = InkBlack.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(48.dp))

      // ISBN Input
      Column(modifier = Modifier.fillMaxWidth()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(width = 1.dp, color = if (isbnError != null) CinnabarRed else InkBlack)
            .background(Color.White)
            .padding(horizontal = 16.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
          ) {
            // Weighted line textfield
            BasicTextField(
              value = isbnInput,
              onValueChange = onIsbnChange,
              textStyle = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = InkBlack
              ),
              cursorBrush = SolidColor(CinnabarRed),
              modifier = Modifier.weight(1f),
              decorationBox = { innerTextField ->
                if (isbnInput.isEmpty()) {
                  Text(
                    text = "請輸入書籍 ISBN 號碼",
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = InkBlack.copy(alpha = 0.3f)
                  )
                }
                innerTextField()
              }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Barcode icon
            Icon(
              imageVector = Icons.Default.QrCodeScanner,
              contentDescription = "扫描条码",
              tint = InkBlack,
              modifier = Modifier
                .size(24.dp)
                .clickable { onScanClick() }
            )
          }
        }

        if (isbnError != null) {
          Text(
            text = isbnError,
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            color = CinnabarRed,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Combined Row for Search and Manual Input
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Search Button (Chop / Seal style, red background)
        Box(
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .background(CinnabarRed)
            .border(width = 1.dp, color = InkBlack)
            .clickable { onSearchClick() },
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = "檢索",
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color.White,
              letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Default.ArrowForward,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        // Manual Input Button (Woodblock color/Light background, black text)
        Box(
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .border(width = 1.dp, color = InkBlack)
            .background(Color(0xFFFAF7EE))
            .clickable { onManualInputClick() },
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = null,
              tint = InkBlack,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "手動輸入",
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = InkBlack,
              letterSpacing = 2.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(48.dp))

      // Red seal logo stamp SVG
      AsyncImage(
        model = "file:///android_asset/stamp.svg",
        contentDescription = "印章",
        imageLoader = svgImageLoader,
        modifier = Modifier.size(54.dp)
      )
    }
  }
}


sealed interface TupuState {
  object Loading : TupuState
  data class Success(val html: String) : TupuState
  data class Error(val message: String) : TupuState
  object NoToken : TupuState
}

@Composable
fun TupuTab(
  refreshTrigger: Int,
  onRefresh: () -> Unit,
  onWebViewCreated: (WebView) -> Unit,
  onLoadingChange: (Boolean) -> Unit
) {
  val context = LocalContext.current
  val settingsRepository = remember { SettingsRepository(context) }
  var tupuState by remember { mutableStateOf<TupuState>(TupuState.Loading) }

  LaunchedEffect(tupuState) {
    onLoadingChange(tupuState is TupuState.Loading)
  }

  LaunchedEffect(refreshTrigger) {
    tupuState = TupuState.Loading
    val token = settingsRepository.getReadwiseToken().trim()
    if (token.isEmpty()) {
      tupuState = TupuState.NoToken
      return@LaunchedEffect
    }

    try {
      val html = DailyReviewCardGenerator.generateHtml(context, token)
      if (isActive) {
        tupuState = TupuState.Success(html)
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      e.printStackTrace()
      if (isActive) {
        tupuState = TupuState.Error(e.message ?: "生成回顧卡片失敗")
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFFAF7EE)),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      when (val state = tupuState) {
        TupuState.Loading -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(color = CinnabarRed)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "正在載入每日回顧，請稍候...",
              fontFamily = FontFamily.Serif,
              fontSize = 16.sp,
              color = InkBlack.copy(alpha = 0.6f)
            )
          }
        }
        TupuState.NoToken -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = CinnabarRed,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "未配置 Readwise API Token",
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = InkBlack,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "請在主页右上角“設定”中配置合法的 Readwise Token，以便同步與回顧您的讀書筆記。",
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.6f),
              textAlign = TextAlign.Center,
              lineHeight = 24.sp
            )
          }
        }
        is TupuState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = CinnabarRed,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "載入失敗",
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = InkBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = state.message,
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.6f),
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
              onClick = onRefresh,
              colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.border(width = 1.dp, color = InkBlack)
            ) {
              Text("重试", fontFamily = FontFamily.Serif, color = Color.White)
            }
          }
        }
        is TupuState.Success -> {
          AndroidView(
            factory = { ctx ->
              WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.allowFileAccess = true
                webViewClient = WebViewClient()
                setBackgroundColor(0) // Transparent background
                onWebViewCreated(this)
              }
            },
            update = { webView ->
              webView.loadDataWithBaseURL("file:///android_asset/", state.html, "text/html", "utf-8", null)
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}

fun saveAndShareCard(context: android.content.Context, webView: WebView) {
  try {
    val width = webView.width
    if (width <= 0) {
      Toast.makeText(context, "正在加載，請稍候...", Toast.LENGTH_SHORT).show()
      return
    }

    val height = (width * (1570.0 / 1080.0)).toInt()

    Toast.makeText(context, "正在生成卡片...", Toast.LENGTH_SHORT).show()
    webView.evaluateJavascript("setExportMode(true)", null)

    webView.postDelayed({
      try {
        // Draw the WebView content directly at its current natural screen-width scale
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        webView.draw(canvas)

        // Restore HTML original padding and view alignment
        webView.evaluateJavascript("setExportMode(false)", null)

        val resolver = context.contentResolver
        val filename = "maiwang_review_${System.currentTimeMillis()}"
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.png")
          put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/脉望")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
          }
        }

        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri == null) {
          Toast.makeText(context, "無法保存卡片圖片", Toast.LENGTH_SHORT).show()
          bitmap.recycle()
          return@postDelayed
        }

        resolver.openOutputStream(imageUri).use { out ->
          if (out != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
          }
        }
        bitmap.recycle()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
          resolver.update(imageUri, contentValues, null, null)
        }

        Toast.makeText(context, "卡片已保存至相冊", Toast.LENGTH_SHORT).show()

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
          type = "image/png"
          putExtra(android.content.Intent.EXTRA_STREAM, imageUri)
          addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "分享回顧卡片"))
      } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "分享失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        webView.evaluateJavascript("setExportMode(false)", null)
      }
    }, 150)
  } catch (e: Exception) {
    e.printStackTrace()
    Toast.makeText(context, "分享失敗: ${e.message}", Toast.LENGTH_SHORT).show()
  }
}

fun saveCardOnly(context: android.content.Context, webView: WebView) {
  try {
    val width = webView.width
    if (width <= 0) {
      Toast.makeText(context, "正在加載，請稍候...", Toast.LENGTH_SHORT).show()
      return
    }

    val height = (width * (1570.0 / 1080.0)).toInt()

    Toast.makeText(context, "正在生成卡片...", Toast.LENGTH_SHORT).show()
    webView.evaluateJavascript("setExportMode(true)", null)

    webView.postDelayed({
      try {
        // Draw the WebView content directly at its current natural screen-width scale
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        webView.draw(canvas)

        // Restore HTML original padding and view alignment
        webView.evaluateJavascript("setExportMode(false)", null)

        val resolver = context.contentResolver
        val filename = "maiwang_review_${System.currentTimeMillis()}"
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.png")
          put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/脉望")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
          }
        }

        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri == null) {
          Toast.makeText(context, "無法保存卡片圖片", Toast.LENGTH_SHORT).show()
          bitmap.recycle()
          return@postDelayed
        }

        resolver.openOutputStream(imageUri).use { out ->
          if (out != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
          }
        }
        bitmap.recycle()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
          resolver.update(imageUri, contentValues, null, null)
        }

        Toast.makeText(context, "卡片已保存至相冊", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "保存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        webView.evaluateJavascript("setExportMode(false)", null)
      }
    }, 150)
  } catch (e: Exception) {
    e.printStackTrace()
    Toast.makeText(context, "保存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
  }
}
