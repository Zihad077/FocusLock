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
        val isDark = isSystemInDarkTheme()
        return when {
            isDark && isElevated -> SleekSurfaceVariantDark.copy(alpha = 0.5f)
            isDark -> SleekSurfaceDark.copy(alpha = 0.4f)
            isElevated -> SleekSurfaceVariantLight.copy(alpha = 0.9f)
            else -> SleekSurfaceLight.copy(alpha = 0.7f)
        }
    }

    @Composable
    fun borderColor(isHighlight: Boolean = false): Color {
        val isDark = isSystemInDarkTheme()
        return when {
            isDark && isHighlight -> SleekPrimaryDark.copy(alpha = 0.4f)
            isDark -> SleekOutlineDark.copy(alpha = 0.2f)
            isHighlight -> SleekPrimaryLight.copy(alpha = 0.6f)
            else -> SleekOutlineLight.copy(alpha = 0.3f)
        }
    }

    @Composable
    fun borderBrush(isHighlight: Boolean = false): Brush {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Brush.linearGradient(
                colors = if (isHighlight) {
                    listOf(
                        SleekPrimaryDark.copy(alpha = 0.9f),
                        SleekPrimaryDark.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.15f),
                        SleekPrimaryDark.copy(alpha = 0.6f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        SleekOutlineDark.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.30f)
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
        spotColor = Color(0x200D6EFD) // SleekPrimaryLight but very faint
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
                Color(0xFF4D9BFF).copy(alpha = if (isHighlight) 0.40f else 0.15f), // SleekPrimaryDark
                Color.White.copy(alpha = 0.08f),
                Color(0xFF0D6EFD).copy(alpha = if (isHighlight) 0.30f else 0.12f)  // SleekPrimaryLight
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
    val context = LocalContext.current
    
    // Check constraints for animations
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isPowerSaveMode = powerManager?.isPowerSaveMode == true
    val animatorDurationScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val animationsDisabled = isPowerSaveMode || animatorDurationScale == 0f

    // Gentle ambient pulse for liquid background depth
    val ambientPulse = if (animationsDisabled) {
        1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "liquid_ambient")
        val animatedPulse by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = EaseInOutSine),
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
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF0F141A), // Deep elegant navy-black
                            Color(0xFF131A24),
                            Color(0xFF161D26),
                            Color(0xFF0F141A)
                        )
                    } else {
                        listOf(
                            Color(0xFFF4F7FA), // Soft blue-grey
                            Color(0xFFEEF2F6),
                            Color(0xFFEBF0F5),
                            Color(0xFFF4F7FA)
                        )
                    }
                )
            )
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Orb 1: Top-Right premium blue soft glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0x254D9BFF), // SleekPrimaryDark soft
                                Color(0x103366CC),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x150D6EFD), // SleekPrimaryLight soft
                                Color(0x0A004B99),
                                Color.Transparent
                            )
                        },
                        center = Offset(canvasWidth * 0.85f, canvasHeight * 0.15f),
                        radius = canvasWidth * 0.70f * ambientPulse
                    )
                )

                // Orb 2: Center-Left teal/success ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0x1547C28C), // SleekSecondaryDark soft
                                Color(0x082F805C),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x10198754), // SleekSecondaryLight soft
                                Color(0x05105C39),
                                Color.Transparent
                            )
                        },
                        center = Offset(canvasWidth * 0.15f, canvasHeight * 0.45f),
                        radius = canvasWidth * 0.75f * (2f - ambientPulse) // Opposite pulse
                    )
                )

                // Orb 3: Bottom-Center deep blue glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0x203366CC),
                                Color(0x0A1A3366),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x10004B99),
                                Color.Transparent
                            )
                        },
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.9f),
                        radius = canvasWidth * 0.8f * ambientPulse
                    )
                )
            }
    ) {
        content()
    }
}
