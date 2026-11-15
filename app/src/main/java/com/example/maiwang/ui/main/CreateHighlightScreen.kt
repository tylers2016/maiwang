package com.example.maiwang.ui.main

import android.net.Uri
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.maiwang.data.BaiduOcrHelper
import com.example.maiwang.data.BaiduOcrWord
import com.example.maiwang.data.SettingsRepository
import com.example.maiwang.data.ReadwiseHighlight
import com.example.maiwang.data.ReadwiseRepository
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperShadow
import com.example.maiwang.theme.PaperWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class HighlightStep {
  SELECTION,
  CROP,
  OCR_SCAN,
  CONFIRM
}


fun getImageOrientation(context: android.content.Context, uri: Uri): Int {
  return try {
    context.contentResolver.openInputStream(uri)?.use { input ->
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        val exifInterface = android.media.ExifInterface(input)
        exifInterface.getAttributeInt(
          android.media.ExifInterface.TAG_ORIENTATION,
          android.media.ExifInterface.ORIENTATION_NORMAL
        )
      } else {
        android.media.ExifInterface.ORIENTATION_NORMAL
      }
    } ?: android.media.ExifInterface.ORIENTATION_NORMAL
  } catch (e: Exception) {
    android.media.ExifInterface.ORIENTATION_NORMAL
  }
}

