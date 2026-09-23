package com.example.ui.theme

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Liquid Glass Design System for FocusLock.
 *
 * Provides:
 * 1. Deep translucent optical glass (zero flat black/solid cards)
 * 2. Edge refraction (dual-tone Fresnel specular borders)
 * 3. Directional specular highlights & inner bevel luminescence
 * 4. Layered optical depth (shadow + glass body + specular reflection + content)
 * 5. Interactive tactile compression & specular brightening on press
 * 6. Dynamic atmospheric background with smooth undulating luminous orbs
 */
object LiquidGlass {
    // Glass Surface Tints (Translucent optical shades, never opaque)
    val GlassTintDark = Color(0x24182E4B)          // Deep translucent sapphire-cyan tint
    val GlassSurfaceDark = Color(0x2E142338)       // Base card surface
    val GlassSurfaceElevatedDark = Color(0x3E1C3352) // Floating card surface
    val GlassHighlightCyan = Color(0xFF00E5FF)     // Specular electric cyan
    val GlassHighlightPurple = Color(0xFF9D4EDD)   // Specular violet accent
    val GlassHighlightBlue = Color(0xFF2979FF)     // Specular azure accent

    // Border and Refraction Rims
    val GlassBorderLuminous = Color(0x6000E5FF)
    val GlassBorderSpecularWhite = Color(0x70FFFFFF)
    val GlassBorderSubtleDark = Color(0x18FFFFFF)

    @Composable
    fun cardColor(isElevated: Boolean = false): Color {
        return if (isElevated) Color(0x321E3250) else Color(0x22132338)
    }

    @Composable
    fun borderColor(isHighlight: Boolean = false): Color {
        return if (isHighlight) Color(0x6000E5FF) else Color(0x2EFFFFFF)
    }

    @Composable
    fun borderBrush(isHighlight: Boolean = false): Brush {
        return if (isHighlight) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color(0xFF00E5FF).copy(alpha = 0.65f),
                    Color(0xFF2979FF).copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.15f)
                ),
                start = Offset(0f, 0f),
                end = Offset(800f, 800f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color(0xFF00E5FF).copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.08f),
                    Color(0xFF0066CC).copy(alpha = 0.20f)
                ),
                start = Offset(0f, 0f),
                end = Offset(800f, 800f)
            )
        }
    }
}

/**
 * Modifier that applies the signature Liquid Glass appearance:
 * - Soft ambient colored floating glow (replaces harsh black shadow)
 * - Deep optical translucent glass substrate
 * - Specular reflection sheen across the upper diagonal plane
 * - Edge refraction dual-tone border
 * - Inner top bevel highlight
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    isElevated: Boolean = false,
    isHighlight: Boolean = false,
    alphaMultiplier: Float = 1.0f,
    borderWidth: Dp = 1.dp
): Modifier = this
    // 1. Soft Floating Ambient Glow (Zero harsh solid black shadow)
    .shadow(
        elevation = if (isElevated) 14.dp else 6.dp,
        shape = shape,
        ambientColor = Color(0x30001025),
        spotColor = if (isHighlight) Color(0x4000E5FF) else Color(0x2000E5FF)
    )
    .clip(shape)
    // 2. Optical Glass Substrate (Deep Translucent, letting background light filter through)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                (if (isElevated) Color(0x381E3452) else Color(0x2A15263C)).copy(alpha = alphaMultiplier),
                (if (isElevated) Color(0x25142438) else Color(0x180E1A29)).copy(alpha = alphaMultiplier)
            )
        )
    )
    // 3. Specular Reflection Sheen & Inner Bevel Highlight (Hardware accelerated draw behind)
    .drawBehind {
        val w = size.width
        val h = size.height

        // Upper-left diagonal glass specular gloss reflection
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isHighlight) 0.16f else 0.08f),
                    Color(0xFF00E5FF).copy(alpha = if (isHighlight) 0.08f else 0.03f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w * 0.7f, h * 0.5f)
            ),
            size = Size(w, h)
        )

        // Subtle top bevel highlight line for physical glass thickness
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (isHighlight) 0.45f else 0.25f),
                    Color(0xFF00E5FF).copy(alpha = if (isHighlight) 0.35f else 0.15f),
                    Color.Transparent
                )
            ),
            start = Offset(w * 0.1f, 1.dp.toPx()),
            end = Offset(w * 0.9f, 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
    // 4. Edge Refraction Dual-Tone Border (Crisp luminous top-left, soft refraction bottom-right)
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = if (isHighlight) {
                listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color(0xFF00E5FF).copy(alpha = 0.60f),
                    Color.White.copy(alpha = 0.12f),
                    Color(0xFF2979FF).copy(alpha = 0.40f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color(0xFF00E5FF).copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.07f),
                    Color(0xFF0066CC).copy(alpha = 0.18f)
                )
            },
            start = Offset(0f, 0f),
            end = Offset(800f, 800f)
        ),
        shape = shape
    )

/**
 * Interactive modifier for Liquid Glass elements:
 * Provides tactile elastic scale compression on press (0.975f) with spring physics
 * and dynamically brightens the specular highlight while touched.
 */
