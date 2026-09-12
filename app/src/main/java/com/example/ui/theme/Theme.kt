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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF001F2B),
    primaryContainer = Color(0xFF004D6B),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = Color(0xFF47C28C),
    onSecondary = Color(0xFF00301B),
    secondaryContainer = Color(0xFF005230),
    onSecondaryContainer = Color(0xFFC7F0DE),
    background = Color(0xFF080D16),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0x22132238), // Translucent liquid glass surface, prevents black square boxes
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0x281B2E48), // Translucent glass variant
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0x3580D8FF),
    error = SleekError,
    onError = SleekOnError
)

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = true, // Permanently Dark / Liquid Glass UI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Light mode is completely removed per design system specifications
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
