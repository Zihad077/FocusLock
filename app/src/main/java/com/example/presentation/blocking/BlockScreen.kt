package com.example.presentation.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.SleekPrimaryDark
import com.example.ui.theme.SleekError

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
    val primaryCyan = SleekPrimaryDark
    val accentRed = SleekError

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Glowing Glass Orb with Lock/Shield
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        spotColor = if (limitMinutes == 0) accentRed else primaryCyan
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                (if (limitMinutes == 0) accentRed else primaryCyan).copy(alpha = 0.35f),
                                Color(0x20000000)
                            ),
                            center = Offset(200f, 200f),
                            radius = 300f
                        )
                    )
                    .border(
                        2.dp,
                        Brush.sweepGradient(
                            listOf(
                                Color.White.copy(alpha = 0.8f),
                                if (limitMinutes == 0) accentRed else primaryCyan,
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.8f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (limitMinutes == 0) Icons.Default.Shield else Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = if (limitMinutes == 0) accentRed else primaryCyan,
                    modifier = Modifier.size(54.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = if (limitMinutes == 0) {
                    stringResource(R.string.app_restricted)
                } else {
                    stringResource(R.string.times_up)
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (limitMinutes == 0) {
                    stringResource(R.string.restricted_reason, appName)
                } else {
                    stringResource(R.string.quota_reached_reason, appName)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Usage Telemetry Glass Badge
            Box(
                modifier = Modifier
                    .liquidGlass(shape = RoundedCornerShape(20.dp), isHighlight = limitMinutes == 0)
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.usage_today),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (limitMinutes == 0) {
                            stringResource(R.string.strict_block_0m)
                        } else {
                            stringResource(R.string.used_format, usedMinutes, limitMinutes)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (limitMinutes == 0) accentRed else primaryCyan
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons with Glass Styling
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Return Home Liquid Primary Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = primaryCyan)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(SleekPrimaryDark, SleekPrimaryDark.copy(alpha = 0.8f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onWaitClick,
                        modifier = Modifier.fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.return_to_home),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
                
                // Challenge Glass Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .liquidGlass(shape = RoundedCornerShape(18.dp), isElevated = true),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onChallengeClick,
                        modifier = Modifier.fillMaxSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.complete_challenge_to_unlock),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                // Emergency Unlock Glass Button
                OutlinedButton(
                    onClick = onEmergencyUnlockClick,
                    enabled = emergencyRemaining > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentRed
                    )
                ) {
                    Text(
                        text = if (emergencyRemaining > 0) {
                            stringResource(R.string.emergency_bypass_left, emergencyRemaining)
                        } else {
                            stringResource(R.string.no_emergency_passes_left)
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