fun Modifier.liquidGlassPressable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = this.composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.975f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glass_press_scale"
    )

    val highlightGlow by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.4f else 1.0f,
        animationSpec = tween(150),
        label = "glass_press_glow"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
        .drawWithContent {
            drawContent()
            if (isPressed && enabled) {
                drawRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.08f)
                )
            }
        }
}

/**
 * Dedicated Liquid Glass Card Composable with integrated layered depth and optional press animation.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isElevated: Boolean = false,
    isHighlight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val baseModifier = modifier.liquidGlass(
        shape = shape,
        isElevated = isElevated,
        isHighlight = isHighlight
    )

    val finalModifier = if (onClick != null) {
        baseModifier.liquidGlassPressable(
            interactionSource = interactionSource,
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier,
        content = content
    )
}

/**
 * Dedicated Liquid Glass Interactive Button with tactile spring compression,
 * luminous gradient background, and crisp specular rim.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val primaryBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF0091EA),
            Color(0xFF2979FF)
        )
    )

    val secondaryBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0x381E3555),
            Color(0x22132338)
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPrimary) (if (isPressed) 6.dp else 12.dp) else 4.dp,
                shape = shape,
                ambientColor = Color(0x30000E20),
                spotColor = if (isPrimary) Color(0x5500E5FF) else Color(0x2000E5FF)
            )
            .clip(shape)
            .background(if (isPrimary) primaryBrush else secondaryBrush)
            .border(
                width = 1.dp,
                brush = if (isPrimary) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.80f),
                            Color(0xFF00E5FF).copy(alpha = 0.60f),
                            Color.White.copy(alpha = 0.20f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.50f),
                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                },
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Background themes available in FocusLock for customization.
 */
/**
 * Animation styles for animated background themes.
 */
enum class ThemeAnimationType {
    SAKURA_PETALS,
    MAPLE_LEAVES,
    CYBER_RAIN,
    AURORA_STARDUST,
    OCEAN_BUBBLES
}

/**
 * Background themes available in FocusLock for customization (All Purely Animated Scenic Themes).
 */
enum class BackgroundThemeType(
    val key: String,
    val titleEn: String,
    val titleBn: String,
    val baseGradient: List<Color>,
    val orb1Colors: List<Color>,
    val orb2Colors: List<Color>,
    val orb3Colors: List<Color>,
    val particleColor: Color,
    val accentColor: Color,
    @DrawableRes val drawableRes: Int,
    val animationType: ThemeAnimationType
) {
    CHERRY_BLOSSOM_NIGHT(
        key = "CHERRY_BLOSSOM_NIGHT",
        titleEn = "Cherry Blossom",
        titleBn = "চেরি ব্লসম",
        baseGradient = listOf(Color(0xFF0A0612), Color(0xFF140A1E), Color(0xFF1F0D29), Color(0xFF0A0612)),
        orb1Colors = listOf(Color(0x55FF69B4), Color(0x22FF1493), Color.Transparent),
        orb2Colors = listOf(Color(0x449D4EDD), Color(0x1C7928CA), Color.Transparent),
        orb3Colors = listOf(Color(0x38FFB7C5), Color(0x14E040FB), Color.Transparent),
        particleColor = Color(0xFFFFB7C5),
        accentColor = Color(0xFFFF69B4),
        drawableRes = R.drawable.img_bg_cherry_blossom,
        animationType = ThemeAnimationType.SAKURA_PETALS
    ),
    SAKURA_TWILIGHT_LAKE(
        key = "SAKURA_TWILIGHT_LAKE",
        titleEn = "Sakura Lake",
        titleBn = "সাকুরা লেক",
        baseGradient = listOf(Color(0xFF070B18), Color(0xFF10162B), Color(0xFF18203D), Color(0xFF070B18)),
        orb1Colors = listOf(Color(0x50FF85A1), Color(0x2000B4D8), Color.Transparent),
        orb2Colors = listOf(Color(0x4000E5FF), Color(0x180077EE), Color.Transparent),
        orb3Colors = listOf(Color(0x38FF69B4), Color(0x129D4EDD), Color.Transparent),
        particleColor = Color(0xFFFFB7C5),
        accentColor = Color(0xFFFF85A1),
        drawableRes = R.drawable.img_bg_sakura_lake,
        animationType = ThemeAnimationType.SAKURA_PETALS
    ),
    AUTUMN_MAPLE_FALL(
        key = "AUTUMN_MAPLE_FALL",
        titleEn = "Autumn Leaves",
        titleBn = "অটাম লিভস",
        baseGradient = listOf(Color(0xFF120806), Color(0xFF1E0E0A), Color(0xFF2E150F), Color(0xFF120806)),
        orb1Colors = listOf(Color(0x55FF6D00), Color(0x22DD2C00), Color.Transparent),
        orb2Colors = listOf(Color(0x44FFAB00), Color(0x1CFF3D00), Color.Transparent),
        orb3Colors = listOf(Color(0x38FF3D00), Color(0x14FF9100), Color.Transparent),
        particleColor = Color(0xFFFFAB40),
        accentColor = Color(0xFFFF6D00),
        drawableRes = R.drawable.img_bg_autumn_maple,
        animationType = ThemeAnimationType.MAPLE_LEAVES
    ),
    CYBERPUNK_NEON_RAIN(
        key = "CYBERPUNK_NEON_RAIN",
        titleEn = "Cyber Rain",
        titleBn = "সাইবার রেইন",
        baseGradient = listOf(Color(0xFF060912), Color(0xFF0A1220), Color(0xFF0F1B2F), Color(0xFF060912)),
        orb1Colors = listOf(Color(0x5500F0FF), Color(0x220088FF), Color.Transparent),
        orb2Colors = listOf(Color(0x44FF007F), Color(0x1C7928CA), Color.Transparent),
        orb3Colors = listOf(Color(0x3800E5FF), Color(0x140055FF), Color.Transparent),
        particleColor = Color(0xFF00F0FF),
        accentColor = Color(0xFF00F0FF),
        drawableRes = R.drawable.img_bg_cyber_rain,
        animationType = ThemeAnimationType.CYBER_RAIN
    ),
    AURORA_NIGHT_FOREST(
        key = "AURORA_NIGHT_FOREST",
        titleEn = "Aurora Forest",
        titleBn = "অরোরা ফরেস্ট",
        baseGradient = listOf(Color(0xFF030D0B), Color(0xFF061A16), Color(0xFF0A2822), Color(0xFF030D0B)),
        orb1Colors = listOf(Color(0x5500E676), Color(0x2200B0FF), Color.Transparent),
        orb2Colors = listOf(Color(0x4469F0AE), Color(0x1C00E5FF), Color.Transparent),
        orb3Colors = listOf(Color(0x38AEEA00), Color(0x1400C853), Color.Transparent),
        particleColor = Color(0xFF69F0AE),
        accentColor = Color(0xFF00E676),
        drawableRes = R.drawable.img_bg_aurora_forest,
        animationType = ThemeAnimationType.AURORA_STARDUST
    ),
    DEEP_OCEAN_ABYSS(
        key = "DEEP_OCEAN_ABYSS",
        titleEn = "Deep Ocean",
        titleBn = "ডিপ ওশান",
        baseGradient = listOf(Color(0xFF030914), Color(0xFF061426), Color(0xFF0A2038), Color(0xFF030914)),
        orb1Colors = listOf(Color(0x5500F5D4), Color(0x2200BBF9), Color.Transparent),
        orb2Colors = listOf(Color(0x440077B6), Color(0x1C023E8A), Color.Transparent),
        orb3Colors = listOf(Color(0x3800B4D8), Color(0x1403045E), Color.Transparent),
        particleColor = Color(0xFF00F5D4),
        accentColor = Color(0xFF00F5D4),
        drawableRes = R.drawable.img_bg_deep_ocean,
        animationType = ThemeAnimationType.OCEAN_BUBBLES
    );

    companion object {
        fun fromKey(key: String): BackgroundThemeType {
            return entries.find { it.key.equals(key, ignoreCase = true) }
                ?: CHERRY_BLOSSOM_NIGHT
        }
    }
}

