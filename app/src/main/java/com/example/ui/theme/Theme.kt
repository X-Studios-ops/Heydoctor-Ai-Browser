package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedDarkPrimary,
    onPrimary = SophisticatedDarkOnPrimary,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = SophisticatedDarkBg,
    surface = SophisticatedDarkSurface,
    onBackground = SophisticatedDarkText,
    onSurface = SophisticatedDarkText,
    surfaceVariant = SophisticatedDarkContainer,
    outline = SophisticatedDarkBorder
  )

private val LightColorScheme = DarkColorScheme // Always premium Sophisticated Dark as requested

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for the Sophisticated Dark look
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve exact theme aesthetics
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
