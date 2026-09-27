package com.example.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.ui.draw.blur
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
 * Unified Dark AMOLED & Subtle Glass Design System for FocusLock.
 *
 * Inspired by the Permission Center reference and FocusLock branding:
 * - Deep navy and charcoal surfaces (#0D1626 - #132034) with high readability (86-92% opacity)
 * - Soft, controlled background blur and atmospheric dimming so wallpaper never competes with text
 * - Readable white (#F8FAFC) and light-gray (#A8B8CC) typography
 * - Cyan/turquoise primary accent (#00E5FF / #24DFEC)
 * - Subtle 1dp borders and soft shadows
 * - Consistent rounded corners and spacing across all screens
 */
object LiquidGlass {
    // Deep Navy & Charcoal Translucent Glass Surfaces
    val GlassTintDark = Color(0xE0101B2D)            // 88% deep navy-charcoal glass
    val GlassSurfaceDark = Color(0xE00E1829)         // Base card dark tinted glass surface
    val GlassSurfaceElevatedDark = Color(0xEB142238) // Elevated card / modal surface (92% opacity)
    val GlassInputSurface = Color(0xD90A121F)        // Search bar / input field recessed surface

    // Primary & Accent Highlights
    val GlassHighlightCyan = Color(0xFF00E5FF)       // Primary electric cyan
    val GlassHighlightTurquoise = Color(0xFF24DFEC)  // Secondary turquoise accent
    val GlassHighlightPurple = Color(0xFF9D4EDD)     // Ambient violet accent
    val GlassHighlightBlue = Color(0xFF2979FF)       // Ambient azure accent

    // Subtle Borders & Dividers
    val GlassBorderLuminous = Color(0x7000E5FF)
    val GlassBorderSpecularWhite = Color(0x29FFFFFF) // 16% crisp subtle white border
    val GlassBorderSubtleDark = Color(0x1FFFFFFF)    // 12% subtle divider/border

    // Typography Colors for Guaranteed Readability
    val TextPrimary = Color(0xFFF8FAFC)              // Crisp readable white
    val TextSecondary = Color(0xFFB0C0D4)            // Readable cool light-gray
    val TextMuted = Color(0xFF7E92AA)                // Subtle caption gray

    @Composable
    fun cardColor(isElevated: Boolean = false): Color {
        return if (isElevated) GlassSurfaceElevatedDark else GlassSurfaceDark
    }

    @Composable
    fun borderColor(isHighlight: Boolean = false): Color {
        return if (isHighlight) Color(0x7500E5FF) else Color(0x26FFFFFF)
    }

    @Composable
    fun borderBrush(isHighlight: Boolean = false): Brush {
        return if (isHighlight) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.65f),
                    Color(0xFF24DFEC).copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.18f),
                    Color(0xFF00E5FF).copy(alpha = 0.30f)
                ),
                start = Offset(0f, 0f),
                end = Offset(800f, 800f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.14f)
                ),
                start = Offset(0f, 0f),
                end = Offset(800f, 800f)
            )
        }
    }
}