// Particle model for smooth floating background stardust
private data class AmbientParticle(
    val relX: Float,
    val relY: Float,
    val baseRadius: Float,
    val phaseOffset: Float,
    val speed: Float
)

private val staticParticles = listOf(
    AmbientParticle(0.12f, 0.15f, 2.2f, 0.0f, 1.1f),
    AmbientParticle(0.28f, 0.08f, 1.8f, 1.4f, 0.8f),
    AmbientParticle(0.45f, 0.22f, 3.0f, 2.8f, 1.3f),
    AmbientParticle(0.68f, 0.12f, 2.0f, 4.1f, 0.9f),
    AmbientParticle(0.85f, 0.25f, 2.6f, 0.7f, 1.2f),
    AmbientParticle(0.18f, 0.38f, 1.9f, 3.2f, 0.7f),
    AmbientParticle(0.35f, 0.45f, 2.8f, 5.0f, 1.4f),
    AmbientParticle(0.78f, 0.42f, 2.2f, 1.9f, 1.0f),
    AmbientParticle(0.92f, 0.52f, 1.6f, 3.8f, 0.8f),
    AmbientParticle(0.08f, 0.62f, 2.5f, 0.5f, 1.1f),
    AmbientParticle(0.24f, 0.70f, 3.2f, 2.2f, 1.5f),
    AmbientParticle(0.52f, 0.58f, 1.8f, 4.4f, 0.9f),
    AmbientParticle(0.70f, 0.68f, 2.7f, 1.1f, 1.3f),
    AmbientParticle(0.88f, 0.75f, 2.0f, 3.5f, 0.8f),
    AmbientParticle(0.15f, 0.85f, 2.4f, 5.3f, 1.0f),
    AmbientParticle(0.40f, 0.82f, 1.7f, 0.9f, 0.7f),
    AmbientParticle(0.62f, 0.89f, 3.1f, 2.6f, 1.4f),
    AmbientParticle(0.82f, 0.92f, 2.1f, 4.8f, 1.1f)
)

