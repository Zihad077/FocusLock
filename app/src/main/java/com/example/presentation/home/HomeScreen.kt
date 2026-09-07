package com.example.presentation.home

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.UserSettings
import com.example.presentation.home.StatsSummary

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import com.example.util.PermissionHelper

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val limits by viewModel.limitsWithUsage.collectAsStateWithLifecycle()
    val stats by viewModel.statsSummary.collectAsStateWithLifecycle()

    val isAccessibilityActive = remember(context) {
        PermissionHelper.hasAccessibilityPermission(context)
    }
    val isOverlayActive = remember(context) {
        PermissionHelper.hasOverlayPermission(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HeaderSection()
        }

        item {
            ProtectionStatusBanner(
                isAccessibilityActive = isAccessibilityActive,
                isOverlayActive = isOverlayActive,
                onEnableAccessibility = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                onEnableOverlay = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }
        
        item {
            StatsGridSection(settings = settings, stats = stats, limits = limits)
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Limits",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (limits.isEmpty()) {
            item {
                EmptyLimitsCard()
            }
        } else {
            items(limits) { limit ->
                AppLimitCard(limit = limit)
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "FocusLock",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "GOOD MORNING", // Ideally user name here, just matching theme aesthetic
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun StatsGridSection(settings: UserSettings?, stats: StatsSummary, limits: List<AppLimitUIModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Card (Focus Score, Today's Time, Time Saved)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background decorative circle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 16.dp, y = 16.dp)
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f))
                )
                
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Focus Score",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${settings?.focusScore ?: 0}",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        // Icon Box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TODAY'S TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${stats.totalUsedMinutes / 60}h ${stats.totalUsedMinutes % 60}m",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TIME SAVED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "+${stats.totalSavedMinutes}m",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Secondary Row (Blocked & Streak)
        val blockedCount = limits.count { it.remainingMinutes <= 0 }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardSecondary(
                title = "Blocked",
                value = "$blockedCount Apps",
                modifier = Modifier.weight(1f)
            )
            StatCardSecondary(
                title = "Streak",
                value = "${settings?.currentStreak ?: 0} Days",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCardSecondary(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (title == "Blocked") Icons.Default.Block else Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EmptyLimitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No limits set.",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Go to the Apps tab to set up your first app limit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AppLimitCard(limit: AppLimitUIModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon Placeholder (We'll load real icon later)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFFFB8C00),
                                androidx.compose.ui.graphics.Color(0xFFE53935)
                            )
                        )),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = limit.appName.take(1).uppercase(),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${limit.usedMinutes} / ${limit.dailyLimitMinutes} min used",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val statusText = if (limit.remainingMinutes <= 0) "Blocked" else "${limit.remainingMinutes}m left"
                val statusColor = if (limit.remainingMinutes <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
            
            LinearProgressIndicator(
                progress = { limit.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (limit.remainingMinutes <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
fun ProtectionStatusBanner(
    isAccessibilityActive: Boolean,
    isOverlayActive: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    val isFullyActive = isAccessibilityActive && isOverlayActive

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFullyActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isFullyActive) Icons.Default.Shield else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (isFullyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFullyActive) "App Blocker Active" else "App Blocker Inactive",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isFullyActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFullyActive) {
                        "Accessibility service & monitor are running. Blocked apps will be restricted."
                    } else if (!isAccessibilityActive) {
                        "Accessibility permission is required to detect and block restricted apps."
                    } else {
                        "Overlay permission is required to display the mindful blocking screen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isFullyActive) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (!isAccessibilityActive) onEnableAccessibility()
                        else onEnableOverlay()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Fix Now", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

