package com.example.maiwang.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maiwang.data.SettingsRepository
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperShadow
import com.example.maiwang.theme.PaperWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current
  val repository = remember { SettingsRepository(context) }
  var tokenInput by remember { mutableStateOf(repository.getReadwiseToken()) }
  var baiduApiKeyInput by remember { mutableStateOf(repository.getBaiduApiKey()) }
  var baiduSecretKeyInput by remember { mutableStateOf(repository.getBaiduSecretKey()) }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "藝文設置",
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
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top
    ) {
      Text(
        text = "配置 Readwise API",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = InkBlack,
        modifier = Modifier.padding(bottom = 24.dp)
      )

      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
          .fillMaxWidth()
          .border(width = 0.5.dp, color = PaperShadow)
          .padding(bottom = 32.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Token Label and Textfield
          Column {
            Text(
              text = "Readwise Access Token",
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.7f),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White)
                .border(width = 1.dp, color = InkBlack.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                textStyle = TextStyle(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 14.sp,
                  color = InkBlack
                ),
                cursorBrush = SolidColor(CinnabarRed),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                  if (tokenInput.isEmpty()) {
                    Text(
                      text = "請輸入您的 Readwise Token",
                      fontFamily = FontFamily.Serif,
                      fontSize = 14.sp,
                      color = InkBlack.copy(alpha = 0.3f)
                    )
                  }
                  innerTextField()
                }
              )
            }
          }

          // Link to get token
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                try {
                  uriHandler.openUri("https://readwise.io/access_token")
                } catch (e: Exception) {
                  Toast.makeText(context, "無法打開瀏覽器", Toast.LENGTH_SHORT).show()
                }
              },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = CinnabarRed,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "點擊此處獲取您的 Readwise Token",
              fontFamily = FontFamily.Serif,
              fontSize = 13.sp,
              color = CinnabarRed,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "配置百度 OCR API",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = InkBlack,
        modifier = Modifier.padding(bottom = 24.dp)
      )

      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
          .fillMaxWidth()
          .border(width = 0.5.dp, color = PaperShadow)
          .padding(bottom = 32.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Baidu API Key
          Column {
            Text(
              text = "Baidu API Key (Client ID)",
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.7f),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White)
                .border(width = 1.dp, color = InkBlack.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = baiduApiKeyInput,
                onValueChange = { baiduApiKeyInput = it },
                textStyle = TextStyle(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 14.sp,
                  color = InkBlack
                ),
                cursorBrush = SolidColor(CinnabarRed),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                  if (baiduApiKeyInput.isEmpty()) {
                    Text(
                      text = "請輸入您的 Baidu API Key",
                      fontFamily = FontFamily.Serif,
                      fontSize = 14.sp,
                      color = InkBlack.copy(alpha = 0.3f)
                    )
                  }
                  innerTextField()
                }
              )
            }
          }

          // Baidu Secret Key
          Column {
            Text(
              text = "Baidu Secret Key (Client Secret)",
              fontFamily = FontFamily.Serif,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.7f),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White)
                .border(width = 1.dp, color = InkBlack.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = baiduSecretKeyInput,
                onValueChange = { baiduSecretKeyInput = it },
                textStyle = TextStyle(
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 14.sp,
                  color = InkBlack
                ),
                cursorBrush = SolidColor(CinnabarRed),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                  if (baiduSecretKeyInput.isEmpty()) {
                    Text(
                      text = "請輸入您的 Baidu Secret Key",
                      fontFamily = FontFamily.Serif,
                      fontSize = 14.sp,
                      color = InkBlack.copy(alpha = 0.3f)
                    )
                  }
                  innerTextField()
                }
              )
            }
          }

          // Link to get Baidu API keys
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                try {
                  uriHandler.openUri("https://console.bce.baidu.com/ai/#/ai/ocr/overview/index")
                } catch (e: Exception) {
                  Toast.makeText(context, "無法打開瀏覽器", Toast.LENGTH_SHORT).show()
                }
              },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = CinnabarRed,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "點擊此處前往百度智能雲獲取 OCR 密鑰",
              fontFamily = FontFamily.Serif,
              fontSize = 13.sp,
              color = CinnabarRed,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Back
        Box(
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .border(width = 0.5.dp, color = InkBlack)
            .background(Color(0xFFFAF7EE))
            .clickable { onBack() },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "取消",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = InkBlack
          )
        }

        // Save
        Box(
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .background(CinnabarRed)
            .border(width = 1.dp, color = InkBlack)
            .clickable {
              repository.saveReadwiseToken(tokenInput.trim())
              repository.saveBaiduApiKey(baiduApiKeyInput.trim())
              repository.saveBaiduSecretKey(baiduSecretKeyInput.trim())
              Toast.makeText(context, "設置已保存！", Toast.LENGTH_SHORT).show()
              onBack()
            },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "保存",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
          )
        }
      }
    }
  }
}