// Physics model for falling sakura cherry blossom petals
private data class FallingPetal(
    val relX: Float,
    val relY: Float,
    val width: Float,
    val height: Float,
    val fallSpeed: Float,
    val swaySpeed: Float,
    val swayDistance: Float,
    val flipSpeed: Float,
    val rotationBase: Float,
    val rotationSpeed: Float,
    val phase: Float,
    val petalColor: Color,
    val edgeColor: Color
)

private val staticPetals = listOf(
    FallingPetal(0.05f, 0.05f, 11f, 17f, 0.12f, 1.2f, 32f, 1.8f, 15f, 45f, 0.2f, Color(0xFFFFB7C5), Color(0xFFFF69B4)),
    FallingPetal(0.18f, 0.15f, 14f, 22f, 0.16f, 1.5f, 44f, 2.2f, 40f, 60f, 1.4f, Color(0xFFFF85A1), Color(0xFFFF1493)),
    FallingPetal(0.32f, 0.02f, 9f, 14f, 0.10f, 0.9f, 24f, 1.4f, 80f, 35f, 2.8f, Color(0xFFFFF0F5), Color(0xFFFFB7C5)),
    FallingPetal(0.48f, 0.22f, 13f, 20f, 0.14f, 1.3f, 38f, 2.0f, 120f, 50f, 0.8f, Color(0xFFFF69B4), Color(0xFFFF4081)),
    FallingPetal(0.62f, 0.10f, 10f, 16f, 0.11f, 1.1f, 30f, 1.6f, 200f, 40f, 3.5f, Color(0xFFFFB7C5), Color(0xFFFF85A1)),
    FallingPetal(0.78f, 0.04f, 15f, 23f, 0.17f, 1.6f, 48f, 2.4f, 260f, 65f, 4.2f, Color(0xFFFF85A1), Color(0xFFFF1493)),
    FallingPetal(0.92f, 0.18f, 11f, 17f, 0.13f, 1.0f, 28f, 1.7f, 310f, 45f, 5.1f, Color(0xFFFFF0F5), Color(0xFFFFB7C5)),
    FallingPetal(0.12f, 0.35f, 12f, 19f, 0.15f, 1.4f, 40f, 2.1f, 45f, 55f, 1.9f, Color(0xFFFF69B4), Color(0xFFFF85A1)),
    FallingPetal(0.25f, 0.48f, 8f, 13f, 0.09f, 0.8f, 22f, 1.3f, 95f, 30f, 3.1f, Color(0xFFFFF0F5), Color(0xFFFFB7C5)),
    FallingPetal(0.42f, 0.40f, 14f, 21f, 0.16f, 1.5f, 46f, 2.3f, 140f, 58f, 0.5f, Color(0xFFFF85A1), Color(0xFFFF1493)),
    FallingPetal(0.58f, 0.52f, 10f, 16f, 0.12f, 1.2f, 32f, 1.8f, 185f, 42f, 2.4f, Color(0xFFFFB7C5), Color(0xFFFF69B4)),
    FallingPetal(0.72f, 0.38f, 13f, 20f, 0.14f, 1.3f, 36f, 1.9f, 230f, 48f, 4.8f, Color(0xFFFF69B4), Color(0xFFFF85A1)),
    FallingPetal(0.85f, 0.45f, 15f, 24f, 0.18f, 1.7f, 50f, 2.5f, 290f, 70f, 1.1f, Color(0xFFFF85A1), Color(0xFFFF1493)),
    FallingPetal(0.08f, 0.65f, 11f, 18f, 0.13f, 1.1f, 30f, 1.7f, 35f, 44f, 5.7f, Color(0xFFFFB7C5), Color(0xFFFF69B4)),
    FallingPetal(0.28f, 0.72f, 13f, 20f, 0.15f, 1.4f, 42f, 2.0f, 85f, 52f, 2.2f, Color(0xFFFF69B4), Color(0xFFFF85A1)),
    FallingPetal(0.50f, 0.68f, 9f, 15f, 0.10f, 0.9f, 25f, 1.5f, 165f, 36f, 3.9f, Color(0xFFFFF0F5), Color(0xFFFFB7C5)),
    FallingPetal(0.68f, 0.78f, 14f, 22f, 0.16f, 1.5f, 45f, 2.2f, 215f, 62f, 0.7f, Color(0xFFFF85A1), Color(0xFFFF1493)),
    FallingPetal(0.88f, 0.62f, 12f, 19f, 0.14f, 1.3f, 38f, 1.9f, 320f, 48f, 4.4f, Color(0xFFFFB7C5), Color(0xFFFF85A1)),
    FallingPetal(0.15f, 0.88f, 10f, 16f, 0.11f, 1.0f, 28f, 1.6f, 60f, 40f, 1.6f, Color(0xFFFFF0F5), Color(0xFFFFB7C5)),
    FallingPetal(0.38f, 0.85f, 14f, 21f, 0.16f, 1.5f, 44f, 2.2f, 110f, 56f, 5.3f, Color(0xFFFF85A1), Color(0xFFFF1493)),
    FallingPetal(0.60f, 0.92f, 11f, 17f, 0.12f, 1.2f, 34f, 1.8f, 195f, 46f, 2.7f, Color(0xFFFF69B4), Color(0xFFFFB7C5)),
    FallingPetal(0.80f, 0.86f, 13f, 20f, 0.15f, 1.4f, 40f, 2.0f, 275f, 54f, 4.0f, Color(0xFFFFB7C5), Color(0xFFFF85A1)),
    FallingPetal(0.95f, 0.90f, 8f, 14f, 0.10f, 0.9f, 26f, 1.4f, 340f, 38f, 1.2f, Color(0xFFFFF0F5), Color(0xFFFF69B4)),
    FallingPetal(0.46f, 0.12f, 16f, 25f, 0.19f, 1.8f, 54f, 2.6f, 150f, 75f, 3.3f, Color(0xFFFF85A1), Color(0xFFFF1493))
)

