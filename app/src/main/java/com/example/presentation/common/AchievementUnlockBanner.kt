package com.example.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Achievement
import com.example.ui.theme.liquidGlass
import com.example.util.AchievementProgress
import com.example.util.StreakAndAchievementManager
import com.example.util.StreakMilestoneInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Floating celebration banner that animates in when a user unlocks a badge.
 */
@Composable
fun AchievementUnlockOverlay(
    achievement: Achievement?,
    onDismiss: () -> Unit,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(achievement?.id) {
        if (achievement != null) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
            delay(5000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = achievement != null,
        enter = slideInVertically(
            initialOffsetY = { -it - 40 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + scaleIn(
            initialScale = 0.88f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 40 },
            animationSpec = tween(250)
        ) + scaleOut(targetScale = 0.92f) + fadeOut(tween(200)),
        modifier = modifier
    ) {
        achievement?.let { ach ->
            val infiniteTransition = rememberInfiniteTransition(label = "badge_celebration")
            val rayAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ray_angle"
            )
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            val goldColor = Color(0xFFFFCA28)
            val cyanColor = Color(0xFF00E5FF)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .shadow(18.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFA14263D),
                                Color(0xFA1A213E),
                                Color(0xFA112236)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(goldColor, cyanColor, Color(0xFFA855F7))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Animated Badge Emblem with Sparkle Rays
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxR = size.minDimension / 2f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        goldColor.copy(alpha = 0.38f),
                                        cyanColor.copy(alpha = 0.14f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = maxR
                                ),
                                radius = maxR
                            )
                            for (i in 0 until 8) {
                                val rad = Math.toRadians((rayAngle + i * 45f).toDouble())
                                val r1 = maxR * 0.62f
                                val r2 = maxR * 0.95f
                                drawLine(
                                    color = if (i % 2 == 0) goldColor.copy(alpha = 0.7f) else cyanColor.copy(alpha = 0.6f),
                                    start = Offset(
                                        center.x + (r1 * cos(rad)).toFloat(),
                                        center.y + (r1 * sin(rad)).toFloat()
                                    ),
                                    end = Offset(
                                        center.x + (r2 * cos(rad)).toFloat(),
                                        center.y + (r2 * sin(rad)).toFloat()
                                    ),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .clip(CircleShape)
                                .background(Color(0x33FFCA28))
                                .border(1.5.dp, goldColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getBadgeIcon(ach.id),
                                contentDescription = null,
                                tint = goldColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "BADGE UNLOCKED ✨",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.1.sp,
                                    fontSize = 10.sp
                                ),
                                color = goldColor
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cyanColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+${ach.xpReward} XP",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = cyanColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ach.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${ach.description} • You're on a roll!",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onShareClick != null) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                onShareClick()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(cyanColor.copy(alpha = 0.2f))
                                .border(1.dp, cyanColor.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Achievement",
                                tint = cyanColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small animated celebration burst icon for completed focus sessions.
 */
@Composable
fun SessionCompleteBurstIcon(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "session_burst")
    val burstProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "burst_progress"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    val cyan = Color(0xFF00E5FF)
    val gold = Color(0xFFFFCA28)

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            for (i in 0 until 10) {
                val angle = Math.toRadians((i * 36.0) + (burstProgress * 25.0))
                val dist = maxRadius * (0.55f + 0.42f * burstProgress)
                val dotAlpha = (1f - burstProgress).coerceIn(0f, 1f)
                drawCircle(
                    color = if (i % 2 == 0) cyan.copy(alpha = dotAlpha) else gold.copy(alpha = dotAlpha),
                    radius = 3.2.dp.toPx() * (1f - 0.4f * burstProgress),
                    center = Offset(
                        center.x + (dist * cos(angle)).toFloat(),
                        center.y + (dist * sin(angle)).toFloat()
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(cyan.copy(alpha = 0.28f), gold.copy(alpha = 0.22f))
                    )
                )
                .border(1.5.dp, cyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Session Completed",
                tint = gold,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Rich Focus Streak, 7-Day Activity & Next Milestone Progress Card.
 */
@Composable
fun FocusStreakMilestonesCard(
    milestoneInfo: StreakMilestoneInfo,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fireColor = Color(0xFFFF9100)
    val cyanColor = Color(0xFF00E5FF)
    val goldColor = Color(0xFFFFCA28)

    val animatedProgress by animateFloatAsState(
        targetValue = milestoneInfo.progressToMilestone,
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "milestone_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "flame_pulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(26.dp),
                isElevated = true,
                isHighlight = milestoneInfo.currentStreak > 0
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Top Row: Gen Z Status Headline + Streak Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(fireColor.copy(alpha = 0.18f))
                                .border(1.dp, fireColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "FOCUS STREAK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    fontSize = 9.5.sp
                                ),
                                color = fireColor
                            )
                        }
                        Text(
                            text = "Best: ${milestoneInfo.bestStreak}d 🏆",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = goldColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = milestoneInfo.statusHeadline,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = milestoneInfo.statusSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = Color(0xFFB0C0D4)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Glowing Streak Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x28FF9100))
                        .border(1.2.dp, fireColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak Flame",
                        tint = fireColor,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = flameScale
                                scaleY = flameScale
                            }
                    )
                    Text(
                        text = "${milestoneInfo.currentStreak}d",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }
            }

            // 7-Day Activity Tracker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x5508101D))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                milestoneInfo.last7Days.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 10.5.sp
                            ),
                            color = if (day.isToday) cyanColor else Color(0xFF94A3B8)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isCompleted -> Brush.linearGradient(
                                            listOf(fireColor, Color(0xFFFFCA28))
                                        )
                                        day.isToday -> Brush.linearGradient(
                                            listOf(cyanColor.copy(alpha = 0.25f), cyanColor.copy(alpha = 0.15f))
                                        )
                                        else -> Brush.linearGradient(
                                            listOf(Color(0xFF152236), Color(0xFF152236))
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        day.isCompleted -> Color(0xFFFFCA28)
                                        day.isToday -> cyanColor
                                        else -> Color.White.copy(alpha = 0.14f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color(0xFF0A121E),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (day.isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(cyanColor)
                                )
                            }
                        }
                    }
                }
            }

            // Milestone Progress Bar (3d -> 7d -> 14d -> 30d)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEXT MILESTONE: ${milestoneInfo.nextMilestoneDays}-DAY STREAK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.5.sp
                        ),
                        color = Color(0xFFB0C0D4)
                    )
                    Text(
                        text = "${milestoneInfo.currentStreak} / ${milestoneInfo.nextMilestoneDays} days",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        ),
                        color = cyanColor
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF091220))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceAtLeast(0.04f))
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(fireColor, goldColor, cyanColor)
                                )
                            )
                    )
                }

                // Milestone chips row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(3, 7, 14, 30).forEach { target ->
                        val reached = milestoneInfo.bestStreak >= target
                        Text(
                            text = if (reached) "✓ ${target}d" else "${target}d",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (reached) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 10.sp
                            ),
                            color = if (reached) goldColor else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section header for Achievements & Badges with prominent "Share Achievements" CTA button.
 */
