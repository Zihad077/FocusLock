package com.example.presentation.blocking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.liquidGlass

@Composable
fun BlockScreen(
    appName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    emergencyRemaining: Int = 1,
    onWaitClick: () -> Unit,
    onChallengeClick: () -> Unit,
    onEmergencyUnlockClick: () -> Unit
) {
    val primaryCyan = Color(0xFF24DFEC)
    val accentRed = Color(0xFFFF5252)
    val isRestricted = limitMinutes == 0
    val activeColor = if (isRestricted) accentRed else primaryCyan
    val scrollState = rememberScrollState()

    LiquidBackground {
        // High-contrast translucent dark scrim to ensure full legibility over any animated wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x9508111D),
                            Color(0xC00B1626),
                            Color(0xE0091322)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag: Mindful Interception Status Badge
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x35142436))
                        .border(1.dp, activeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(activeColor)
                        )
                        Text(
                            text = if (isRestricted) "STRICT LOCKDOWN ACTIVE" else "FOCUS INTERCEPTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                fontSize = 11.sp
                            ),
                            color = activeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Liquid Glass Card Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            isElevated = true,
                            isHighlight = isRestricted
                        )
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glowing Frosted Orb with Lock / Shield Icon
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            activeColor.copy(alpha = 0.30f),
                                            activeColor.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(1.5.dp, activeColor.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRestricted) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = "Lock Status",
                                tint = activeColor,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Headline
                        Text(
                            text = if (isRestricted) {
                                stringResource(R.string.app_restricted)
                            } else {
                                stringResource(R.string.times_up)
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // App Name Badge Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x303E4C5E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.20f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = primaryCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Motivating Description text
                        Text(
                            text = if (isRestricted) {
                                stringResource(R.string.restricted_reason, appName)
                            } else {
                                stringResource(R.string.quota_reached_reason, appName)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Usage Telemetry Sub-Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0x35101E2E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isRestricted) accentRed.copy(alpha = 0.35f) else primaryCyan.copy(alpha = 0.30f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.usage_today).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isRestricted) {
                                        stringResource(R.string.strict_block_0m)
                                    } else {
                                        stringResource(R.string.used_format, usedMinutes, limitMinutes)
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    ),
                                    color = activeColor
                                )

                                if (!isRestricted && limitMinutes > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val progress = (usedMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
                                    val animProgress by animateFloatAsState(
                                        targetValue = progress,
                                        animationSpec = tween(500),
                                        label = "block_progress"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animProgress)
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(activeColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Full-Width Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Primary CTA: Return to Home
                    BlockFullWidthButton(
                        text = stringResource(R.string.return_to_home),
                        icon = Icons.Default.Home,
                        isPrimary = true,
                        onClick = onWaitClick
                    )

                    // 2. Secondary CTA: Complete Challenge to Unlock
                    BlockFullWidthButton(
                        text = stringResource(R.string.complete_challenge_to_unlock),
                        icon = Icons.Default.Psychology,
                        isPrimary = false,
                        onClick = onChallengeClick
                    )

                    // 3. Emergency Bypass CTA
                    if (emergencyRemaining > 0) {
                        BlockEmergencyButton(
                            text = stringResource(R.string.emergency_bypass_left, emergencyRemaining),
                            enabled = true,
                            onClick = onEmergencyUnlockClick
                        )
                    } else {
                        BlockEmergencyButton(
                            text = stringResource(R.string.no_emergency_passes_left),
                            enabled = false,
                            onClick = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun BlockFullWidthButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        label = "btn_scale"
    )

    val primaryBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00E5FF), Color(0xFF24DFEC))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(26.dp))
            .then(
                if (isPrimary) {
                    Modifier.background(primaryBrush)
                } else {
                    Modifier
                        .background(Color(0x353E4C5E))
                        .border(1.2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) Color(0xFF061820) else Color.White,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = if (isPrimary) Color(0xFF061820) else Color.White
            )
        }
    }
}

@Composable
private fun BlockEmergencyButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        label = "emergency_btn_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (enabled) Color(0x30551822) else Color(0x18301518)
            )
            .border(
                1.dp,
                if (enabled) Color(0x70EF4444) else Color(0x25EF4444),
                RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (enabled) Color(0xFFFF6B6B) else Color(0x60FF6B6B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                ),
                color = if (enabled) Color(0xFFFF6B6B) else Color(0x60FF6B6B)
            )
        }
    }
}