// Physics model for falling golden/red autumn maple leaves
private data class FallingMapleLeaf(
    val relX: Float,
    val relY: Float,
    val size: Float,
    val fallSpeed: Float,
    val swaySpeed: Float,
    val swayDistance: Float,
    val rotationSpeed: Float,
    val phase: Float,
    val color: Color
)

private val staticMapleLeaves = listOf(
    FallingMapleLeaf(0.08f, 0.02f, 18f, 0.11f, 1.1f, 34f, 50f, 0.3f, Color(0xFFFF3D00)),
    FallingMapleLeaf(0.25f, 0.12f, 22f, 0.15f, 1.4f, 42f, 65f, 1.6f, Color(0xFFFF9100)),
    FallingMapleLeaf(0.45f, 0.05f, 16f, 0.09f, 0.9f, 26f, 40f, 3.2f, Color(0xFFFF6D00)),
    FallingMapleLeaf(0.68f, 0.18f, 24f, 0.16f, 1.6f, 48f, 70f, 0.9f, Color(0xFFDD2C00)),
    FallingMapleLeaf(0.88f, 0.08f, 19f, 0.12f, 1.2f, 36f, 55f, 4.5f, Color(0xFFFFAB00)),
    FallingMapleLeaf(0.18f, 0.35f, 20f, 0.13f, 1.3f, 38f, 60f, 2.1f, Color(0xFFFF5722)),
    FallingMapleLeaf(0.38f, 0.45f, 17f, 0.10f, 1.0f, 28f, 45f, 5.0f, Color(0xFFFF9800)),
    FallingMapleLeaf(0.58f, 0.38f, 23f, 0.15f, 1.5f, 45f, 68f, 1.2f, Color(0xFFE64A19)),
    FallingMapleLeaf(0.82f, 0.48f, 21f, 0.14f, 1.4f, 40f, 58f, 3.8f, Color(0xFFFF6D00)),
    FallingMapleLeaf(0.12f, 0.68f, 19f, 0.12f, 1.1f, 32f, 52f, 0.6f, Color(0xFFFF3D00)),
    FallingMapleLeaf(0.32f, 0.75f, 22f, 0.15f, 1.5f, 44f, 66f, 2.7f, Color(0xFFFFAB00)),
    FallingMapleLeaf(0.52f, 0.62f, 16f, 0.10f, 0.9f, 25f, 42f, 4.1f, Color(0xFFDD2C00)),
    FallingMapleLeaf(0.75f, 0.72f, 24f, 0.16f, 1.6f, 50f, 72f, 1.8f, Color(0xFFFF9100)),
    FallingMapleLeaf(0.92f, 0.82f, 18f, 0.11f, 1.2f, 35f, 48f, 5.4f, Color(0xFFFF5722)),
    FallingMapleLeaf(0.22f, 0.90f, 20f, 0.13f, 1.3f, 38f, 56f, 3.0f, Color(0xFFFF6D00)),
    FallingMapleLeaf(0.62f, 0.88f, 21f, 0.14f, 1.4f, 42f, 62f, 0.4f, Color(0xFFFF3D00))
)

// Physics model for cyber rain streaks
private data class CyberRainStreak(
    val relX: Float,
    val relY: Float,
    val length: Float,
    val speed: Float,
    val width: Float,
    val color: Color
)