/**
 * Primary surface modifier for all cards and containers in FocusLock.
 *
 * Uses a consistent dark navy-charcoal tinted glass surface (86%-92% opacity) so text, icons,
 * and controls remain effortlessly readable over any wallpaper, with a subtle 1dp border
 * and soft top-edge glass sheen.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(22.dp),
    isElevated: Boolean = false,
    isHighlight: Boolean = false,
    alphaMultiplier: Float = 1.0f,
    borderWidth: Dp = 1.dp
): Modifier {
    val clampedScale = alphaMultiplier.coerceIn(0.85f, 1.10f)
    val topColor = when {
        isHighlight -> Color(0xFF13283F).copy(alpha = (0.76f * clampedScale).coerceIn(0.68f, 0.88f))
        isElevated -> Color(0xFF152338).copy(alpha = (0.80f * clampedScale).coerceIn(0.72f, 0.90f))
        else -> Color(0xFF101B2D).copy(alpha = (0.72f * clampedScale).coerceIn(0.64f, 0.85f))
    }
    val midColor = when {
        isHighlight -> Color(0xFF0E1F33).copy(alpha = (0.76f * clampedScale).coerceIn(0.68f, 0.88f))
        isElevated -> Color(0xFF101C2E).copy(alpha = (0.80f * clampedScale).coerceIn(0.72f, 0.90f))
        else -> Color(0xFF0C1524).copy(alpha = (0.72f * clampedScale).coerceIn(0.64f, 0.85f))
    }
    val bottomColor = when {
        isHighlight -> Color(0xFF0B1828).copy(alpha = (0.78f * clampedScale).coerceIn(0.70f, 0.90f))
        isElevated -> Color(0xFF0D1726).copy(alpha = (0.82f * clampedScale).coerceIn(0.74f, 0.92f))
        else -> Color(0xFF09111E).copy(alpha = (0.75f * clampedScale).coerceIn(0.66f, 0.88f))
    }

    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(topColor, midColor, bottomColor)
            )
        )
        .drawWithContent {
            drawContent()
            // Subtle top glass edge highlight
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (isHighlight) Color(0x2400E5FF) else Color(0x12FFFFFF),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 28.dp.toPx()
                )
            )
        }
        .border(
            width = borderWidth,
            brush = if (isHighlight) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.60f),
                        Color(0xFF24DFEC).copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.16f),
                        Color(0xFF00E5FF).copy(alpha = 0.28f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(500f, 500f)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color(0xFF94A3B8).copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.07f),
                        Color(0xFF94A3B8).copy(alpha = 0.14f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(500f, 500f)
                )
            },
            shape = shape
        )
}

/**
 * Interactive modifier for Liquid Glass elements:
 * Provides tactile elastic scale compression on press (0.98f) with spring physics
 * and a subtle cyan highlight feedback when touched.
 */
fun Modifier.liquidGlassPressable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = this.composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glass_press_scale"
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
                    color = Color(0xFF00E5FF).copy(alpha = 0.07f)
                )
            }
        }
}

/**
 * Dedicated Liquid Glass Card Composable with consistent dark tinted glass surface and optional press animation.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
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
 * Button styles supported by the global Glass Design System.
 */
enum class GlassButtonStyle {
    PRIMARY,     // Bright cyan/turquoise fill with dark navy text
    SECONDARY,   // Dark navy tinted glass with subtle border and crisp white text
    WARNING,     // Warm amber tinted glass
    DESTRUCTIVE  // Crimson tinted glass for irreversible actions
}

/**
 * Unified Glass Button matching the Permission Center reference design.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    style: GlassButtonStyle = GlassButtonStyle.PRIMARY,
    shape: Shape = RoundedCornerShape(14.dp),
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glass_button_scale"
    )

    val bgModifier = when (style) {
        GlassButtonStyle.PRIMARY -> Modifier
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFF24DFEC))
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(Color(0x5500E5FF), Color(0x5524DFEC))
                    )
                }
            )
        GlassButtonStyle.SECONDARY -> Modifier
            .background(Color(0xE6162438))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
        GlassButtonStyle.WARNING -> Modifier
            .background(Color(0xE62B1D10))
            .border(1.dp, Color(0x90FFA726), shape)
        GlassButtonStyle.DESTRUCTIVE -> Modifier
            .background(Color(0xE62D1219))
            .border(1.dp, Color(0x90EF4444), shape)
    }

    val textColor = when (style) {
        GlassButtonStyle.PRIMARY -> if (enabled) Color(0xFF04151F) else Color(0x9904151F)
        GlassButtonStyle.SECONDARY -> if (enabled) Color(0xFFF8FAFC) else Color(0x80F8FAFC)
        GlassButtonStyle.WARNING -> Color(0xFFFFB74D)
        GlassButtonStyle.DESTRUCTIVE -> Color(0xFFFF6E6E)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(bgModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                ),
                color = textColor
            )
        }
    }
}

/**
 * Dedicated Liquid Glass Interactive Button (Backwards compatible).
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
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

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (isPrimary) {
                    Modifier.background(
                        brush = if (enabled) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF24DFEC))
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0x5500E5FF), Color(0x5524DFEC))
                            )
                        }
                    )
                } else {
                    Modifier
                        .background(Color(0xE6162438))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
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
 * Frosted Glass Circular Icon Bubble (48-50dp).
 */
