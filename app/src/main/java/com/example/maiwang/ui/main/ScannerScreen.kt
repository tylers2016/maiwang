package com.example.maiwang.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.maiwang.theme.CinnabarRed
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperWhite
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.FocusMeteringAction
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
  onBack: () -> Unit,
  onIsbnScanned: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
      hasCameraPermission = granted
    }
  )

  LaunchedEffect(key1 = true) {
    if (!hasCameraPermission) {
      launcher.launch(Manifest.permission.CAMERA)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    if (hasCameraPermission) {
      CameraScannerView(
        onIsbnScanned = onIsbnScanned,
        onBack = onBack
      )
    } else {
      // Permission Denied View
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(PaperWhite)
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "相機權限受限",
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          color = CinnabarRed,
          modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
          text = "掃描 ISBN 需要使用相機權限。請在系統設置中授予權限，或者返回主頁手動輸入。",
          fontFamily = FontFamily.Serif,
          fontSize = 16.sp,
          color = InkBlack,
          textAlign = TextAlign.Center,
          lineHeight = 28.sp,
          modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
          onClick = { launcher.launch(Manifest.permission.CAMERA) },
          colors = ButtonDefaults.buttonColors(containerColor = CinnabarRed),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(width = 1.dp, color = InkBlack)
        ) {
          Text("請求相機權限", fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onBack,
          colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(width = 0.5.dp, color = InkBlack)
        ) {
          Text("返回主頁", fontFamily = FontFamily.Serif, fontSize = 16.sp, color = InkBlack)
        }
      }
    }
  }
}

