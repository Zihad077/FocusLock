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
    surface = Color(0x24FFFFFF), // Translucent frosted liquid glass surface
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0x1AFFFFFF), // Translucent frosted glass variant
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0x45FFFFFF),
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