@Composable
fun AchievementsSectionHeader(
    unlockedCount: Int,
    totalCount: Int,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyanColor = Color(0xFF00E5FF)
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "share_ach_btn_scale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Achievements & Badges",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = Color.White
            )
            Text(
                text = "$unlockedCount of $totalCount badges unlocked",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFFB0C0D4)
            )
        }

        Box(
            modifier = Modifier
                .testTag("share_achievements_button")
                .graphicsLayer {
                    scaleX = btnScale
                    scaleY = btnScale
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00E5FF), Color(0xFF24DFEC))
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    onShareClick()
                }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Achievements",
                    tint = Color(0xFF04151F),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Share Achievements",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp
                    ),
                    color = Color(0xFF04151F)
                )
            }
        }
    }
}

/**
 * Enhanced Achievement Badge Card with real progress bar, XP reward pill, and unlock date history.
 */
@Composable
fun EnhancedAchievementCard(
    progressItem: AchievementProgress,
    modifier: Modifier = Modifier
) {
    val achievement = progressItem.achievement
    val isUnlocked = achievement.isUnlocked
    val cyanColor = Color(0xFF00E5FF)
    val goldColor = Color(0xFFFFCA28)

    val animatedProgress by animateFloatAsState(
        targetValue = progressItem.progressFraction,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "ach_progress_${achievement.id}"
    )

    val formattedDate = remember(achievement.unlockedAt) {
        achievement.unlockedAt?.let { ts ->
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ts))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isHighlight = isUnlocked
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Badge Icon Orb
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) {
                                Brush.linearGradient(
                                    listOf(cyanColor.copy(alpha = 0.25f), goldColor.copy(alpha = 0.20f))
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(Color(0xFF162438), Color(0xFF101B2B))
                                )
                            }
                        )
                        .border(
                            width = 1.2.dp,
                            color = if (isUnlocked) cyanColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) getBadgeIcon(achievement.id) else Icons.Default.Lock,
                        contentDescription = achievement.title,
                        tint = if (isUnlocked) goldColor else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp
                            ),
                            color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.78f)
                        )

                        // XP Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isUnlocked) goldColor.copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    0.8.dp,
                                    if (isUnlocked) goldColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+${achievement.xpReward} XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.5.sp
                                ),
                                color = if (isUnlocked) goldColor else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = Color(0xFFB0C0D4)
                    )
                }
            }

            // Progress Bar + Unlock Status / History Row
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF08111E))
                ) {
                    if (animatedProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(CircleShape)
                                .background(
                                    if (isUnlocked) {
                                        Brush.horizontalGradient(listOf(cyanColor, goldColor))
                                    } else {
                                        Brush.horizontalGradient(listOf(cyanColor.copy(alpha = 0.7f), cyanColor))
                                    }
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUnlocked) {
                            if (formattedDate != null) "UNLOCKED • $formattedDate" else "UNLOCKED ✓"
                        } else {
                            "MILESTONE PROGRESS"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                            fontSize = 10.sp
                        ),
                        color = if (isUnlocked) cyanColor else Color(0xFF7E92AA)
                    )

                    Text(
                        text = progressItem.progressLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (isUnlocked) goldColor else Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

fun getBadgeIcon(achievementId: String): ImageVector {
    return when (achievementId) {
        "first_step" -> Icons.Default.Bolt
        "streak_3", "consistency", "streak_14", "streak_30" -> Icons.Default.LocalFireDepartment
        "deep_diver" -> Icons.Default.Timer
        "focus_5_sessions", "focus_300_min" -> Icons.Default.AutoAwesome
        "limit_setter" -> Icons.Default.Shield
        "goal_crusher" -> Icons.Default.Flag
        "iron_will" -> Icons.Default.Spa
        else -> Icons.Default.EmojiEvents
    }
}
