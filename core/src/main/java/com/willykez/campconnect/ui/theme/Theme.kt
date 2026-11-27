package com.willykez.campconnect.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun darkScheme(p: AppPalette) = darkColorScheme(
  primary = p.primaryOnDark,
  onPrimary = p.onPrimaryOnDark,
  primaryContainer = p.containerDark,
  onPrimaryContainer = p.onContainerDark,
  secondary = WarmAmber,
  onSecondary = Color(0xFF452B00),
  secondaryContainer = Color(0xFF633F00),
  onSecondaryContainer = WarmAmberLight,
  tertiary = SuccessGreen,
  background = BackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceDark,
  surfaceContainerLowest = Color(0xFF090C12),
  surfaceContainerLow = Color(0xFF131722),
  surfaceContainer = Color(0xFF1E2536),
  surfaceContainerHigh = Color(0xFF252D40),
  surfaceContainerHighest = Color(0xFF30394D),
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = OutlineDark,
  outlineVariant = Color(0xFF39445A),
  error = DangerRed
)

private fun lightScheme(p: AppPalette) = lightColorScheme(
  primary = p.accentLight,
  onPrimary = Color.White,
  primaryContainer = p.containerLight,
  onPrimaryContainer = p.onContainerLight,
  secondary = WarmAmber,
  onSecondary = Color.White,
  secondaryContainer = WarmAmberLight,
  onSecondaryContainer = WarmAmberDark,
  tertiary = SuccessGreen,
  background = BackgroundLight,
  onBackground = TextPrimaryLight,
  surface = SurfaceLight,
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF8FAFC),
  surfaceContainer = Color(0xFFF1F5F9),
  surfaceContainerHigh = Color(0xFFE8EEF5),
  surfaceContainerHighest = Color(0xFFDDE5EF),
  onSurface = TextPrimaryLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
  outline = OutlineLight,
  outlineVariant = Color(0xFFD7E0EB),
  error = DangerRed
)

private val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CampusConnectTheme(
  darkTheme: Boolean? = null,
  content: @Composable () -> Unit
) {
  val mode = AppThemeSettings.mode
  val useDarkTheme = darkTheme ?: when (mode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }
  val palette = Brand.palette
  val colorScheme = remember(useDarkTheme, palette) { if (useDarkTheme) darkScheme(palette) else lightScheme(palette) }

  // Lets BrandPrimary pick the right accent for the current theme.
  SideEffect { if (Brand.dark != useDarkTheme) Brand.dark = useDarkTheme }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window ?: return@SideEffect
      WindowCompat.setDecorFitsSystemWindows(window, false)
      @Suppress("DEPRECATION")
      window.statusBarColor = android.graphics.Color.TRANSPARENT
      @Suppress("DEPRECATION")
      window.navigationBarColor = android.graphics.Color.TRANSPARENT
      if (Build.VERSION.SDK_INT >= 29) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
      }
      val controller = WindowCompat.getInsetsController(window, view)
      controller.isAppearanceLightStatusBars = !useDarkTheme
      controller.isAppearanceLightNavigationBars = !useDarkTheme
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content
  )
}