fun rotateAndCropImage(
  context: android.content.Context,
  inputUri: Uri,
  cropRectPercent: RectF
): Uri? {
  return try {
    val orientation = getImageOrientation(context, inputUri)
    val matrix = android.graphics.Matrix()
    when (orientation) {
      android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }

    var bitmap: android.graphics.Bitmap? = null
    context.contentResolver.openInputStream(inputUri)?.use { input ->
      bitmap = android.graphics.BitmapFactory.decodeStream(input)
    }
    val originalBitmap = bitmap ?: return null

    val rotatedBitmap = android.graphics.Bitmap.createBitmap(
      originalBitmap,
      0,
      0,
      originalBitmap.width,
      originalBitmap.height,
      matrix,
      true
    )
    if (rotatedBitmap != originalBitmap) {
      originalBitmap.recycle()
    }

    val left = (cropRectPercent.left * rotatedBitmap.width).toInt().coerceIn(0, rotatedBitmap.width - 1)
    val top = (cropRectPercent.top * rotatedBitmap.height).toInt().coerceIn(0, rotatedBitmap.height - 1)
    val right = (cropRectPercent.right * rotatedBitmap.width).toInt().coerceIn(left + 1, rotatedBitmap.width)
    val bottom = (cropRectPercent.bottom * rotatedBitmap.height).toInt().coerceIn(top + 1, rotatedBitmap.height)
    
    val croppedBitmap = android.graphics.Bitmap.createBitmap(
      rotatedBitmap,
      left,
      top,
      right - left,
      bottom - top
    )
    rotatedBitmap.recycle()

    val outFile = createTempPhotoFile(context)
    outFile.outputStream().use { out ->
      croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
    }
    croppedBitmap.recycle()

    Uri.fromFile(outFile)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun createTempPhotoFile(context: android.content.Context): File {
  return File.createTempFile("maiwang_ocr_", ".jpg", context.cacheDir).apply {
    createNewFile()
    deleteOnExit()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHighlightScreen(
  title: String,
  author: String?,
  imageUrl: String?,
  sourceUrl: String?,
  isbn: String?,
  onBack: () -> Unit,
  onSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val settingsRepository = remember { SettingsRepository(context) }
  val readwiseRepository = remember { ReadwiseRepository(context) }

  // Steps state
  var currentStep by remember { mutableStateOf(HighlightStep.SELECTION) }
  var isManualInput by remember { mutableStateOf(false) }

  // Camera & Image URI
  var tempFile by remember { mutableStateOf<File?>(null) }
  var photoUri by remember { mutableStateOf<Uri?>(null) }

  // OCR specific states
  var ocrProcessing by remember { mutableStateOf(false) }
  var ocrError by remember { mutableStateOf<String?>(null) }
  var ocrLines by remember { mutableStateOf<List<BaiduOcrWord>>(emptyList()) }
  var selectedOcrLines by remember { mutableStateOf<Set<Int>>(emptySet()) }
  var nativeWidth by remember { mutableStateOf(3) }
  var nativeHeight by remember { mutableStateOf(4) }

  // Final confirmation step fields
  var highlightText by remember { mutableStateOf("") }
  var noteText by remember { mutableStateOf("") }
  var pageText by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var textError by remember { mutableStateOf<String?>(null) }

  // Crop window coordinates
  var cropLeft by remember { mutableStateOf(0.1f) }
  var cropTop by remember { mutableStateOf(0.2f) }
  var cropRight by remember { mutableStateOf(0.9f) }
  var cropBottom by remember { mutableStateOf(0.8f) }

  // Camera Launcher
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
    onResult = { success ->
      if (success && photoUri != null) {
        currentStep = HighlightStep.CROP
        // Reset crop bounds
        cropLeft = 0.1f
        cropTop = 0.2f
        cropRight = 0.9f
        cropBottom = 0.8f

        try {
          // Read native image dimensions and orientation
          val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
          }
          context.contentResolver.openInputStream(photoUri!!)?.use { inputStream ->
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
          }
          if (options.outWidth > 0 && options.outHeight > 0) {
            val orientation = getImageOrientation(context, photoUri!!)
            val isRotated = orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90 ||
                            orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270
            if (isRotated) {
              nativeWidth = options.outHeight
              nativeHeight = options.outWidth
            } else {
              nativeWidth = options.outWidth
              nativeHeight = options.outHeight
            }
          } else {
            nativeWidth = 3
            nativeHeight = 4
          }
        } catch (e: Exception) {
          e.printStackTrace()
          nativeWidth = 3
          nativeHeight = 4
        }
      } else {
        // Cancelled or failed, revert to selection
        currentStep = HighlightStep.SELECTION
      }
    }
  )

  // Launch Camera helper
  val triggerCamera = {
    val apiKey = settingsRepository.getBaiduApiKey().trim()
    val secretKey = settingsRepository.getBaiduSecretKey().trim()
    if (apiKey.isEmpty() || secretKey.isEmpty()) {
      Toast.makeText(context, "請先在設置中配置百度 OCR API 密鑰", Toast.LENGTH_LONG).show()
    } else {
      try {
        val file = createTempPhotoFile(context)
        tempFile = file
        val uri = FileProvider.getUriForFile(context, "com.example.maiwang.fileprovider", file)
        photoUri = uri
        cameraLauncher.launch(uri)
      } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "無法初始化相機: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Root Box to handle overlays
  Box(modifier = Modifier.fillMaxSize().background(PaperWhite)) {
    // Backdrop background content (blurs when selection step is open)
    val blurModifier = if (currentStep == HighlightStep.SELECTION) Modifier.blur(4.dp) else Modifier
    
    Column(
      modifier = modifier
        .fillMaxSize()
        .then(blurModifier)
    ) {
      when (currentStep) {
        HighlightStep.SELECTION -> {
          // Placeholder structure underneath the overlay
          Scaffold(
            topBar = {
              CenterAlignedTopAppBar(
                title = { Text("建立書摘", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = InkBlack) },
                navigationIcon = {
                  IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = InkBlack) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PaperWhite),
                modifier = Modifier.border(width = 0.5.dp, color = PaperShadow)
              )
            },
            containerColor = PaperWhite
          ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
          }
        }

        HighlightStep.CROP -> {
          Scaffold(
            topBar = {
              CenterAlignedTopAppBar(
                title = {
                  Text(
                    text = "裁剪文字區域",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = InkBlack
                  )
                },
                navigationIcon = {
                  IconButton(onClick = { currentStep = HighlightStep.SELECTION }) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "返回",
                      tint = InkBlack
                    )
                  }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PaperWhite),
                modifier = Modifier.border(width = 0.5.dp, color = PaperShadow)
              )
            },
            containerColor = PaperWhite
          ) { innerPadding ->
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "請拖動四角標記，僅保留需要識別的文字區域",
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = InkBlack.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
              )

              val photoAspectRatio = nativeWidth.toFloat() / nativeHeight.toFloat()
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .background(Color(0xFFFAF7EE))
                  .border(width = 0.5.dp, color = PaperShadow)
                  .padding(8.dp),
                contentAlignment = Alignment.Center
              ) {
                BoxWithConstraints(
                  modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(photoAspectRatio)
                    .border(width = 0.5.dp, color = InkBlack)
                ) {
                  val widthPx = constraints.maxWidth.toFloat()
                  val heightPx = constraints.maxHeight.toFloat()

                  AsyncImage(
                    model = photoUri,
                    contentDescription = "To crop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                  )

                  Canvas(modifier = Modifier.fillMaxSize()) {
                    val left = cropLeft * size.width
                    val top = cropTop * size.height
                    val right = cropRight * size.width
                    val bottom = cropBottom * size.height

                    val path = Path().apply {
                      addRect(Rect(left, top, right, bottom))
                    }
                    clipPath(path = path, clipOp = ClipOp.Difference) {
                      drawRect(color = Color.Black.copy(alpha = 0.5f))
                    }
                    drawRect(
                      color = CinnabarRed,
                      topLeft = Offset(left, top),
                      size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                      style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                  }

                  val handleSize = 36.dp

                  // Top-Left Handle
                  Box(
                    modifier = Modifier
                      .offset(
                        x = (cropLeft * maxWidth.value - handleSize.value / 2).dp,
                        y = (cropTop * maxHeight.value - handleSize.value / 2).dp
                      )
                      .size(handleSize)
                      .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                          change.consume()
                          cropLeft = (cropLeft + dragAmount.x / size.width).coerceIn(0f, cropRight - 0.1f)
                          cropTop = (cropTop + dragAmount.y / size.height).coerceIn(0f, cropBottom - 0.1f)
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Box(modifier = Modifier.size(14.dp).background(Color.White, shape = androidx.compose.foundation.shape.CircleShape).border(2.dp, CinnabarRed, shape = androidx.compose.foundation.shape.CircleShape))
                  }

                  // Top-Right Handle
                  Box(
                    modifier = Modifier
                      .offset(
                        x = (cropRight * maxWidth.value - handleSize.value / 2).dp,
                        y = (cropTop * maxHeight.value - handleSize.value / 2).dp
                      )
                      .size(handleSize)
                      .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                          change.consume()
                          cropRight = (cropRight + dragAmount.x / size.width).coerceIn(cropLeft + 0.1f, 1f)
                          cropTop = (cropTop + dragAmount.y / size.height).coerceIn(0f, cropBottom - 0.1f)
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Box(modifier = Modifier.size(14.dp).background(Color.White, shape = androidx.compose.foundation.shape.CircleShape).border(2.dp, CinnabarRed, shape = androidx.compose.foundation.shape.CircleShape))
                  }

                  // Bottom-Left Handle
                  Box(
                    modifier = Modifier
                      .offset(
                        x = (cropLeft * maxWidth.value - handleSize.value / 2).dp,
                        y = (cropBottom * maxHeight.value - handleSize.value / 2).dp
                      )
                      .size(handleSize)
                      .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                          change.consume()
                          cropLeft = (cropLeft + dragAmount.x / size.width).coerceIn(0f, cropRight - 0.1f)
                          cropBottom = (cropBottom + dragAmount.y / size.height).coerceIn(cropTop + 0.1f, 1f)
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Box(modifier = Modifier.size(14.dp).background(Color.White, shape = androidx.compose.foundation.shape.CircleShape).border(2.dp, CinnabarRed, shape = androidx.compose.foundation.shape.CircleShape))
                  }

                  // Bottom-Right Handle
                  Box(
                    modifier = Modifier
                      .offset(
                        x = (cropRight * maxWidth.value - handleSize.value / 2).dp,
                        y = (cropBottom * maxHeight.value - handleSize.value / 2).dp
                      )
                      .size(handleSize)
                      .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                          change.consume()
                          cropRight = (cropRight + dragAmount.x / size.width).coerceIn(cropLeft + 0.1f, 1f)
                          cropBottom = (cropBottom + dragAmount.y / size.height).coerceIn(cropTop + 0.1f, 1f)
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Box(modifier = Modifier.size(14.dp).background(Color.White, shape = androidx.compose.foundation.shape.CircleShape).border(2.dp, CinnabarRed, shape = androidx.compose.foundation.shape.CircleShape))
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                // Re-take photo
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .border(width = 0.5.dp, color = InkBlack)
                    .background(Color(0xFFFAF7EE))
                    .clickable(onClick = triggerCamera),
                  contentAlignment = Alignment.Center
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = InkBlack, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重新拍照", fontFamily = FontFamily.Serif, fontSize = 15.sp, color = InkBlack)
                  }
                }

                // Confirm crop
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .background(CinnabarRed)
                    .border(width = 1.dp, color = InkBlack)
                    .clickable {
                      val cropRect = RectF(cropLeft, cropTop, cropRight, cropBottom)
                      ocrProcessing = true
                      currentStep = HighlightStep.OCR_SCAN
                      coroutineScope.launch {
                        val croppedUri = withContext(Dispatchers.IO) {
                          rotateAndCropImage(context, photoUri!!, cropRect)
                        }
                        if (croppedUri != null) {
                          photoUri = croppedUri

                          // Get cropped dimensions
                          val options = android.graphics.BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                          }
                          context.contentResolver.openInputStream(croppedUri)?.use { inputStream ->
                            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                          }
                          nativeWidth = options.outWidth
                          nativeHeight = options.outHeight

                          val apiKey = settingsRepository.getBaiduApiKey().trim()
                          val secretKey = settingsRepository.getBaiduSecretKey().trim()
                          if (apiKey.isEmpty() || secretKey.isEmpty()) {
                            ocrError = "請先在設置中配置百度 OCR API 密鑰"
                            ocrProcessing = false
                            return@launch
                          }

                          val bytes = context.contentResolver.openInputStream(croppedUri)?.use { it.readBytes() }
                          if (bytes == null) {
                            ocrError = "無法讀取裁剪後的照片"
                            ocrProcessing = false
                            return@launch
                          }

                          val accessToken = BaiduOcrHelper.getAccessToken(apiKey, secretKey)
                          if (accessToken.isNullOrEmpty()) {
                            ocrError = "百度 OCR 鑒權失敗，請檢查 API Key / Secret Key"
                            ocrProcessing = false
                            return@launch
                          }

                          val result = BaiduOcrHelper.performOcr(accessToken, bytes)
                          if (result.isSuccess) {
                            val lines = result.getOrNull() ?: emptyList()
                            if (lines.isEmpty()) {
                              ocrError = "未在照片中識別出任何文字"
                            } else {
                              ocrLines = lines
                              selectedOcrLines = lines.indices.toSet()
                            }
                          } else {
                            ocrError = result.exceptionOrNull()?.message ?: "OCR 請求失敗"
                          }
                        } else {
                          ocrError = "處理照片裁剪失敗"
                        }
                        ocrProcessing = false
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("確認裁剪並識別", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                  }
                }
              }
            }
          }
        }

        HighlightStep.OCR_SCAN -> {
          Scaffold(
            topBar = {
              CenterAlignedTopAppBar(
                title = {
                  Text(
                    text = "文字識別 (OCR)",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = InkBlack
                  )
                },
                navigationIcon = {
                  IconButton(onClick = { currentStep = HighlightStep.CROP }) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "返回",
                      tint = InkBlack
                    )
                  }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PaperWhite),
                modifier = Modifier.border(width = 0.5.dp, color = PaperShadow)
              )
            },
            containerColor = PaperWhite
          ) { innerPadding ->
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              if (ocrProcessing) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  CircularProgressIndicator(color = CinnabarRed)
                  Spacer(modifier = Modifier.height(16.dp))
                  Text(
                    text = "正在進行高精度文字識別，請稍候...",
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = InkBlack.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                  )
                }
              } else if (ocrError != null) {
                Column(
                  modifier = Modifier.weight(1f),
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
                    text = ocrError!!,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = InkBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                  )
                  Spacer(modifier = Modifier.height(24.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                      onClick = { currentStep = HighlightStep.SELECTION },
                      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                      shape = MaterialTheme.shapes.small,
                      modifier = Modifier.border(width = 0.5.dp, color = InkBlack)
                    ) {
                      Text("返回選擇", fontFamily = FontFamily.Serif, color = InkBlack)
                    }
                    Button(
                      onClick = triggerCamera,
                      colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
                      shape = MaterialTheme.shapes.small,
                      modifier = Modifier.border(width = 1.dp, color = InkBlack)
                    ) {
                      Text("重新拍照", fontFamily = FontFamily.Serif, color = Color.White)
                    }
                  }
                }
              } else {
                Text(
                  text = "點選取需要的文字行（默認全選）：",
                  fontFamily = FontFamily.Serif,
                  fontSize = 14.sp,
                  color = InkBlack.copy(alpha = 0.7f),
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFFAF7EE))
                    .border(width = 0.5.dp, color = PaperShadow)
                    .padding(8.dp),
                  contentAlignment = Alignment.Center
                ) {
                  val photoAspectRatio = nativeWidth.toFloat() / nativeHeight.toFloat()
                  BoxWithConstraints(
                    modifier = Modifier
                      .fillMaxHeight()
                      .aspectRatio(photoAspectRatio)
                      .border(width = 0.5.dp, color = InkBlack)
                  ) {
                    val scaleX = maxWidth.value / nativeWidth
                    val scaleY = maxHeight.value / nativeHeight

                    AsyncImage(
                      model = photoUri,
                      contentDescription = "Captured page",
                      contentScale = ContentScale.FillBounds,
                      modifier = Modifier.fillMaxSize()
                    )

                    ocrLines.forEachIndexed { index, word ->
                      val loc = word.location
                      if (loc != null) {
                        val isSelected = selectedOcrLines.contains(index)
                        val leftDp = loc.left * scaleX
                        val topDp = loc.top * scaleY
                        val widthDp = loc.width * scaleX
                        val heightDp = loc.height * scaleY

                        Box(
                          modifier = Modifier
                            .offset(x = leftDp.dp, y = topDp.dp)
                            .size(width = widthDp.dp, height = heightDp.dp)
                            .background(
                              if (isSelected) CinnabarRed.copy(alpha = 0.25f)
                              else Color.Black.copy(alpha = 0.05f)
                            )
                            .border(
                              width = 1.dp,
                              color = if (isSelected) CinnabarRed else Color.Gray.copy(alpha = 0.5f)
                            )
                            .clickable {
                              selectedOcrLines = if (isSelected) {
                                selectedOcrLines - index
                              } else {
                                selectedOcrLines + index
                              }
                            }
                        )
                      }
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                      Box(modifier = Modifier.align(Alignment.TopStart).size(12.dp).border(width = 2.dp, color = InkBlack.copy(alpha = 0.4f), shape = MaterialTheme.shapes.extraSmall))
                      Box(modifier = Modifier.align(Alignment.TopEnd).size(12.dp).border(width = 2.dp, color = InkBlack.copy(alpha = 0.4f), shape = MaterialTheme.shapes.extraSmall))
                      Box(modifier = Modifier.align(Alignment.BottomStart).size(12.dp).border(width = 2.dp, color = InkBlack.copy(alpha = 0.4f), shape = MaterialTheme.shapes.extraSmall))
                      Box(modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).border(width = 2.dp, color = InkBlack.copy(alpha = 0.4f), shape = MaterialTheme.shapes.extraSmall))
                    }
                  }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(50.dp)
                      .border(width = 0.5.dp, color = InkBlack)
                      .background(Color(0xFFFAF7EE))
                      .clickable(onClick = { currentStep = HighlightStep.CROP }),
                    contentAlignment = Alignment.Center
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Crop, contentDescription = null, tint = InkBlack, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("重新裁剪", fontFamily = FontFamily.Serif, fontSize = 15.sp, color = InkBlack)
                    }
                  }

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(50.dp)
                      .background(CinnabarRed)
                      .border(width = 1.dp, color = InkBlack)
                      .clickable {
                        val joined = ocrLines.indices
                          .filter { selectedOcrLines.contains(it) }
                          .map { ocrLines[it].words }
                          .joinToString("\n")

                        highlightText = joined
                        isManualInput = false
                        currentStep = HighlightStep.CONFIRM
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("確認選擇", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                  }
                }
              }
            }
          }
        }

        HighlightStep.CONFIRM -> {
          Scaffold(
            topBar = {
              CenterAlignedTopAppBar(
                title = {
                  Text(
                    text = "書摘確認",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = InkBlack
                  )
                },
                navigationIcon = {
                  IconButton(onClick = {
                    currentStep = if (isManualInput) HighlightStep.SELECTION else HighlightStep.OCR_SCAN
                  }) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "返回",
                      tint = InkBlack
                    )
                  }
                },
                actions = {
                  TextButton(
                    onClick = {
                      if (highlightText.trim().isEmpty()) {
                        textError = "書摘內容不能爲空"
                        return@TextButton
                      }

                      val token = settingsRepository.getReadwiseToken().trim()
                      if (token.isEmpty()) {
                        Toast.makeText(context, "請先在設置中配置 Readwise API Token", Toast.LENGTH_LONG).show()
                        return@TextButton
                      }

                      isSubmitting = true
                      coroutineScope.launch {
                        val currentIsoTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        val pageNum = pageText.trim().toIntOrNull()
                        val highlight = ReadwiseHighlight(
                          text = highlightText.trim(),
                          title = title,
                          author = author,
                          image_url = imageUrl,
                          source_url = sourceUrl,
                          note = if (noteText.trim().isNotEmpty()) noteText.trim() else null,
                          highlighted_at = currentIsoTime,
                          location = pageNum,
                          location_type = if (pageNum != null) "page" else null,
                          category = "books"
                        )

                        val result = readwiseRepository.createHighlight(token, highlight)
                        isSubmitting = false
                        if (result.isSuccess) {
                          Toast.makeText(context, "書摘同步至 Readwise 成功！", Toast.LENGTH_SHORT).show()
                          onSuccess()
                        } else {
                          val error = result.exceptionOrNull()?.message ?: "未知網絡錯誤"
                          Toast.makeText(context, "同步失敗: $error", Toast.LENGTH_LONG).show()
                        }
                      }
                    },
                    enabled = !isSubmitting
                  ) {
                    Text(
                      text = "確認",
                      fontFamily = FontFamily.Serif,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                      color = if (isSubmitting) CinnabarRed.copy(alpha = 0.5f) else CinnabarRed
                    )
                  }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PaperWhite),
                modifier = Modifier.border(width = 0.5.dp, color = PaperShadow)
              )
            },
            containerColor = PaperWhite
          ) { innerPadding ->
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              // Excerpt metadata card
              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                  .fillMaxWidth()
                  .border(width = 0.5.dp, color = PaperShadow)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .width(60.dp)
                      .height(90.dp)
                      .border(width = 0.5.dp, color = InkBlack)
                      .background(Color.White)
                  ) {
                    if (!imageUrl.isNullOrEmpty()) {
                      AsyncImage(
                        model = imageUrl,
                        contentDescription = "Book cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(16.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = title,
                      fontFamily = FontFamily.Serif,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                      color = InkBlack
                    )
                    if (!author.isNullOrEmpty()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = author,
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = InkBlack.copy(alpha = 0.7f)
                      )
                    }
                    if (!isbn.isNullOrEmpty()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = "ISBN: $isbn",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        color = InkBlack.copy(alpha = 0.5f)
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(24.dp))

              // Form fields
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
              ) {
                // Content Excerpt
                Column {
                  Text(
                    text = "內文 *",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = InkBlack.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                  )

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 140.dp, max = 260.dp)
                      .background(Color.White)
                      .border(width = 1.dp, color = if (textError != null) CinnabarRed else InkBlack.copy(alpha = 0.3f))
                      .padding(12.dp),
                    contentAlignment = Alignment.TopStart
                  ) {
                    BasicTextField(
                      value = highlightText,
                      onValueChange = {
                        highlightText = it
                        if (it.isNotEmpty()) textError = null
                      },
                      textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = InkBlack,
                        lineHeight = 22.sp
                      ),
                      cursorBrush = SolidColor(CinnabarRed),
                      modifier = Modifier.fillMaxWidth(),
                      decorationBox = { innerTextField ->
                        if (highlightText.isEmpty()) {
                          Text(
                            text = "請輸入書摘內容...",
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            color = InkBlack.copy(alpha = 0.3f)
                          )
                        }
                        innerTextField()
                      }
                    )
                  }
                  if (textError != null) {
                    Text(
                      text = textError!!,
                      fontFamily = FontFamily.Serif,
                      fontSize = 12.sp,
                      color = CinnabarRed,
                      modifier = Modifier.padding(top = 4.dp)
                    )
                  }
                }

                // Notes
                Column {
                  Text(
                    text = "筆記 (可選)",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = InkBlack.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                  )

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 100.dp, max = 200.dp)
                      .background(Color.White)
                      .border(width = 1.dp, color = InkBlack.copy(alpha = 0.3f))
                      .padding(12.dp),
                    contentAlignment = Alignment.TopStart
                  ) {
                    BasicTextField(
                      value = noteText,
                      onValueChange = { noteText = it },
                      textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = InkBlack,
                        lineHeight = 22.sp
                      ),
                      cursorBrush = SolidColor(CinnabarRed),
                      modifier = Modifier.fillMaxWidth(),
                      decorationBox = { innerTextField ->
                        if (noteText.isEmpty()) {
                          Text(
                            text = "在此寫下您的反思、感悟或標籤...",
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            color = InkBlack.copy(alpha = 0.3f)
                          )
                        }
                        innerTextField()
                      }
                    )
                  }
                }

                // Page Number
                Column(modifier = Modifier.width(150.dp)) {
                  Text(
                    text = "頁碼 (可選)",
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
                      value = pageText,
                      onValueChange = { pageText = it },
                      textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = InkBlack,
                        textAlign = TextAlign.Center
                      ),
                      cursorBrush = SolidColor(CinnabarRed),
                      modifier = Modifier.fillMaxWidth(),
                      decorationBox = { innerTextField ->
                        if (pageText.isEmpty()) {
                          Text(
                            text = "---",
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            color = InkBlack.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                          )
                        }
                        innerTextField()
                      }
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(32.dp))

              // Submit Button
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .background(if (isSubmitting) CinnabarRed.copy(alpha = 0.5f) else CinnabarRed)
                  .border(width = 1.dp, color = InkBlack)
                  .clickable(enabled = !isSubmitting) {
                    if (highlightText.trim().isEmpty()) {
                      textError = "內文不能爲空"
                      return@clickable
                    }

                    val token = settingsRepository.getReadwiseToken().trim()
                    if (token.isEmpty()) {
                      Toast.makeText(context, "請先在設置中配置 Readwise API Token", Toast.LENGTH_LONG).show()
                      return@clickable
                    }

                    isSubmitting = true
                    coroutineScope.launch {
                      val currentIsoTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                      val pageNum = pageText.trim().toIntOrNull()
                      val highlight = ReadwiseHighlight(
                        text = highlightText.trim(),
                        title = title,
                        author = author,
                        image_url = imageUrl,
                        source_url = sourceUrl,
                        note = if (noteText.trim().isNotEmpty()) noteText.trim() else null,
                        highlighted_at = currentIsoTime,
                        location = pageNum,
                        location_type = if (pageNum != null) "page" else null,
                        category = "books"
                      )

                      val result = readwiseRepository.createHighlight(token, highlight)
                      isSubmitting = false
                      if (result.isSuccess) {
                        Toast.makeText(context, "書摘同步至 Readwise 成功！", Toast.LENGTH_SHORT).show()
                        onSuccess()
                      } else {
                        val error = result.exceptionOrNull()?.message ?: "未知網絡錯誤"
                        Toast.makeText(context, "同步失敗: $error", Toast.LENGTH_LONG).show()
                      }
                    }
                  },
                contentAlignment = Alignment.Center
              ) {
                if (isSubmitting) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                      color = Color.White,
                      modifier = Modifier.size(18.dp),
                      strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                      text = "正在同步至 Readwise...",
                      fontFamily = FontFamily.Serif,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                      color = Color.White
                    )
                  }
                } else {
                  Text(
                    text = "確認並同步",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                  )
                }
              }
            }
          }
        }
      }
    }

    // Step 1 Selection bottom sheet styled overlay
    if (currentStep == HighlightStep.SELECTION) {
      // Scrim background
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.4f))
          .clickable { onBack() }
      )

      // Bottom sheet container card
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7EE)),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .border(width = 0.5.dp, color = PaperShadow)
          .clickable(enabled = false) {} // Consume clicks to prevent dismissing
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Bottom Sheet Drag handle hint
          Box(
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .width(40.dp)
              .height(4.dp)
              .background(InkBlack.copy(alpha = 0.2f), shape = MaterialTheme.shapes.extraSmall)
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Bottom sheet header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "選取錄入方式",
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = CinnabarRed,
              letterSpacing = 1.sp
            )
            IconButton(onClick = onBack) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "關閉",
                tint = InkBlack.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Options List
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Option 1: OCR Scan & Extract
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .border(width = 0.5.dp, color = InkBlack.copy(alpha = 0.3f))
                .background(Color.White)
                .clickable {
                  val apiKey = settingsRepository.getBaiduApiKey().trim()
                  val secretKey = settingsRepository.getBaiduSecretKey().trim()
                  if (apiKey.isEmpty() || secretKey.isEmpty()) {
                    Toast.makeText(context, "請先在設置中配置百度 OCR API 密鑰", Toast.LENGTH_LONG).show()
                  } else {
                    triggerCamera()
                  }
                }
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .border(width = 0.5.dp, color = InkBlack.copy(alpha = 0.2f))
                  .background(Color(0xFFFAF7EE)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.QrCodeScanner,
                  contentDescription = null,
                  tint = CinnabarRed,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "識別",
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = InkBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Scan & Extract",
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 11.sp,
                  color = InkBlack.copy(alpha = 0.5f)
                )
              }
            }

            // Option 2: Manual Input
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .border(width = 0.5.dp, color = InkBlack.copy(alpha = 0.3f))
                .background(Color.White)
                .clickable {
                  highlightText = ""
                  isManualInput = true
                  currentStep = HighlightStep.CONFIRM
                }
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .border(width = 0.5.dp, color = InkBlack.copy(alpha = 0.2f))
                  .background(Color(0xFFFAF7EE)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.EditNote,
                  contentDescription = null,
                  tint = CinnabarRed,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "手動填寫",
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = InkBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Manual Input",
                  fontFamily = FontFamily.SansSerif,
                  fontSize = 11.sp,
                  color = InkBlack.copy(alpha = 0.5f)
                )
              }
            }
          }
          
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}