private val staticCyberRain = listOf(
    CyberRainStreak(0.04f, 0.05f, 28f, 0.85f, 1.5f, Color(0xFF00F0FF)),
    CyberRainStreak(0.12f, 0.22f, 36f, 1.10f, 2.0f, Color(0xFFFF007F)),
    CyberRainStreak(0.20f, 0.08f, 24f, 0.75f, 1.2f, Color(0xFF00E5FF)),
    CyberRainStreak(0.28f, 0.45f, 40f, 1.25f, 2.2f, Color(0xFF00F0FF)),
    CyberRainStreak(0.36f, 0.15f, 30f, 0.90f, 1.6f, Color(0xFFE040FB)),
    CyberRainStreak(0.44f, 0.60f, 34f, 1.05f, 1.8f, Color(0xFF00F0FF)),
    CyberRainStreak(0.52f, 0.30f, 26f, 0.80f, 1.4f, Color(0xFF80D8FF)),
    CyberRainStreak(0.60f, 0.75f, 38f, 1.15f, 2.0f, Color(0xFFFF007F)),
    CyberRainStreak(0.68f, 0.18f, 32f, 0.95f, 1.7f, Color(0xFF00F0FF)),
    CyberRainStreak(0.76f, 0.50f, 42f, 1.30f, 2.4f, Color(0xFF00E5FF)),
    CyberRainStreak(0.84f, 0.10f, 25f, 0.78f, 1.3f, Color(0xFFE040FB)),
    CyberRainStreak(0.92f, 0.65f, 35f, 1.08f, 1.9f, Color(0xFF00F0FF)),
    CyberRainStreak(0.08f, 0.80f, 30f, 0.92f, 1.5f, Color(0xFF80D8FF)),
    CyberRainStreak(0.24f, 0.88f, 38f, 1.18f, 2.1f, Color(0xFFFF007F)),
    CyberRainStreak(0.40f, 0.92f, 28f, 0.88f, 1.6f, Color(0xFF00F0FF)),
    CyberRainStreak(0.56f, 0.85f, 36f, 1.12f, 1.8f, Color(0xFF00E5FF)),
    CyberRainStreak(0.72f, 0.95f, 32f, 0.98f, 1.7f, Color(0xFFE040FB)),
    CyberRainStreak(0.88f, 0.82f, 40f, 1.22f, 2.2f, Color(0xFF00F0FF))
)

// Physics model for rising bioluminescent ocean bubbles
private data class OceanBubble(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val riseSpeed: Float,
    val wobbleSpeed: Float,
    val wobbleDistance: Float,
    val phase: Float,
    val color: Color
)

private val staticOceanBubbles = listOf(
    OceanBubble(0.08f, 0.85f, 7f, 0.10f, 1.2f, 16f, 0.2f, Color(0xFF00F5D4)),
    OceanBubble(0.22f, 0.95f, 12f, 0.14f, 1.5f, 22f, 1.4f, Color(0xFF00B4D8)),
    OceanBubble(0.35f, 0.70f, 5f, 0.08f, 0.9f, 12f, 2.8f, Color(0xFF80FFDB)),
    OceanBubble(0.48f, 0.90f, 10f, 0.12f, 1.3f, 18f, 0.8f, Color(0xFF00F5D4)),
    OceanBubble(0.62f, 0.80f, 8f, 0.11f, 1.1f, 15f, 3.5f, Color(0xFF00B4D8)),
    OceanBubble(0.78f, 0.98f, 14f, 0.15f, 1.6f, 24f, 4.2f, Color(0xFF80FFDB)),
    OceanBubble(0.90f, 0.75f, 6f, 0.09f, 1.0f, 14f, 5.1f, Color(0xFF00F5D4)),
    OceanBubble(0.15f, 0.50f, 9f, 0.12f, 1.4f, 17f, 1.9f, Color(0xFF00B4D8)),
    OceanBubble(0.30f, 0.40f, 11f, 0.13f, 1.3f, 20f, 3.1f, Color(0xFF80FFDB)),
    OceanBubble(0.55f, 0.45f, 7f, 0.10f, 1.1f, 15f, 0.5f, Color(0xFF00F5D4)),
    OceanBubble(0.70f, 0.35f, 13f, 0.15f, 1.5f, 22f, 2.4f, Color(0xFF00B4D8)),
    OceanBubble(0.85f, 0.55f, 8f, 0.11f, 1.2f, 16f, 4.8f, Color(0xFF80FFDB)),
    OceanBubble(0.18f, 0.20f, 10f, 0.12f, 1.3f, 18f, 1.1f, Color(0xFF00F5D4)),
    OceanBubble(0.42f, 0.15f, 6f, 0.08f, 0.9f, 12f, 5.7f, Color(0xFF00B4D8)),
    OceanBubble(0.65f, 0.25f, 11f, 0.14f, 1.4f, 20f, 2.2f, Color(0xFF80FFDB)),
    OceanBubble(0.82f, 0.10f, 8f, 0.10f, 1.0f, 14f, 3.9f, Color(0xFF00F5D4))
)

/**
 * Shared animated canvas for rendering the living wallpaper with physics-based particles,
 * falling petals, autumn leaves, cyber rain, aurora waves, or ocean bubbles.
 */
