package com.airkey.wifiqr.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Color Palette - Cyber/Neon Dark
val NeonPurple = Color(0xFF6C63FF)
val NeonCyan = Color(0xFF00F5FF)
val NeonPink = Color(0xFFFF006E)
val DeepBlack = Color(0xFF0A0A1A)
val DarkSurface = Color(0xFF12121F)
val CardSurface = Color(0xFF1A1A2E)
val CardSurface2 = Color(0xFF16213E)
val GlassWhite = Color(0x1AFFFFFF)
val GlassWhite2 = Color(0x33FFFFFF)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB3FFFFFF)
val TextMuted = Color(0x66FFFFFF)
val GreenSuccess = Color(0xFF00E676)
val OrangeWarn = Color(0xFFFFAB40)
val RedError = Color(0xFFFF5252)

val GradientStart = NeonPurple
val GradientEnd = NeonCyan

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D35CC),
    onPrimaryContainer = Color(0xFFE0DEFF),
    secondary = NeonCyan,
    onSecondary = DeepBlack,
    secondaryContainer = Color(0xFF005F6B),
    onSecondaryContainer = Color(0xFFB2EBEE),
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF44446A),
    error = RedError,
    onError = Color.White,
)

@Composable
fun AirKeyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AirKeyTypography,
        content = content
    )
}
