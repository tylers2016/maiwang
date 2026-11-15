package com.example.maiwang

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.example.maiwang.theme.InkBlack
import com.example.maiwang.theme.PaperWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onTimeout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val svgImageLoader = remember {
    ImageLoader.Builder(context)
      .components {
        add(SvgDecoder.Factory())
      }
      .build()
  }

  // Animation states
  val alpha = remember { Animatable(0f) }
  val scale = remember { Animatable(0.95f) }

  LaunchedEffect(Unit) {
    // Fade in
    alpha.animateTo(1f, animationSpec = tween(400))
    scale.animateTo(1.0f, animationSpec = tween(400))
    // Wait for splash duration
    delay(500)
    // Fade out
    alpha.animateTo(0f, animationSpec = tween(300))
    onTimeout()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(PaperWhite),
    contentAlignment = Alignment.Center
  ) {
    // Background watermark SVG
    AsyncImage(
      model = "file:///android_asset/background.svg",
      contentDescription = null,
      imageLoader = svgImageLoader,
      modifier = Modifier
        .fillMaxSize()
        .alpha(alpha.value * 0.08f),
      contentScale = ContentScale.FillBounds
    )
  }
}