@Composable
fun GlassIconBubble(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    isHighlight: Boolean = false,
    tint: Color? = null,
    contentDescription: String? = null
) {
    val effectiveTint = tint ?: if (isHighlight) Color(0xFF00E5FF) else Color(0xFFF1F5F9)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isHighlight) Color(0x2600E5FF)
                else Color(0xE617263B)
            )
            .border(
                1.dp,
                if (isHighlight) Color(0x6600E5FF)
                else Color.White.copy(alpha = 0.16f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Section Header with cyan/white dot indicator.
 */
@Composable
fun GlassSectionHeader(
    title: String,
    subtitle: String? = null,
    isCritical: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primaryCyan = Color(0xFF00E5FF)

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isCritical) primaryCyan else Color(0xFF38BDF8))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    fontSize = 12.sp
                ),
                color = if (isCritical) primaryCyan else Color(0xFFE2E8F0)
            )
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LiquidGlass.TextSecondary,
                    fontSize = 12.5.sp
                )
            )
        }
    }
}

/**
 * Status Badge matching SETUP NEEDED / PROTECTED from Permission Center reference.
 */
@Composable
fun GlassStatusBadge(
    text: String,
    isWarning: Boolean = false,
    isHighlight: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isWarning -> Color(0xE62D141C)
        isHighlight -> Color(0x2900E5FF)
        else -> Color(0xE6142538)
    }
    val strokeColor = when {
        isWarning -> Color(0x88FF5252)
        isHighlight -> Color(0x8000E5FF)
        else -> Color(0x5500E5FF)
    }
    val contentColor = if (isWarning) Color(0xFFFF6E6E) else Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, strokeColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Standardized Progress Bar with deep recessed track and cyan-turquoise gradient indicator.
 */
@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    isWarning: Boolean = false
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Color(0xFF080E1A))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), CircleShape)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(CircleShape)
                    .background(
                        brush = if (isWarning) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF9100), Color(0xFFFF5252))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF10B981))
                            )
                        }
                    )
            )
        }
    }
}

/**
 * Clean glass empty state component.
 */
@Composable
fun GlassEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassIconBubble(
                icon = icon,
                size = 54.dp,
                iconSize = 26.dp,
                isHighlight = true
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = LiquidGlass.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LiquidGlass.TextSecondary,
                    fontSize = 13.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                GlassButton(
                    onClick = onAction,
                    text = actionText,
                    style = GlassButtonStyle.PRIMARY
                )
            }
        }
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
 * Observes real-time device battery level and returns true ONLY when battery is below 20%.
 * Ensures animations always run smoothly unless battery charge drops under 20%.
 */
@Composable
private fun rememberIsBatteryBelow20Percent(context: Context): Boolean {
    fun checkBatteryBelow20(intent: Intent?): Boolean {
        return try {
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val pct = (level * 100) / scale
                    return pct in 1..19
                }
            }
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            capacity in 1..19
        } catch (_: Exception) {
            false
        }
    }

    var isBelow20 by remember {
        val stickyIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        mutableStateOf(checkBatteryBelow20(stickyIntent))
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isBelow20 = checkBatteryBelow20(intent)
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return isBelow20
}

/**
 * Shared animated canvas for rendering the living wallpaper with physics-based particles,
 * falling petals, autumn leaves, cyber rain, aurora waves, or ocean bubbles.
 *
 * - Runs continuously and smoothly at native VSYNC frame rate (never stutters, jumps, or gets stuck).
 * - Only pauses animation when device battery is strictly below 20%, displaying a beautifully arranged
 *   static composition of leaves/petals over the scenic background.
 */
