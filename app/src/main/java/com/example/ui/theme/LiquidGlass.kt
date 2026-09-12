package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext

/**
 * Liquid Glass design system utilities for FocusLock.
 * Features translucent layered glass surfaces, luminous borders, ambient specular highlights,
 * and adaptive dark/light backdrop mesh gradients inspired by iOS 26/27 aesthetic.
 */
object LiquidGlass {
    // Glass Surface tints
    val GlassTintDark = Color(0x281B2B48)       // Deep translucent navy/cyan tint
    val GlassSurfaceDark = Color(0x3516243A)    // Elevated card surface
    val GlassSurfaceElevatedDark = Color(0x45203352) // Floating surface
    val GlassBorderDark = Color(0x4080D8FF)     // Luminous specular cyan edge
    val GlassBorderSubtleDark = Color(0x20FFFFFF) // Ambient specular rim

    val GlassTintLight = Color(0x184A78A8)
    val GlassSurfaceLight = Color(0x66FFFFFF)   // Frosted white glass
    val GlassSurfaceElevatedLight = Color(0x88FFFFFF)
    val GlassBorderLight = Color(0x60FFFFFF)    // High gloss white specular rim
    val GlassBorderSubtleLight = Color(0x30386699)

    // Glow and neon accent colors (Removed in favor of Premium Palette)

    @Composable
    fun cardColor(isElevated: Boolean = false): Color {
        return if (isElevated) Color(0x281B2E48) else Color(0x1A142236)
    }

    @Composable
    fun borderColor(isHighlight: Boolean = false): Color {
        return if (isHighlight) Color(0x5000E5FF) else Color(0x25FFFFFF)
    }

    @Composable
    fun borderBrush(isHighlight: Boolean = false): Brush {
        return Brush.linearGradient(
            colors = if (isHighlight) {
                listOf(
                    Color.White.copy(alpha = 0.60f),
                    Color(0xFF00E5FF).copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.10f),
                    Color(0xFF0077D6).copy(alpha = 0.35f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color(0xFF00E5FF).copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.20f)
                )
            },
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )
    }
}

/**
 * Applies a Liquid Glass panel background with subtle gradient tint, luminous specular border,
 * rounded corners, and soft depth shadows.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    isElevated: Boolean = false,
    isHighlight: Boolean = false,
    alphaMultiplier: Float = 1.0f,
    borderWidth: Dp = 0.8.dp
): Modifier = this
    .shadow(
        elevation = if (isElevated) 8.dp else 3.dp,
        shape = shape,
        ambientColor = Color(0x25000C1C),
        spotColor = Color(0x1500E5FF)
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                (if (isElevated) Color(0x2E1E3250) else Color(0x1F16253C)).copy(alpha = alphaMultiplier),
                (if (isElevated) Color(0x20142236) else Color(0x150E1826)).copy(alpha = alphaMultiplier)
            )
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isHighlight) 0.55f else 0.32f),
                Color(0xFF00E5FF).copy(alpha = if (isHighlight) 0.40f else 0.15f),
                Color.White.copy(alpha = 0.06f),
                Color(0xFF0077D6).copy(alpha = if (isHighlight) 0.25f else 0.10f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        ),
        shape = shape
    )

/**
 * Renders an animated or dynamic atmospheric backdrop mesh representing deep space, twilight mountains,
 * and luminous orbs seen in modern Liquid Glass OS experiences.
 */
@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    
    // Check constraints for animations with graceful fallback
    val isPowerSaveMode = try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isPowerSaveMode == true
    } catch (e: Exception) {
        false
    }
    
    val animatorDurationScale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    
    val animationsDisabled = isPowerSaveMode || animatorDurationScale == 0f

    // Gentle ambient pulse for liquid background depth
    val ambientPulse = if (animationsDisabled) {
        1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "liquid_ambient")
        val animatedPulse by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
        animatedPulse
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080D16), // Deep liquid midnight
                        Color(0xFF0C1420),
                        Color(0xFF0E1826),
                        Color(0xFF080D16)
                    )
                )
            )
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Orb 1: Top-Right electric cyan soft glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3500E5FF),
                            Color(0x120077D6),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.82f, canvasHeight * 0.18f),
                        radius = canvasWidth * 0.72f * ambientPulse
                    )
                )

                // Orb 2: Center-Left deep blue/violet ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x282979FF),
                            Color(0x0E1A237E),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.15f, canvasHeight * 0.48f),
                        radius = canvasWidth * 0.76f * (2f - ambientPulse)
                    )
                )

                // Orb 3: Bottom-Center teal glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2200B4D8),
                            Color(0x0A003566),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.50f, canvasHeight * 0.88f),
                        radius = canvasWidth * 0.80f * ambientPulse
                    )
                )
            }
    ) {
        content()
    }
}