@Composable
fun CameraScannerView(
  onIsbnScanned: (String) -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var camera by remember { mutableStateOf<Camera?>(null) }
  var isTorchOn by remember { mutableStateOf(false) }
  var hasScanned by remember { mutableStateOf(false) }

  // Set up camera preview with touch to focus
  val previewView = remember {
    PreviewView(context).apply {
      setOnTouchListener { view, event ->
        if (event.action == android.view.MotionEvent.ACTION_UP) {
          val factory = meteringPointFactory
          val point = factory.createPoint(event.x, event.y)
          val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
          camera?.cameraControl?.startFocusAndMetering(action)
          view.performClick()
        }
        true
      }
    }
  }
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
  val executor = remember { Executors.newSingleThreadExecutor() }

  // Barcode scanner client with custom formats and zoom suggestions
  val scanner = remember {
    val zoomCallback = ZoomSuggestionOptions.ZoomCallback { zoomRatio ->
      camera?.cameraControl?.setZoomRatio(zoomRatio)
      true
    }
    val options = BarcodeScannerOptions.Builder()
      .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A)
      .setZoomSuggestionOptions(
        ZoomSuggestionOptions.Builder(zoomCallback).build()
      )
      .build()
    BarcodeScanning.getClient(options)
  }

  // Scanning animation
  val infiniteTransition = rememberInfiniteTransition(label = "scanning")
  val scanLineYVal by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "linePosition"
  )

  LaunchedEffect(cameraProviderFuture) {
    cameraProviderFuture.addListener({
      val cameraProvider = cameraProviderFuture.get()

      // Explicitly request 1280x720 (720p) resolution for barcode clarity
      val resolutionSelector = ResolutionSelector.Builder()
        .setResolutionStrategy(
          ResolutionStrategy(
            Size(1280, 720),
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
          )
        )
        .setAspectRatioStrategy(
          AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        )
        .build()

      val preview = Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()
        .also {
          it.setSurfaceProvider(previewView.surfaceProvider)
        }

      val imageAnalysis = ImageAnalysis.Builder()
        .setResolutionSelector(resolutionSelector)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

      imageAnalysis.setAnalyzer(executor) { imageProxy ->
        if (hasScanned) {
          imageProxy.close()
          return@setAnalyzer
        }
        @OptIn(ExperimentalGetImage::class)
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
          val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
          scanner.process(image)
            .addOnSuccessListener { barcodes ->
              if (hasScanned) return@addOnSuccessListener
              for (barcode in barcodes) {
                val value = barcode.rawValue
                if (value != null) {
                  val cleanValue = value.replace("-", "").replace(" ", "").trim()
                  if (cleanValue.length == 13 || cleanValue.length == 10) {
                    // ISBN detected, trigger callback and stop analyzing
                    hasScanned = true
                    onIsbnScanned(cleanValue)
                    onBack()
                    break
                  }
                }
              }
            }
            .addOnCompleteListener {
              imageProxy.close()
            }
        } else {
          imageProxy.close()
        }
      }

      val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

      try {
        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
          lifecycleOwner,
          cameraSelector,
          preview,
          imageAnalysis
        )
      } catch (exc: Exception) {
        // Log or handle binding errors
      }
    }, ContextCompat.getMainExecutor(context))
  }

  DisposableEffect(Unit) {
    onDispose {
      executor.shutdown()
      scanner.close()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Camera Preview
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize()
    )

    // Scanning HUD overlay
    Canvas(modifier = Modifier.fillMaxSize()) {
      val width = size.width
      val height = size.height
      val boxHeight = height / 3f
      val boxWidth = (width - 48.dp.toPx()).coerceAtLeast(100f)
      val left = (width - boxWidth) / 2
      val top = (height - boxHeight) / 2
      val right = left + boxWidth
      val bottom = top + boxHeight

      // Draw transparent cutout using clipPath
      val path = Path().apply {
        addRoundRect(
          RoundRect(
            rect = Rect(left, top, right, bottom),
            cornerRadius = CornerRadius(0f)
          )
        )
      }

      clipPath(path = path, clipOp = ClipOp.Difference) {
        drawRect(
          color = Color.Black.copy(alpha = 0.6f)
        )
      }

      // Draw framing corner lines (White brackets)
      val bracketLen = 24.dp.toPx()
      val strokeW = 3.dp.toPx()

      // Top Left Corner
      drawLine(Color.White, Offset(left, top), Offset(left + bracketLen, top), strokeW)
      drawLine(Color.White, Offset(left, top), Offset(left, top + bracketLen), strokeW)

      // Top Right Corner
      drawLine(Color.White, Offset(right, top), Offset(right - bracketLen, top), strokeW)
      drawLine(Color.White, Offset(right, top), Offset(right, top + bracketLen), strokeW)

      // Bottom Left Corner
      drawLine(Color.White, Offset(left, bottom), Offset(left + bracketLen, bottom), strokeW)
      drawLine(Color.White, Offset(left, bottom), Offset(left, bottom - bracketLen), strokeW)

      // Bottom Right Corner
      drawLine(Color.White, Offset(right, bottom), Offset(right - bracketLen, bottom), strokeW)
      drawLine(Color.White, Offset(right, bottom), Offset(right, bottom - bracketLen), strokeW)

      // Draw scanning laser line
      val scanY = top + (boxHeight * scanLineYVal)
      drawLine(
        color = CinnabarRed,
        start = Offset(left + 8.dp.toPx(), scanY),
        end = Offset(right - 8.dp.toPx(), scanY),
        strokeWidth = 2.dp.toPx()
      )
    }

    // Top Controls Bar (Transparent)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "关闭",
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }

      Text(
        text = "脈 望",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color.White,
        letterSpacing = 4.sp
      )

      IconButton(
        onClick = {
          if (camera?.cameraInfo?.hasFlashUnit() == true) {
            isTorchOn = !isTorchOn
            camera?.cameraControl?.enableTorch(isTorchOn)
          }
        }
      ) {
        Icon(
          imageVector = if (isTorchOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
          contentDescription = "手电筒",
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }
    }

    // Bottom hint
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 48.dp)
    ) {
      Text(
        text = "將書籍條碼置於框內以掃描",
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp,
        color = Color.White.copy(alpha = 0.7f)
      )
    }
  }
}
