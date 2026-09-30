package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SleekPrimaryDark,
    onPrimary = SleekOnPrimaryDark,
    primaryContainer = SleekPrimaryContainerDark,
    onPrimaryContainer = SleekOnPrimaryContainerDark,
    secondary = SleekSecondaryDark,
    onSecondary = SleekOnSecondaryDark,
    secondaryContainer = SleekSecondaryContainerDark,
    onSecondaryContainer = SleekOnSecondaryContainerDark,
    tertiary = SleekPremium,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2B1846),
    onTertiaryContainer = Color(0xFFE9D5FF),
    background = SleekBackgroundDark,
    onBackground = SleekOnBackgroundDark,
    surface = Color(0xFF101B2B),
    onSurface = SleekOnSurfaceDark,
    surfaceVariant = SleekSurfaceVariantDark,
    onSurfaceVariant = SleekOnSurfaceVariantDark,
    outline = Color(0x38FFFFFF),
    error = SleekError,
    onError = SleekOnError,
    errorContainer = Color(0xFF381721),
    onErrorContainer = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimaryLight,
    onPrimary = SleekOnPrimaryLight,
    primaryContainer = SleekPrimaryContainerLight,
    onPrimaryContainer = SleekOnPrimaryContainerLight,
    secondary = SleekSecondaryLight,
    onSecondary = SleekOnSecondaryLight,
    secondaryContainer = SleekSecondaryContainerLight,
    onSecondaryContainer = SleekOnSecondaryContainerLight,
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = SleekBackgroundLight,
    onBackground = SleekOnBackgroundLight,
    surface = SleekSurfaceLight,
    onSurface = SleekOnSurfaceLight,
    surfaceVariant = SleekSurfaceVariantLight,
    onSurfaceVariant = SleekOnSurfaceVariantLight,
    outline = SleekOutlineLight,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

object FocusLockSemantics {
    val colors: SemanticColorSystem
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current
}

val FocusLockSemanticColors: SemanticColorSystem
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
