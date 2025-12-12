package com.bingoroyale.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BingoColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = BackgroundPrimary,
    primaryContainer = GoldDark,
    onPrimaryContainer = TextPrimary,

    secondary = RedPrimary,
    onSecondary = TextPrimary,
    secondaryContainer = RedDark,
    onSecondaryContainer = TextPrimary,

    tertiary = OrangePrimary,
    onTertiary = TextPrimary,

    background = BackgroundPrimary,
    onBackground = TextPrimary,

    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = TextPrimary,

    outline = GlassBorder
)

@Composable
fun BingoRoyaleTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = BingoColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundPrimary.toArgb()
            window.navigationBarColor = BackgroundPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}