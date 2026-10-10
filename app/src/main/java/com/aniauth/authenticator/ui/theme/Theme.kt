package com.aniauth.authenticator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RawDarkAccent,
    secondary = RawDarkSuccess,
    background = RawDarkBg,
    surface = RawDarkCard,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF2E204C),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF493064),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE9DDFC),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF30243F),
    outlineVariant = RawBorderColor,
    onBackground = RawTextPrimary,
    onSurface = RawTextPrimary,
    onSurfaceVariant = RawTextSecondary,
    outline = RawBorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = RawLightAccent,
    secondary = RawLightSuccess,
    background = RawLightBg,
    surface = RawLightCard,
    onPrimary = RawLightCard,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFEADCFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF392951),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0EDF4),
    outlineVariant = RawBorderColorLight,
    onBackground = RawTextPrimaryLight,
    onSurface = RawTextPrimaryLight,
    onSurfaceVariant = RawTextSecondaryLight,
    outline = RawBorderColorLight
)

@Composable
fun AniAuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}
