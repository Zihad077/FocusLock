package com.example.presentation.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ads.AdsterraSocialBar
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.database.UserSettings
import com.example.database.isPremiumActive
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalContext.current.applicationContext as Application)
    ),
    onNavigateToApps: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val limits by viewModel.limitsWithUsage.collectAsStateWithLifecycle()
    val stats by viewModel.statsSummary.collectAsStateWithLifecycle()

    var editingLimit by remember { mutableStateOf<AppLimitUIModel?>(null) }

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
                viewModel.syncUsageData()
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
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HeaderSection(settings = settings)
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
                isPremium = settings?.isPremiumActive ?: false
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.active_limits),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${limits.size} ${stringResource(R.string.monitored_suffix)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF00E5FF)
                )
            }
        }

        if (limits.isEmpty()) {
            item {
                EmptyLimitsCard(onNavigateToApps = onNavigateToApps)
            }
        } else {
            items(limits) { limit ->
                AppLimitCard(
                    limit = limit,
                    onEditClick = { editingLimit = limit },
                    onDeleteClick = { viewModel.removeLimit(limit.packageName) }
                )
            }
        }

        // 320x50 Banner in lower content area (Zero ads for premium)
        item {
            LiquidGlassAdaptiveBanner(
                isPremium = settings?.isPremiumActive ?: false
            )
        }

        // Social Bar format (cooldown protected, zero ads for premium)
        item {
            AdsterraSocialBar(
                isPremium = settings?.isPremiumActive ?: false,
                isFocusActive = settings?.isFocusModeActive ?: false
            )
        }
    }

    editingLimit?.let { limit ->
        EditLimitDialog(
            limit = limit,
            onDismiss = { editingLimit = null },
            onConfirm = { minutes ->
                viewModel.updateLimit(limit.packageName, limit.appName, minutes)
                editingLimit = null
            }
        )
    }
}

@Composable
private fun HeaderSection(settings: UserSettings?) {
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
                text = stringResource(R.string.digital_equilibrium),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                ),
                color = Color(0xFF00E5FF)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Streak Flame Glass Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x28FFAB00))
                    .border(1.dp, Color(0x55FFAB00), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFFAB00),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${settings?.currentStreak ?: 0}d",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFAB00)
                    )
                }
            }

            // User Level Glass Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "L${settings?.level ?: 1}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

@Composable
private fun StatsGridSection(settings: UserSettings?, stats: StatsSummary, limits: List<AppLimitUIModel>) {
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
                            colors = listOf(
                                Color(0x3500E5FF),
                                Color(0x152979FF),
                                Color.Transparent
                            ),
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
                                text = stringResource(R.string.focus_score),
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
                                color = Color(0xFF00E5FF)
                            )
                        }

                        // Glass Badge with Star Icon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x3000E5FF))
                                .border(
                                    width = 1.dp,
                                    color = Color(0x6000E5FF),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
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
                                    text = stringResource(R.string.todays_usage),
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
                                    text = stringResource(R.string.time_reclaimed),
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
                                    color = Color(0xFF00E5FF)
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
                title = stringResource(R.string.blocked_apps),
                value = "$blockedCount ${stringResource(R.string.monitored_suffix)}",
                isBlocked = true,
                modifier = Modifier.weight(1f)
            )
            StatCardSecondary(
                title = stringResource(R.string.current_streak),
                value = "${settings?.currentStreak ?: 0} ${stringResource(R.string.days_suffix)}",
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
    val accentColor = if (isBlocked) Color(0xFFFF5252) else Color(0xFFFFAB00)

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
                    .background(accentColor.copy(alpha = 0.25f))
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
private fun EmptyLimitsCard(onNavigateToApps: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.no_limits_set),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_limits_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            if (onNavigateToApps != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToApps,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.set_app_limits),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLimitCard(
    limit: AppLimitUIModel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isExceeded = limit.remainingMinutes <= 0
    val progress = limit.progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    val progressColor = if (isExceeded) Color(0xFFFF5252) else Color(0xFF00E5FF)

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
                com.example.presentation.common.RealAppIcon(
                    packageName = limit.packageName,
                    appName = limit.appName,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.used_format, limit.usedMinutes, limit.dailyLimitMinutes),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }

                // Action buttons (Edit & Delete)
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Limit",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Limit",
                        tint = Color(0xFFFF5252).copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Status chip
                val statusText = if (isExceeded) {
                    stringResource(R.string.blocked_status)
                } else {
                    stringResource(R.string.min_left, limit.remainingMinutes)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(progressColor.copy(alpha = 0.25f))
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
                    .background(Color(0x30FFFFFF))
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
private fun EditLimitDialog(
    limit: AppLimitUIModel,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(limit.dailyLimitMinutes.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true),
        containerColor = Color.Transparent,
        title = {
            Text(
                text = stringResource(R.string.adjust_limit_title, limit.appName),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.daily_allowance, sliderValue.toInt()),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF00E5FF)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 5f..240f,
                    steps = 46,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.min_5),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.hours_4),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(sliderValue.toInt()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    )
}

@Composable
fun ProtectionStatusBanner(
    isAccessibilityActive: Boolean,
    isOverlayActive: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    val isFullyActive = isAccessibilityActive && isOverlayActive
    val bannerColor = if (isFullyActive) Color(0xFF00E5FF) else Color(0xFFFF5252)

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
                    .background(bannerColor.copy(alpha = 0.25f))
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
                    text = if (isFullyActive) {
                        stringResource(R.string.active_protection)
                    } else {
                        stringResource(R.string.protection_limited)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFullyActive) {
                        stringResource(R.string.protection_active_desc)
                    } else if (!isAccessibilityActive) {
                        stringResource(R.string.accessibility_needed_desc)
                    } else {
                        stringResource(R.string.overlay_needed_desc)
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
                        text = stringResource(R.string.enable_btn),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
