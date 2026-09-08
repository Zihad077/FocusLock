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

    // Glow and neon accent colors
    val NeonCyan = Color(0xFF00E5FF)
    val NeonBlue = Color(0xFF2979FF)
    val NeonPurple = Color(0xFF7C4DFF)
    val NeonTeal = Color(0xFF00BFA5)
    val NeonAmber = Color(0xFFFFAB00)
    val NeonCoral = Color(0xFFFF5252)

    @Composable
    fun cardColor(isElevated: Boolean = false): Color {
        val isDark = isSystemInDarkTheme()
        return when {
            isDark && isElevated -> GlassSurfaceElevatedDark
            isDark -> GlassSurfaceDark
            isElevated -> GlassSurfaceElevatedLight
            else -> GlassSurfaceLight
        }
    }

    @Composable
    fun borderColor(isHighlight: Boolean = false): Color {
        val isDark = isSystemInDarkTheme()
        return when {
            isDark && isHighlight -> GlassBorderDark
            isDark -> GlassBorderSubtleDark
            isHighlight -> GlassBorderLight
            else -> GlassBorderSubtleLight
        }
    }

    @Composable
    fun borderBrush(isHighlight: Boolean = false): Brush {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Brush.linearGradient(
                colors = if (isHighlight) {
                    listOf(
                        Color(0x9080D8FF),
                        Color(0x3000E5FF),
                        Color(0x15FFFFFF),
                        Color(0x6000E5FF)
                    )
                } else {
                    listOf(
                        Color(0x55FFFFFF),
                        Color(0x1580D8FF),
                        Color(0x05FFFFFF),
                        Color(0x30FFFFFF)
                    )
                },
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = if (isHighlight) {
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0x6080D8FF),
                        Color(0x40FFFFFF),
                        Color(0x90FFFFFF)
                    )
                } else {
                    listOf(
                        Color(0xAAFFFFFF),
                        Color(0x30B0D0E8),
                        Color(0x20FFFFFF),
                        Color(0x60FFFFFF)
                    )
                },
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
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
    borderWidth: Dp = 1.dp
): Modifier = this
    .shadow(
        elevation = if (isElevated) 12.dp else 4.dp,
        shape = shape,
        ambientColor = Color(0x40001025),
        spotColor = Color(0x3000E5FF)
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                (if (isElevated) Color(0x35406085) else Color(0x22304565)).copy(alpha = (if (isElevated) 0.35f else 0.22f) * alphaMultiplier),
                (if (isElevated) Color(0x2015253A) else Color(0x18101E30)).copy(alpha = (if (isElevated) 0.22f else 0.16f) * alphaMultiplier)
            )
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isHighlight) 0.55f else 0.35f),
                Color(0xFF00E5FF).copy(alpha = if (isHighlight) 0.40f else 0.15f),
                Color.White.copy(alpha = 0.08f),
                Color(0xFF2979FF).copy(alpha = if (isHighlight) 0.30f else 0.12f)
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
    val isDark = isSystemInDarkTheme()

    // Gentle ambient pulse for liquid background depth
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_ambient")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF070B12), // Deepest twilight midnight
                            Color(0xFF0C1524),
                            Color(0xFF0E1A2C),
                            Color(0xFF09111E)
                        )
                    } else {
                        listOf(
                            Color(0xFFEAF2F8), // Soft glacial sky
                            Color(0xFFDDEAF5),
                            Color(0xFFE4EDF7),
                            Color(0xFFF1F6FA)
                        )
                    }
                )
            )
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Orb 1: Top-Right Cyan/Blue soft glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0x3500E5FF),
                                Color(0x182979FF),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x4040A5FF),
                                Color(0x1564B5F6),
                                Color.Transparent
                            )
                        },
                        center = Offset(canvasWidth * 0.85f, canvasHeight * 0.15f),
                        radius = canvasWidth * 0.65f * ambientPulse
                    )
                )

                // Orb 2: Center-Left Deep Violet/Indigo ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0x257C4DFF),
                                Color(0x103D5AFE),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x25B388FF),
                                Color(0x087C4DFF),
                                Color.Transparent
                            )
                        },
                        center = Offset(canvasWidth * 0.15f, canvasHeight * 0.45f),
                        radius = canvasWidth * 0.7f
                    )
                )

                // Orb 3: Bottom-Center Subtle Turquoise/Aqua depth glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0x2000BFA5),
                                Color(0x0A00E5FF),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x2580CBC4),
                                Color.Transparent
                            )
                        },
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.9f),
                        radius = canvasWidth * 0.8f
                    )
                )
            }
    ) {
        content()
    }
}
