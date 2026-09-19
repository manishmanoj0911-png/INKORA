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

private val DarkColorScheme = darkColorScheme(
    primary = InkoraPrimaryDark,
    onPrimary = InkoraOnPrimaryDark,
    primaryContainer = InkoraPrimaryContainerDark,
    onPrimaryContainer = InkoraOnPrimaryContainerDark,
    secondary = InkoraSecondaryDark,
    onSecondary = InkoraOnSecondaryDark,
    secondaryContainer = InkoraSecondaryContainerDark,
    onSecondaryContainer = InkoraOnSecondaryContainerDark,
    tertiary = InkoraTertiaryDark,
    onTertiary = InkoraOnTertiaryDark,
    tertiaryContainer = InkoraTertiaryContainerDark,
    onTertiaryContainer = InkoraOnTertiaryContainerDark,
    background = InkoraBackgroundDark,
    onBackground = InkoraOnBackgroundDark,
    surface = InkoraSurfaceDark,
    onSurface = InkoraOnSurfaceDark,
    surfaceVariant = InkoraSurfaceVariantDark,
    onSurfaceVariant = InkoraOnSurfaceVariantDark,
    outline = InkoraOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = InkoraPrimaryLight,
    onPrimary = InkoraOnPrimaryLight,
    primaryContainer = InkoraPrimaryContainerLight,
    onPrimaryContainer = InkoraOnPrimaryContainerLight,
    secondary = InkoraSecondaryLight,
    onSecondary = InkoraOnSecondaryLight,
    secondaryContainer = InkoraSecondaryContainerLight,
    onSecondaryContainer = InkoraOnSecondaryContainerLight,
    tertiary = InkoraTertiaryLight,
    onTertiary = InkoraOnTertiaryLight,
    tertiaryContainer = InkoraTertiaryContainerLight,
    onTertiaryContainer = InkoraOnTertiaryContainerLight,
    background = InkoraBackgroundLight,
    onBackground = InkoraOnBackgroundLight,
    surface = InkoraSurfaceLight,
    onSurface = InkoraOnSurfaceLight,
    surfaceVariant = InkoraSurfaceVariantLight,
    onSurfaceVariant = InkoraOnSurfaceVariantLight,
    outline = InkoraOutlineLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
