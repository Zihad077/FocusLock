package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF04151F),
    primaryContainer = Color(0xFF0B3548),
    onPrimaryContainer = Color(0xFFB8F2FF),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF03281C),
    secondaryContainer = Color(0xFF084230),
    onSecondaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF070C15),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF101B2B), // Rich dark navy-charcoal surface for dialogs/cards
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF162438), // Subtle elevated navy surface variant
    onSurfaceVariant = Color(0xFFA8B8CC),
    outline = Color(0x38FFFFFF),
    error = SleekError,
    onError = SleekOnError
)

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

