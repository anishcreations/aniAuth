package com.aniauth.authenticator.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Raw Accent Colors (Shared)
val PurpleAccent = Color(0xFF8B5CF6)
val IndigoAccent = Color(0xFF4F46E5)
val EmeraldGreen = Color(0xFF10B981)

// Raw Dark Theme Colors
val RawDarkBg = Color(0xFF07050A)
val RawDarkCard = Color(0xFF120E1A)
val RawTextPrimary = Color(0xFFF3F4F6)
val RawTextSecondary = Color(0xFF9CA3AF)
val RawBorderColor = Color(0xFF2D1F42)

// Raw Light Theme Colors
val RawLightBg = Color(0xFFF8FAFC)
val RawLightCard = Color(0xFFFFFFFF)
val RawTextPrimaryLight = Color(0xFF0F172A)
val RawTextSecondaryLight = Color(0xFF64748B)
val RawBorderColorLight = Color(0xFFE2E8F0)

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
    get() = if (MaterialTheme.colorScheme.background == RawLightBg) PurpleAccent else Color(0xFFA78BFA)


