package com.example.presentation.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdsterraSocialBar
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.database.UserSettings
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val limits by viewModel.limitsWithUsage.collectAsStateWithLifecycle()
    val stats by viewModel.statsSummary.collectAsStateWithLifecycle()

    var isAccessibilityActive by remember {
        mutableStateOf(PermissionHelper.hasAccessibilityPermission(context))
    }
    var isOverlayActive by remember {
        mutableStateOf(PermissionHelper.hasOverlayPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = PermissionHelper.hasAccessibilityPermission(context)
                isOverlayActive = PermissionHelper.hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
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
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }

        item {
            StatsGridSection(settings = settings, stats = stats, limits = limits)
        }

        // Sponsored Native Glass Card (Zero ads for premium)
        item {
            LiquidGlassNativeAdCard(
                isPremium = settings?.isPremium ?: false
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Limits",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${limits.size} monitored",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSystemInDarkTheme()) Color(0xFF00E5FF) else Color(0xFF0077D6)
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

        // 320x50 Banner in lower content area (Zero ads for premium)
        item {
            LiquidGlassAdaptiveBanner(
                isPremium = settings?.isPremium ?: false
            )
        }

        // Social Bar format (cooldown protected, zero ads for premium)
        item {
            AdsterraSocialBar(
                isPremium = settings?.isPremium ?: false,
                isFocusActive = settings?.isFocusModeActive ?: false
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "FocusLock",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "DIGITAL EQUILIBRIUM",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                ),
                color = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
                        } else {
                            listOf(Color(0xFF0077D6), Color(0xFF00B0FF))
                        }
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "FL",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}

@Composable
private fun StatsGridSection(settings: UserSettings?, stats: StatsSummary, limits: List<AppLimitUIModel>) {
    val isDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Liquid Glass Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    isElevated = true,
                    isHighlight = true
                )
        ) {
            // Subtle ambient backdrop gradient inside the glass container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0x3500E5FF),
                                    Color(0x152979FF),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0x3064B5F6),
                                    Color(0x100077D6),
                                    Color.Transparent
                                )
                            },
                            center = Offset(Float.POSITIVE_INFINITY, 0f),
                            radius = 600f
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Focus Score",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${settings?.focusScore ?: 0}",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isDark) Color(0xFF00E5FF) else Color(0xFF005DB2)
                            )
                        }

                        // Glass Badge with Star Icon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isDark) Color(0x3000E5FF) else Color(0x200077D6)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color(0x6000E5FF) else Color(0x500077D6),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    isElevated = false,
                                    borderWidth = 0.5.dp
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TODAY'S USAGE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${stats.totalUsedMinutes / 60}h ${stats.totalUsedMinutes % 60}m",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    isElevated = false,
                                    borderWidth = 0.5.dp
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TIME RECLAIMED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${stats.totalSavedMinutes}m",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Secondary Row (Blocked & Streak)
        val blockedCount = limits.count { it.remainingMinutes <= 0 }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCardSecondary(
                title = "Blocked Apps",
                value = "$blockedCount restricted",
                isBlocked = true,
                modifier = Modifier.weight(1f)
            )
            StatCardSecondary(
                title = "Current Streak",
                value = "${settings?.currentStreak ?: 0} Days",
                isBlocked = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCardSecondary(
    title: String,
    value: String,
    isBlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = if (isBlocked) {
        if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
    } else {
        if (isDark) Color(0xFFFFAB00) else Color(0xFFF57C00)
    }

    Box(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(22.dp), isElevated = false)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.25f else 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape)
                .padding(10.dp)
            ) {
                Icon(
                    imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun EmptyLimitsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No limits set yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure app restrictions in the Apps tab to start building mindful habits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AppLimitCard(limit: AppLimitUIModel) {
    val isDark = isSystemInDarkTheme()
    val isExceeded = limit.remainingMinutes <= 0
    val progress = limit.progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    val progressColor = if (isExceeded) {
        if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
    } else {
        if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                isElevated = false,
                isHighlight = isExceeded
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glass App Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0077D6),
                                    Color(0xFF00B4D8)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = limit.appName.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${limit.usedMinutes} / ${limit.dailyLimitMinutes} min used",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }

                // Status chip
                val statusText = if (isExceeded) "Blocked" else "${limit.remainingMinutes}m left"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(progressColor.copy(alpha = if (isDark) 0.25f else 0.15f))
                        .border(1.dp, progressColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = progressColor
                    )
                }
            }

            // Glass Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0x30FFFFFF) else Color(0x20000000)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    progressColor.copy(alpha = 0.8f),
                                    progressColor
                                )
                            )
                        )
                )
            }
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
    val isDark = isSystemInDarkTheme()
    val isFullyActive = isAccessibilityActive && isOverlayActive

    val bannerColor = if (isFullyActive) {
        if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)
    } else {
        if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                isHighlight = !isFullyActive
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bannerColor.copy(alpha = if (isDark) 0.25f else 0.15f))
                    .border(1.dp, bannerColor.copy(alpha = 0.4f), CircleShape)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = if (isFullyActive) Icons.Default.Shield else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = bannerColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFullyActive) "Active Protection" else "Protection Limited",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFullyActive) {
                        "Accessibility monitor and screen interception running smoothly."
                    } else if (!isAccessibilityActive) {
                        "Accessibility permission needed for instant app interception."
                    } else {
                        "Overlay permission needed to display mindful lock screen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            if (!isFullyActive) {
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        if (!isAccessibilityActive) onEnableAccessibility()
                        else onEnableOverlay()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bannerColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Enable",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