@Composable
fun AnimatedThemeCanvas(
    theme: BackgroundThemeType,
    modifier: Modifier = Modifier,
    isThumbnail: Boolean = false
) {
    val context = LocalContext.current
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

    val infiniteTransition = rememberInfiniteTransition(label = "theme_canvas_anim")

    val driftPhase by if (animationsDisabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(16000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambient_drift_phase"
        )
    }

    val continuousTime by if (animationsDisabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 60f,
            animationSpec = infiniteRepeatable(
                animation = tween(60000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "continuous_time"
        )
    }

    val pulseGlow by if (animationsDisabled) {
        remember { mutableFloatStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
    }

    val particleTwinkle by if (animationsDisabled) {
        remember { mutableFloatStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particle_twinkle"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = theme.baseGradient))
    ) {
        // Wallpaper Image
        Image(
            painter = painterResource(id = theme.drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Contrast Veil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isThumbnail) {
                            listOf(
                                Color(0x3305070D),
                                Color(0x55080E18),
                                Color(0x7705070D)
                            )
                        } else {
                            listOf(
                                Color(0x6605070D),
                                Color(0x7A080E18),
                                Color(0xAA05070D)
                            )
                        }
                    )
                )
        )

        // Dynamic Animation Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val scaleFactor = if (isThumbnail) (w / 360f).coerceIn(0.4f, 1f) else 1f

                    // 1. Undulating glowing orbs
                    val orb1X = w * (0.80f + 0.09f * cos(driftPhase.toDouble()).toFloat())
                    val orb1Y = h * (0.18f + 0.07f * sin(driftPhase.toDouble()).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = theme.orb1Colors,
                            center = Offset(orb1X, orb1Y),
                            radius = w * 0.80f * pulseGlow
                        )
                    )

                    val orb2X = w * (0.16f + 0.08f * sin(driftPhase.toDouble()).toFloat())
                    val orb2Y = h * (0.50f + 0.09f * cos(driftPhase.toDouble()).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = theme.orb2Colors,
                            center = Offset(orb2X, orb2Y),
                            radius = w * 0.85f * (2.14f - pulseGlow)
                        )
                    )

                    // 2. Specialized Physics-Based Animation Layer
                    when (theme.animationType) {
                        ThemeAnimationType.SAKURA_PETALS -> {
                            val petalsToRender = if (isThumbnail) staticPetals.take(12) else staticPetals
                            petalsToRender.forEach { petal ->
                                val t = continuousTime
                                val normalizedY = (petal.relY + t * petal.fallSpeed) % 1.0f
                                val py = normalizedY * h

                                val swayPhase = t * petal.swaySpeed + petal.phase
                                val windGust = sin((t * 0.4f).toDouble()).toFloat() * (14f * scaleFactor)
                                val px = (petal.relX * w + sin(swayPhase.toDouble()).toFloat() * (petal.swayDistance * scaleFactor) + windGust)
                                    .coerceIn(-20f, w + 20f)

                                val rotationDeg = petal.rotationBase + t * petal.rotationSpeed
                                val flipAngle = t * petal.flipSpeed + petal.phase
                                val scaleX = cos(flipAngle.toDouble()).toFloat().coerceIn(-1f, 1f)

                                val pw = petal.width.dp.toPx() * scaleFactor
                                val ph = petal.height.dp.toPx() * scaleFactor

                                rotate(degrees = rotationDeg, pivot = Offset(px, py)) {
                                    scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(px, py)) {
                                        val petalPath = Path().apply {
                                            moveTo(px, py - ph * 0.5f)
                                            cubicTo(
                                                px - pw * 0.65f, py - ph * 0.35f,
                                                px - pw * 0.65f, py + ph * 0.25f,
                                                px, py + ph * 0.5f
                                            )
                                            cubicTo(
                                                px + pw * 0.65f, py + ph * 0.25f,
                                                px + pw * 0.65f, py - ph * 0.35f,
                                                px, py - ph * 0.5f
                                            )
                                            close()
                                        }

                                        drawPath(
                                            path = petalPath,
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    petal.edgeColor.copy(alpha = 0.85f),
                                                    petal.petalColor.copy(alpha = 0.92f),
                                                    Color.White.copy(alpha = 0.75f)
                                                ),
                                                center = Offset(px, py),
                                                radius = ph * 0.6f
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        ThemeAnimationType.MAPLE_LEAVES -> {
                            val leavesToRender = if (isThumbnail) staticMapleLeaves.take(9) else staticMapleLeaves
                            leavesToRender.forEach { leaf ->
                                val t = continuousTime
                                val normalizedY = (leaf.relY + t * leaf.fallSpeed) % 1.0f
                                val py = normalizedY * h

                                val swayPhase = t * leaf.swaySpeed + leaf.phase
                                val px = (leaf.relX * w + sin(swayPhase.toDouble()).toFloat() * (leaf.swayDistance * scaleFactor))
                                    .coerceIn(-20f, w + 20f)

                                val rotationDeg = leaf.phase * 50f + t * leaf.rotationSpeed
                                val s = leaf.size.dp.toPx() * scaleFactor

                                rotate(degrees = rotationDeg, pivot = Offset(px, py)) {
                                    val leafPath = Path().apply {
                                        moveTo(px, py - s * 0.6f)
                                        lineTo(px - s * 0.25f, py - s * 0.2f)
                                        lineTo(px - s * 0.55f, py - s * 0.3f)
                                        lineTo(px - s * 0.35f, py + s * 0.1f)
                                        lineTo(px - s * 0.45f, py + s * 0.35f)
                                        lineTo(px, py + s * 0.55f)
                                        lineTo(px + s * 0.45f, py + s * 0.35f)
                                        lineTo(px + s * 0.35f, py + s * 0.1f)
                                        lineTo(px + s * 0.55f, py - s * 0.3f)
                                        lineTo(px + s * 0.25f, py - s * 0.2f)
                                        close()
                                    }

                                    drawPath(
                                        path = leafPath,
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.75f),
                                                leaf.color.copy(alpha = 0.90f),
                                                Color(0xFF8D1C00).copy(alpha = 0.85f)
                                            ),
                                            center = Offset(px, py),
                                            radius = s * 0.6f
                                        )
                                    )
                                }
                            }
                        }

                        ThemeAnimationType.CYBER_RAIN -> {
                            val rainToRender = if (isThumbnail) staticCyberRain.take(10) else staticCyberRain
                            rainToRender.forEach { rain ->
                                val t = continuousTime * 4f
                                val normalizedY = (rain.relY + t * rain.speed) % 1.0f
                                val py = normalizedY * h
                                val px = (rain.relX * w + (t * 0.12f * w) % (w * 0.1f)).coerceIn(0f, w)

                                val streakLen = rain.length.dp.toPx() * scaleFactor
                                val streakWidth = rain.width.dp.toPx() * scaleFactor

                                drawLine(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            rain.color.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.95f)
                                        ),
                                        startY = py - streakLen,
                                        endY = py
                                    ),
                                    start = Offset(px - streakLen * 0.2f, py - streakLen),
                                    end = Offset(px, py),
                                    strokeWidth = streakWidth
                                )
                            }
                        }

                        ThemeAnimationType.AURORA_STARDUST -> {
                            // Aurora waves
                            val auroraY = h * (0.20f + 0.06f * sin(driftPhase.toDouble()).toFloat())
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x5500E676),
                                        Color(0x4000E5FF),
                                        Color(0x209D4EDD),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, auroraY - 60f),
                                    end = Offset(w, auroraY + 180f)
                                ),
                                size = Size(w, h * 0.50f)
                            )

                            // Twinkling stardust
                            val particles = if (isThumbnail) staticParticles.take(10) else staticParticles
                            particles.forEach { p ->
                                val particlePhase = driftPhase * p.speed + p.phaseOffset
                                val offsetY = 12f * sin(particlePhase.toDouble()).toFloat()
                                val offsetX = 6f * cos(particlePhase.toDouble()).toFloat()
                                val px = (p.relX * w + offsetX).coerceIn(0f, w)
                                val py = (p.relY * h + offsetY).coerceIn(0f, h)

                                val alphaMod = ((sin(particlePhase.toDouble()).toFloat() + 1f) / 2f)
                                val alpha = (0.35f + 0.55f * alphaMod) * particleTwinkle

                                drawCircle(
                                    color = theme.particleColor.copy(alpha = alpha * 0.4f),
                                    radius = p.baseRadius * 2.6f * scaleFactor,
                                    center = Offset(px, py)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha * 0.9f),
                                    radius = p.baseRadius * 0.9f * scaleFactor,
                                    center = Offset(px, py)
                                )
                            }
                        }

                        ThemeAnimationType.OCEAN_BUBBLES -> {
                            val bubblesToRender = if (isThumbnail) staticOceanBubbles.take(9) else staticOceanBubbles
                            bubblesToRender.forEach { bubble ->
                                val t = continuousTime
                                val normalizedY = 1.0f - ((1.0f - bubble.relY + t * bubble.riseSpeed) % 1.0f)
                                val py = normalizedY * h

                                val wobblePhase = t * bubble.wobbleSpeed + bubble.phase
                                val px = (bubble.relX * w + sin(wobblePhase.toDouble()).toFloat() * (bubble.wobbleDistance * scaleFactor))
                                    .coerceIn(0f, w)

                                val r = bubble.radius.dp.toPx() * scaleFactor

                                // Translucent bubble body
                                drawCircle(
                                    color = bubble.color.copy(alpha = 0.22f),
                                    radius = r,
                                    center = Offset(px, py)
                                )
                                // Rim stroke
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.65f),
                                    radius = r,
                                    center = Offset(px, py),
                                    style = Stroke(width = 1.2.dp.toPx() * scaleFactor)
                                )
                                // Specular highlight glint
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = r * 0.28f,
                                    center = Offset(px - r * 0.35f, py - r * 0.35f)
                                )
                            }
                        }
                    }
                }
        )
    }
}

