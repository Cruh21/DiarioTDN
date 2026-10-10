package com.example.diariotdn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BlueContainerLight,
    onPrimaryContainer = BlueOnContainerLight,

    secondary = BlueSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = BlueSecondaryContainerLight,
    onSecondaryContainer = BlueOnSecondaryContainerLight,

    tertiary = BlueTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = BlueTertiaryContainerLight,
    onTertiaryContainer = Color(0xFF001E2B),

    background = GrayBackgroundLight,
    onBackground = GrayOnBackgroundLight,

    surface = GraySurfaceLight,
    onSurface = GrayOnBackgroundLight,
    surfaceVariant = GraySurfaceVariantLight,
    onSurfaceVariant = GrayOnSurfaceVariantLight,

    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = BlueOnContainerDark,

    secondary = BlueSecondaryDark,
    onSecondary = BlueOnPrimaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,

    tertiary = BlueTertiaryDark,
    onTertiary = Color(0xFF003548),
    tertiaryContainer = BlueTertiaryContainerDark,
    onTertiaryContainer = BlueTertiaryContainerLight,

    background = GrayBackgroundDark,
    onBackground = GrayOnBackgroundDark,

    surface = GraySurfaceDark,
    onSurface = GrayOnBackgroundDark,
    surfaceVariant = GraySurfaceVariantDark,
    onSurfaceVariant = GrayOnSurfaceVariantDark,

    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF475569)
)

@Composable
fun DiarioTDNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Usamos la paleta personalizada de Azul y Gris por defecto
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
