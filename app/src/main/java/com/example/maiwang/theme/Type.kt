package com.example.maiwang.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// We map Noto Serif / Newsreader to FontFamily.Serif for that classic, woodblock-print feel,
// and Work Sans to FontFamily.SansSerif.
val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Bold,
      fontSize = 48.sp,
      lineHeight = 57.sp,
      letterSpacing = (-0.02).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Medium,
      fontSize = 24.sp,
      lineHeight = 33.sp,
      letterSpacing = 0.02.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Normal,
      fontSize = 20.sp,
      lineHeight = 36.sp, // line height 1.8
      letterSpacing = 0.01.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 25.sp, // line height 1.6
      letterSpacing = 0.01.sp
    ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 12.sp,
      letterSpacing = 0.1.sp
    )
  )

// Custom quote style
val QuoteTextStyle = TextStyle(
  fontFamily = FontFamily.Serif,
  fontWeight = FontWeight.Normal,
  fontSize = 22.sp,
  lineHeight = 39.sp, // line height 1.8
  letterSpacing = 0.02.sp
)