/**
 * Renders the living atmospheric backdrop with multi-layered, undulating, breathing luminous orbs,
 * and live animated falling petals, autumn leaves, cyber rain, or ocean bubbles according to [theme].
 */
@Composable
fun LiquidBackground(
    theme: String = "CHERRY_BLOSSOM_NIGHT",
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val currentTheme = remember(theme) { BackgroundThemeType.fromKey(theme) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedThemeCanvas(
            theme = currentTheme,
            modifier = Modifier.fillMaxSize(),
            isThumbnail = false
        )
        content()
    }
}

/**
 * Beautiful visual card preview for selecting themes in Settings.
 * Shows the live animated wallpaper scene inside a high-contrast rounded glass card with short title.
 */
@Composable
fun ThemePreviewCard(
    theme: BackgroundThemeType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            listOf(
                                Color.White,
                                theme.accentColor,
                                Color(0xFF00E5FF)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Live animation preview inside thumbnail
            AnimatedThemeCanvas(
                theme = theme,
                modifier = Modifier.fillMaxSize(),
                isThumbnail = true
            )

            // Active selection badge
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Concise title underneath - NO clutter text
        Text(
            text = theme.titleEn,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 12.5.sp
            ),
            color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        Text(
            text = theme.titleBn,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            ),
            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.85f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            maxLines = 1
        )
    }
}
