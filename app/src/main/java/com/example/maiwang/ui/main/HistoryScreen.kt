package com.example.maiwang.ui.main

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.maiwang.SearchResult
import com.example.maiwang.Settings
import com.example.maiwang.data.HistoryItem
import com.example.maiwang.data.SearchHistoryRepository
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperShadow
import com.example.maiwang.theme.PaperWhite
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  onBack: () -> Unit,
  onNavigate: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val repository = remember { SearchHistoryRepository(context) }
  var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
  var showClearDialog by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    historyList = repository.getHistory()
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "檢索歷史",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = InkBlack
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
        actions = {
          IconButton(onClick = { onNavigate(Settings) }) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "设置",
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
      if (historyList.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "暫無檢索歷史",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = InkBlack.copy(alpha = 0.4f)
          )
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
        ) {
          // Clear history button
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.clickable { showClearDialog = true },
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = InkBlack.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "清空歷史記錄",
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = InkBlack.copy(alpha = 0.7f)
              )
            }
          }

          Divider(color = PaperShadow, thickness = 0.5.dp)

          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(historyList) { item ->
              HistoryCard(
                item = item,
                onClick = {
                  // Both success and failure direct to search result screen (failed can retry)
                  onNavigate(SearchResult(isbn = item.isbn, source = item.source))
                }
              )
            }
          }
        }
      }
    }
  }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = {
        Text(
          text = "確認清空歷史？",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = InkBlack
        )
      },
      text = {
        Text(
          text = "清空後歷史檢索記錄將無法恢復。",
          fontFamily = FontFamily.Serif,
          fontSize = 14.sp,
          color = InkBlack
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            scope.launch {
              repository.clearHistory()
              historyList = emptyList()
              showClearDialog = false
            }
          }
        ) {
          Text(
            text = "確定",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = CinnabarRed
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text(
            text = "取消",
            fontFamily = FontFamily.Serif,
            color = InkBlack
          )
        }
      },
      containerColor = PaperWhite,
      shape = MaterialTheme.shapes.small
    )
  }
}

@Composable
fun HistoryCard(
  item: HistoryItem,
  onClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
    shape = MaterialTheme.shapes.small,
    modifier = Modifier
      .fillMaxWidth()
      .border(width = 0.5.dp, color = PaperShadow)
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left Status Block Indicator
      Box(
        modifier = Modifier
          .size(12.dp)
          .border(width = 1.dp, color = if (item.success) CinnabarRed else InkBlack.copy(alpha = 0.4f))
          .background(if (item.success) CinnabarRed else Color.Transparent)
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "ISBN ${item.isbn}",
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            color = InkBlack.copy(alpha = 0.6f)
          )
          Text(
            text = formatTime(item.timestamp),
            fontFamily = FontFamily.Serif,
            fontSize = 11.sp,
            color = InkBlack.copy(alpha = 0.5f)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = item.title,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = InkBlack,
          fontStyle = if (!item.success) FontStyle.Italic else FontStyle.Normal
        )

        item.subtitle?.let {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = it,
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            color = if (!item.success) CinnabarRed.copy(alpha = 0.8f) else InkBlack.copy(alpha = 0.7f),
            fontWeight = if (!item.success) FontWeight.Medium else FontWeight.Normal
          )
        }
      }
    }
  }
}

private fun formatTime(timeMs: Long): String {
  val now = Calendar.getInstance()
  val itemTime = Calendar.getInstance().apply { timeInMillis = timeMs }

  val nowDay = now.get(Calendar.DAY_OF_YEAR)
  val nowYear = now.get(Calendar.YEAR)
  val itemDay = itemTime.get(Calendar.DAY_OF_YEAR)
  val itemYear = itemTime.get(Calendar.YEAR)

  return when {
    nowYear == itemYear && nowDay == itemDay -> {
      DateFormat.format("hh:mm a", timeMs).toString()
    }
    nowYear == itemYear && nowDay - itemDay == 1 -> {
      "昨天"
    }
    else -> {
      DateFormat.format("M月d日", timeMs).toString()
    }
  }
}