@Composable
fun AnimatedThemeCanvas(
    theme: BackgroundThemeType,
    modifier: Modifier = Modifier,
    isThumbnail: Boolean = false
) {
    val context = LocalContext.current
    val isBatteryLow = rememberIsBatteryBelow20Percent(context)

    // Monotonic continuous time in seconds (curated static arrangement at 4.2s when battery < 20%)
    var continuousTime by remember { mutableFloatStateOf(4.2f) }

    LaunchedEffect(isBatteryLow) {
        if (!isBatteryLow) {
            var lastFrameNanos = 0L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos != 0L) {
                        val deltaSec = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f)
                            .coerceIn(0f, 0.05f)
                        continuousTime += deltaSec
                        // Keep float precision high over many hours while avoiding visible wrap
                        if (continuousTime > 86400f) {
                            continuousTime -= 86400f
                        }
                    }
                    lastFrameNanos = frameTimeNanos
                }
            }
        } else {
            // Beautifully arranged static snapshot when battery < 20%
            continuousTime = 4.2f
        }
    }

    val driftPhase = (continuousTime * 0.3927f) % (2f * Math.PI.toFloat())
    val pulseGlow = if (isBatteryLow) {
        1.0f
    } else {
        1.01f + 0.13f * sin((continuousTime * 0.52f).toDouble()).toFloat()
    }
    val particleTwinkle = if (isBatteryLow) {
        0.95f
    } else {
        0.70f + 0.30f * sin((continuousTime * 0.90f).toDouble()).toFloat()
    }

    // Reusable Path instance to prevent per-frame object allocations and GC stutters
    val reusablePath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.baseGradient.first())
    ) {
        // 1. High-Res Scenic Wallpaper Image (Crisp & Vivid)
        Image(
            painter = painterResource(id = theme.drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isThumbnail) 0.95f else 0.90f,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Subtle Atmospheric Vignette Scrim (Underneath particles so leaves/petals shine brightly)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isThumbnail) {
                            listOf(
                                Color(0x22040810),
                                Color(0x11040810),
                                Color(0x55040810)
                            )
                        } else {
                            listOf(
                                Color(0x4D050912),
                                Color(0x33070D19),
                                Color(0x400A1122),
                                Color(0x66050912)
                            )
                        }
                    )
                )
        )

        // 3. Dynamic Animated Canvas Overlay (Luminous Orbs + Physics Petals/Leaves/Rain/Bubbles)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val scaleFactor = if (isThumbnail) (w / 360f).coerceIn(0.35f, 1f) else 1f

                    // Undulating ambient lighting glows
                    val orb1X = w * (0.82f + 0.10f * cos(driftPhase.toDouble()).toFloat())
                    val orb1Y = h * (0.14f + 0.08f * sin(driftPhase.toDouble()).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = theme.orb1Colors,
                            center = Offset(orb1X, orb1Y),
                            radius = w * 0.75f * pulseGlow
                        )
                    )

                    val orb2X = w * (0.15f + 0.10f * sin(driftPhase.toDouble()).toFloat())
                    val orb2Y = h * (0.55f + 0.09f * cos(driftPhase.toDouble()).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = theme.orb2Colors,
                            center = Offset(orb2X, orb2Y),
                            radius = w * 0.80f * (2.02f - pulseGlow)
                        )
                    )

                    val orb3X = w * (0.78f + 0.08f * sin((driftPhase + 1.8f).toDouble()).toFloat())
                    val orb3Y = h * (0.88f + 0.06f * cos((driftPhase + 1.8f).toDouble()).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = theme.orb3Colors,
                            center = Offset(orb3X, orb3Y),
                            radius = w * 0.70f * pulseGlow
                        )
                    )

                    // Theme-specific live particle physics (or curated static arrangement when battery < 20%)
                    when (theme.animationType) {
                        ThemeAnimationType.SAKURA_PETALS -> {
                            val petalsToRender = if (isThumbnail) staticPetals.take(14) else staticPetals
                            petalsToRender.forEach { petal ->
                                val t = continuousTime
                                val normalizedY = (petal.relY + t * petal.fallSpeed) % 1.0f
                                val py = normalizedY * h

                                val swayPhase = t * petal.swaySpeed + petal.phase
                                val windGust = sin((t * 0.4f).toDouble()).toFloat() * (18f * scaleFactor)
                                val px = (petal.relX * w + sin(swayPhase.toDouble()).toFloat() * (petal.swayDistance * scaleFactor) + windGust)
                                    .coerceIn(-20f, w + 20f)

                                val rotationDeg = petal.rotationBase + t * petal.rotationSpeed
                                val flipAngle = t * petal.flipSpeed + petal.phase
                                val scaleX = cos(flipAngle.toDouble()).toFloat().coerceIn(-1f, 1f)

                                val pw = petal.width.dp.toPx() * scaleFactor
                                val ph = petal.height.dp.toPx() * scaleFactor

                                // Soft glowing aura behind each cherry blossom petal
                                drawCircle(
                                    color = petal.petalColor.copy(alpha = 0.22f),
                                    radius = ph * 0.7f,
                                    center = Offset(px, py)
                                )

                                rotate(degrees = rotationDeg, pivot = Offset(px, py)) {
                                    scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(px, py)) {
                                        reusablePath.reset()
                                        reusablePath.moveTo(px, py - ph * 0.5f)
                                        reusablePath.cubicTo(
                                            px - pw * 0.65f, py - ph * 0.35f,
                                            px - pw * 0.65f, py + ph * 0.25f,
                                            px, py + ph * 0.5f
                                        )
                                        reusablePath.cubicTo(
                                            px + pw * 0.65f, py + ph * 0.25f,
                                            px + pw * 0.65f, py - ph * 0.35f,
                                            px, py - ph * 0.5f
                                        )
                                        reusablePath.close()

                                        drawPath(
                                            path = reusablePath,
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    petal.edgeColor.copy(alpha = 0.92f),
                                                    petal.petalColor.copy(alpha = 0.88f),
                                                    Color.White.copy(alpha = 0.70f)
                                                ),
                                                center = Offset(px, py),
                                                radius = ph * 0.6f
                                            )
                                        )

                                        // Subtle petal center highlight vein
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.55f),
                                            start = Offset(px, py - ph * 0.30f),
                                            end = Offset(px, py + ph * 0.25f),
                                            strokeWidth = (0.8f * scaleFactor).dp.toPx()
                                        )
                                    }
                                }
                            }
                        }

                        ThemeAnimationType.MAPLE_LEAVES -> {
                            val leavesToRender = if (isThumbnail) staticMapleLeaves.take(10) else staticMapleLeaves
                            leavesToRender.forEach { leaf ->
                                val t = continuousTime
                                val normalizedY = (leaf.relY + t * leaf.fallSpeed) % 1.0f
                                val py = normalizedY * h

                                val swayPhase = t * leaf.swaySpeed + leaf.phase
                                val px = (leaf.relX * w + sin(swayPhase.toDouble()).toFloat() * (leaf.swayDistance * scaleFactor))
                                    .coerceIn(-20f, w + 20f)

                                val rotationDeg = leaf.phase * 50f + t * leaf.rotationSpeed
                                val s = leaf.size.dp.toPx() * scaleFactor

                                // Warm amber glow behind leaf
                                drawCircle(
                                    color = leaf.color.copy(alpha = 0.25f),
                                    radius = s * 0.75f,
                                    center = Offset(px, py)
                                )

                                rotate(degrees = rotationDeg, pivot = Offset(px, py)) {
                                    reusablePath.reset()
                                    reusablePath.moveTo(px, py - s * 0.6f)
                                    reusablePath.lineTo(px - s * 0.25f, py - s * 0.2f)
                                    reusablePath.lineTo(px - s * 0.55f, py - s * 0.3f)
                                    reusablePath.lineTo(px - s * 0.35f, py + s * 0.1f)
                                    reusablePath.lineTo(px - s * 0.45f, py + s * 0.35f)
                                    reusablePath.lineTo(px, py + s * 0.55f)
                                    reusablePath.lineTo(px + s * 0.45f, py + s * 0.35f)
                                    reusablePath.lineTo(px + s * 0.35f, py + s * 0.1f)
                                    reusablePath.lineTo(px + s * 0.55f, py - s * 0.3f)
                                    reusablePath.lineTo(px + s * 0.25f, py - s * 0.2f)
                                    reusablePath.close()

                                    drawPath(
                                        path = reusablePath,
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFE082),
                                                leaf.color.copy(alpha = 0.92f),
                                                Color(0xFF8D1C00).copy(alpha = 0.85f)
                                            ),
                                            center = Offset(px, py),
                                            radius = s * 0.6f
                                        )
                                    )

                                    // Golden leaf center vein
                                    drawLine(
                                        color = Color(0xFFFFF59D).copy(alpha = 0.65f),
                                        start = Offset(px, py - s * 0.45f),
                                        end = Offset(px, py + s * 0.45f),
                                        strokeWidth = (1.0f * scaleFactor).dp.toPx()
                                    )
                                }
                            }
                        }

                        ThemeAnimationType.CYBER_RAIN -> {
                            val rainToRender = if (isThumbnail) staticCyberRain.take(12) else staticCyberRain
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
                                            rain.color.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.90f)
                                        ),
                                        startY = py - streakLen,
                                        endY = py
                                    ),
                                    start = Offset(px - streakLen * 0.2f, py - streakLen),
                                    end = Offset(px, py),
                                    strokeWidth = streakWidth
                                )

                                // Glowing raindrop tip
                                drawCircle(
                                    color = rain.color.copy(alpha = 0.55f),
                                    radius = streakWidth * 2.2f,
                                    center = Offset(px, py)
                                )
                            }
                        }

                        ThemeAnimationType.AURORA_STARDUST -> {
                            // Sweeping Aurora Borealis wave ribbon
                            val auroraY = h * (0.22f + 0.08f * sin(driftPhase.toDouble()).toFloat())
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x3800E676),
                                        Color(0x2800E5FF),
                                        Color(0x189D4EDD),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, auroraY - 60f),
                                    end = Offset(w, auroraY + 180f)
                                ),
                                size = Size(w, h * 0.55f)
                            )

                            val particles = if (isThumbnail) staticParticles.take(12) else staticParticles
                            particles.forEach { p ->
                                val particlePhase = driftPhase * p.speed + p.phaseOffset
                                val offsetY = 18f * sin(particlePhase.toDouble()).toFloat()
                                val offsetX = 10f * cos(particlePhase.toDouble()).toFloat()
                                val px = (p.relX * w + offsetX).coerceIn(0f, w)
                                val py = (p.relY * h + offsetY).coerceIn(0f, h)

                                val alphaMod = ((sin(particlePhase.toDouble()).toFloat() + 1f) / 2f)
                                val alpha = (0.35f + 0.65f * alphaMod) * particleTwinkle

                                drawCircle(
                                    color = theme.particleColor.copy(alpha = alpha * 0.45f),
                                    radius = p.baseRadius * 3.0f * scaleFactor,
                                    center = Offset(px, py)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha * 0.95f),
                                    radius = p.baseRadius * 1.1f * scaleFactor,
                                    center = Offset(px, py)
                                )
                            }
                        }

                        ThemeAnimationType.OCEAN_BUBBLES -> {
                            val bubblesToRender = if (isThumbnail) staticOceanBubbles.take(10) else staticOceanBubbles
                            bubblesToRender.forEach { bubble ->
                                val t = continuousTime
                                // Rise upwards from bottom (1.0 -> 0.0)
                                val normalizedY = 1.0f - ((1.0f - bubble.relY + t * bubble.riseSpeed) % 1.0f)
                                val py = normalizedY * h

                                val wobblePhase = t * bubble.wobbleSpeed + bubble.phase
                                val px = (bubble.relX * w + sin(wobblePhase.toDouble()).toFloat() * (bubble.wobbleDistance * scaleFactor))
                                    .coerceIn(0f, w)

                                val r = bubble.radius.dp.toPx() * scaleFactor

                                // Outer bioluminescent halo
                                drawCircle(
                                    color = bubble.color.copy(alpha = 0.25f),
                                    radius = r * 1.4f,
                                    center = Offset(px, py)
                                )
                                // Bubble rim
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.65f),
                                    radius = r,
                                    center = Offset(px, py),
                                    style = Stroke(width = 1.2.dp.toPx() * scaleFactor)
                                )
                                // Specular bubble highlight dot
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.9f),
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
