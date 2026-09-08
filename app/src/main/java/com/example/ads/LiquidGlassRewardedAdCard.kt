package com.example.ads

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.liquidGlass

/**
 * Optional Rewarded Ad Card for XP and Focus Boosts.
 *
 * Rules:
 * - Only plays when user explicitly taps "Watch Video".
 * - Grants configured XP only after full completion of the ad.
 * - Displays in the Goals & Achievements area.
 * - Hidden automatically for Premium users.
 */
@Composable
fun LiquidGlassRewardedAdCard(
    isPremium: Boolean,
    onRewardEarned: (amount: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val context = LocalContext.current
    val activity = context as? Activity
    val isDark = isSystemInDarkTheme()
    val isAdLoaded by AdsManager.isRewardedAdLoaded.collectAsState()

    val goldColor = if (isDark) Color(0xFFFFD54F) else Color(0xFFFFA000)
    val cyanAccent = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    LaunchedEffect(Unit) {
        AdsManager.preloadRewardedAd(context)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                isElevated = false,
                borderWidth = 0.8.dp
            )
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                goldColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                                cyanAccent.copy(alpha = if (isDark) 0.3f else 0.2f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bonus XP Boost",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(cyanAccent.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+100 XP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = cyanAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Watch a short sponsored video to boost your discipline tier.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Watch Action Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6),
                                if (isDark) Color(0xFF0091EA) else Color(0xFF005DB2)
                            )
                        )
                    )
                    .clickable {
                        if (activity != null) {
                            AdsManager.showRewardedAd(
                                activity = activity,
                                onUserEarnedReward = { amount, _ ->
                                    val rewardXP = if (amount > 0) amount else 100
                                    onRewardEarned(rewardXP)
                                    Toast.makeText(
                                        context,
                                        "Earned +$rewardXP Bonus XP!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onAdUnavailableOrFailed = {
                                    Toast.makeText(
                                        context,
                                        "Rewarded video is loading, please retry shortly.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
