package com.example.maiwang.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = PaperWhite,
  onPrimary = InkBlack,
  secondary = CeladonGreen,
  onSecondary = InkBlack,
  tertiary = CinnabarRed,
  onTertiary = PaperWhite,
  background = InkBlack,
  onBackground = PaperWhite,
  surface = InkBlack,
  onSurface = PaperWhite,
  outline = PaperShadow,
  surfaceVariant = PaperShadow,
  onSurfaceVariant = PaperWhite
)

private val LightColorScheme = lightColorScheme(
  primary = InkBlack,
  onPrimary = PaperWhite,
  secondary = CeladonGreen,
  onSecondary = InkBlack,
  tertiary = CinnabarRed,
  onTertiary = PaperWhite,
  background = PaperWhite,
  onBackground = InkBlack,
  surface = PaperWhite,
  onSurface = InkBlack,
  outline = PaperShadow,
  surfaceVariant = PaperShadow,
  onSurfaceVariant = InkBlack
)

// Sharp corners for all components as per DESIGN.md
private val RectilinearShapes = Shapes(
  small = RoundedCornerShape(0.dp),
  medium = RoundedCornerShape(0.dp),
  large = RoundedCornerShape(0.dp)
)

@Composable
fun Theme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the Xuan Paper brand palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = RectilinearShapes,
    content = content
  )
}



