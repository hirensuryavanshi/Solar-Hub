package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = SolarNavyDark,
  onPrimary = Color.White,
  primaryContainer = SolarNavyMid,
  onPrimaryContainer = Color.White,
  secondary = SolarBlue,
  onSecondary = Color.White,
  secondaryContainer = SolarBlueLight,
  onSecondaryContainer = SolarBlueDark,
  tertiary = SolarGold,
  onTertiary = Color.White,
  tertiaryContainer = SolarGoldLight,
  onTertiaryContainer = SolarNavyDark,
  background = BackgroundLightGrey,
  onBackground = TextPrimary,
  surface = SurfaceWhite,
  onSurface = TextPrimary,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = Color(0xFF334155),
  outline = Color(0xFF64748B)
)

private val DarkColorScheme = darkColorScheme(
  primary = SolarBlue,
  onPrimary = Color.White,
  primaryContainer = SolarNavyMid,
  onPrimaryContainer = Color.White,
  secondary = SolarGold,
  onSecondary = SolarNavyDark,
  background = Color(0xFF0F172A),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF1E293B),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF475569)
)

@Composable
fun SolarHubTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
