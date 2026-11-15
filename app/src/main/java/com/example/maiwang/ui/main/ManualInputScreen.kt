package com.example.maiwang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.maiwang.CreateHighlight
import com.example.maiwang.data.MetadataFetcher
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperShadow
import com.example.maiwang.theme.PaperWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(
  onBack: () -> Unit,
  onNavigate: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  var titleInput by remember { mutableStateOf("") }
  var authorInput by remember { mutableStateOf("") }

  var titleError by remember { mutableStateOf<String?>(null) }
  var authorError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "手動錄入",
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
          IconButton(
            onClick = {
              var hasError = false
              if (titleInput.trim().isEmpty()) {
                titleError = "書籍題名不能爲空"
                hasError = true
              }
              if (authorInput.trim().isEmpty()) {
                authorError = "責任者不能爲空"
                hasError = true
              }

              if (!hasError) {
                onNavigate(
                  CreateHighlight(
                    title = titleInput.trim(),
                    author = authorInput.trim(),
                    imageUrl = MetadataFetcher.getRandomPlaceholder(),
                    sourceUrl = null,
                    isbn = null
                  )
                )
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "確認",
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
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top
    ) {
      Text(
        text = "錄入書籍信息",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = InkBlack,
        modifier = Modifier.padding(bottom = 32.dp)
      )

      // Book Card Form Layout
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
          .fillMaxWidth()
          .border(width = 0.5.dp, color = PaperShadow)
          .padding(bottom = 32.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
          // Title Field
          Column {
            Text(
              text = "題名 (書名)",
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.6f),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White)
                .border(width = 1.dp, color = if (titleError != null) CinnabarRed else InkBlack.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = titleInput,
                onValueChange = {
                  titleInput = it
                  if (it.isNotEmpty()) titleError = null
                },
                textStyle = TextStyle(
                  fontFamily = FontFamily.Serif,
                  fontSize = 16.sp,
                  color = InkBlack
                ),
                cursorBrush = SolidColor(CinnabarRed),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                  if (titleInput.isEmpty()) {
                    Text(
                      text = "請輸入書籍題名（必填）",
                      fontFamily = FontFamily.Serif,
                      fontSize = 16.sp,
                      color = InkBlack.copy(alpha = 0.3f)
                    )
                  }
                  innerTextField()
                }
              )
            }
            if (titleError != null) {
              Text(
                text = titleError!!,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = CinnabarRed,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }

          // Author Field
          Column {
            Text(
              text = "責任者 (作者/編者)",
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.6f),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White)
                .border(width = 1.dp, color = if (authorError != null) CinnabarRed else InkBlack.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = authorInput,
                onValueChange = {
                  authorInput = it
                  if (it.isNotEmpty()) authorError = null
                },
                textStyle = TextStyle(
                  fontFamily = FontFamily.Serif,
                  fontSize = 16.sp,
                  color = InkBlack
                ),
                cursorBrush = SolidColor(CinnabarRed),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                  if (authorInput.isEmpty()) {
                    Text(
                      text = "請輸入主要責任者（必填）",
                      fontFamily = FontFamily.Serif,
                      fontSize = 16.sp,
                      color = InkBlack.copy(alpha = 0.3f)
                    )
                  }
                  innerTextField()
                }
              )
            }
            if (authorError != null) {
              Text(
                text = authorError!!,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                color = CinnabarRed,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
        }
      }

    }
  }
}
