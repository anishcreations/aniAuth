package com.aniauth.authenticator.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Purple remains the brand accent; each theme supplies a readable foreground.
val RawDarkAccent = Color(0xFFBA9CFF)
val RawLightAccent = Color(0xFF703BD1)
val RawDarkSuccess = Color(0xFF9CCDB7)
val RawLightSuccess = Color(0xFF326C53)

// Raw Dark Theme Colors
val RawDarkBg = Color(0xFF100D17)
val RawDarkCard = Color(0xFF20192C)
val RawTextPrimary = Color(0xFFECEAF0)
val RawTextSecondary = Color(0xFFB7B2C0)
val RawBorderColor = Color(0xFF49454F)

// Raw Light Theme Colors
val RawLightBg = Color(0xFFF8F7FA)
val RawLightCard = Color(0xFFFFFFFF)
val RawTextPrimaryLight = Color(0xFF25232B)
val RawTextSecondaryLight = Color(0xFF65606E)
val RawBorderColorLight = Color(0xFFDDD9E3)

val PurpleAccent: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val EmeraldGreen: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondary

// Dynamic Color accessors for screens
val DarkBg: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val DarkCard: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val TextPrimary: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val BorderColor: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val SoftFooterColor: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant


