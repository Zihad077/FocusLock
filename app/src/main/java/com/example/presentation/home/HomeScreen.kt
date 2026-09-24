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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.GlassButton
import com.example.ui.theme.GlassButtonStyle
import com.example.ui.theme.GlassEmptyState
import com.example.ui.theme.GlassIconBubble
import com.example.ui.theme.GlassSectionHeader
import com.example.ui.theme.GlassStatusBadge
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalContext.current.applicationContext as Application)
    ),
    onNavigateToApps: (() -> Unit)? = null,
    onNavigateToFocus: (() -> Unit)? = null,
    onNavigateToStats: (() -> Unit)? = null,
    onNavigateToPermissions: (() -> Unit)? = null
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
        contentPadding = PaddingValues(top = 16.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection(settings = settings)
        }

        // Primary Hero Action: Quick Focus Session
        item {
            PrimaryFocusActionCard(
                onStartFocus = { onNavigateToFocus?.invoke() }
            )
        }

        // 3-4 Core Metrics Dashboard
        item {
            CoreMetricsSection(
                settings = settings,
                stats = stats,
                limits = limits,
                onNavigateToStats = onNavigateToStats
            )
        }

        // Permission Banner (only if needed or compact)
        if (!isAccessibilityActive || !isOverlayActive) {
            item {
                ProtectionStatusBanner(
                    isAccessibilityActive = isAccessibilityActive,
                    isOverlayActive = isOverlayActive,
                    onOpenPermissionCenter = onNavigateToPermissions,
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
        }

        // Native Ad (Premium users see zero ads)
        item {
            LiquidGlassNativeAdCard(
                isPremium = settings?.isPremiumActive ?: false
            )
        }

        // Active Limits Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSectionHeader(
                    title = stringResource(R.string.active_limits),
                    subtitle = "${limits.size} ${stringResource(R.string.monitored_suffix)}",
                    isCritical = true
                )
                if (limits.isNotEmpty()) {
                    GlassButton(
                        onClick = { onNavigateToApps?.invoke() },
                        text = "Manage",
                        style = GlassButtonStyle.SECONDARY
                    )
                }
            }
        }

        // Limits List or Simplified Empty State
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

        // Adaptive Banner Ad (Zero for premium)
        item {
            LiquidGlassAdaptiveBanner(
                isPremium = settings?.isPremiumActive ?: false
            )
        }

        // Social Bar (Zero for premium)
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
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF24DFEC))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.digital_equilibrium).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF24DFEC)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Streak Flame Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x35FFAB00))
                    .border(1.dp, Color(0x80FFAB00), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFFAB00),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "${settings?.currentStreak ?: 0}d",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFAB00)
                    )
                }
            }

            // User Level Avatar (Frosted glass circle)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x3524DFEC))
                    .border(1.2.dp, Color(0xFF24DFEC).copy(alpha = 0.8f), CircleShape),
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
private fun PrimaryFocusActionCard(onStartFocus: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(26.dp),
                isElevated = true,
                isHighlight = true
            )
            .clickable { onStartFocus() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                GlassIconBubble(
                    icon = Icons.Default.PlayArrow,
                    size = 50.dp,
                    iconSize = 28.dp,
                    isHighlight = true
                )

                Column {
                    Text(
                        text = stringResource(R.string.start_focus),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Instant 25m distraction block",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            GlassButton(
                onClick = onStartFocus,
                text = "Start",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                style = GlassButtonStyle.PRIMARY
            )
        }
    }
}

@Composable
private fun CoreMetricsSection(
    settings: UserSettings?,
    stats: StatsSummary,
    limits: List<AppLimitUIModel>,
    onNavigateToStats: (() -> Unit)? = null
) {
    // 4 Clean Core Metrics
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metric 1: Today's Usage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                    .clickable { onNavigateToStats?.invoke() }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.todays_usage).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stats.totalUsedMinutes / 60}h ${stats.totalUsedMinutes % 60}m",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )
                }
            }

            // Metric 2: Focus Score
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(shape = RoundedCornerShape(22.dp), isHighlight = true)
                    .clickable { onNavigateToStats?.invoke() }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.focus_score).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${settings?.focusScore ?: 0}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFF24DFEC)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metric 3: Time Saved
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                    .clickable { onNavigateToStats?.invoke() }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.time_reclaimed).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+${stats.totalSavedMinutes}m",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFF24DFEC)
                    )
                }
            }

            // Metric 4: Active Limits
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                    .clickable { onNavigateToStats?.invoke() }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.blocked_apps).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${limits.size}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLimitsCard(onNavigateToApps: (() -> Unit)? = null) {
    GlassEmptyState(
        icon = Icons.Default.Shield,
        title = stringResource(R.string.no_limits_set),
        subtitle = stringResource(R.string.no_limits_desc),
        actionText = if (onNavigateToApps != null) stringResource(R.string.set_app_limits) else null,
        onAction = onNavigateToApps
    )
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
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    val progressColor = if (isExceeded) Color(0xFFFF5252) else Color(0xFF24DFEC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isElevated = false,
                isHighlight = isExceeded
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                com.example.presentation.common.RealAppIcon(
                    packageName = limit.packageName,
                    appName = limit.appName,
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.used_format, limit.usedMinutes, limit.dailyLimitMinutes),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                val statusText = if (isExceeded) {
                    stringResource(R.string.blocked_status)
                } else {
                    stringResource(R.string.min_left, limit.remainingMinutes)
                }

                GlassStatusBadge(
                    text = statusText,
                    isWarning = isExceeded
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Limit",
                        tint = Color(0xFF24DFEC),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Limit",
                        tint = Color(0xFFFF5252).copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Clean thin progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0x30FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(progressColor)
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
        modifier = Modifier.liquidGlass(shape = RoundedCornerShape(26.dp), isElevated = true),
        containerColor = Color.Transparent,
        title = {
            Text(
                text = stringResource(R.string.adjust_limit_title, limit.appName),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.daily_allowance, sliderValue.toInt()),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF24DFEC)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 5f..240f,
                    steps = 46,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF24DFEC),
                        activeTrackColor = Color(0xFF24DFEC),
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
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.hours_4),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { onConfirm(sliderValue.toInt()) },
                text = stringResource(R.string.save),
                style = GlassButtonStyle.PRIMARY
            )
        },
        dismissButton = {
            GlassButton(
                onClick = onDismiss,
                text = stringResource(R.string.cancel),
                style = GlassButtonStyle.SECONDARY
            )
        }
    )
}

@Composable
fun ProtectionStatusBanner(
    isAccessibilityActive: Boolean,
    isOverlayActive: Boolean,
    onOpenPermissionCenter: (() -> Unit)? = null,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    val isFullyActive = isAccessibilityActive && isOverlayActive
    val bannerColor = if (isFullyActive) Color(0xFF24DFEC) else Color(0xFFFF5252)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isHighlight = !isFullyActive
            )
            .clickable(enabled = onOpenPermissionCenter != null) {
                onOpenPermissionCenter?.invoke()
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconBubble(
                icon = if (isFullyActive) Icons.Default.Shield else Icons.Default.WarningAmber,
                size = 46.dp,
                iconSize = 24.dp,
                isHighlight = isFullyActive
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFullyActive) {
                        stringResource(R.string.active_protection)
                    } else {
                        stringResource(R.string.protection_limited)
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
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
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            if (!isFullyActive) {
                Spacer(modifier = Modifier.width(10.dp))
                GlassButton(
                    onClick = {
                        if (onOpenPermissionCenter != null) {
                            onOpenPermissionCenter.invoke()
                        } else if (!isAccessibilityActive) {
                            onEnableAccessibility()
                        } else {
                            onEnableOverlay()
                        }
                    },
                    text = if (onOpenPermissionCenter != null) "Manage" else stringResource(R.string.enable_btn),
                    style = GlassButtonStyle.PRIMARY
                )
            }
        }
    }
}
