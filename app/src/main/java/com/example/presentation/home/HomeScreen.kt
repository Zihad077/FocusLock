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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.presentation.common.FocusStreakMilestonesCard
import com.example.service.AppMonitorService
import com.example.ui.theme.FocusLockSemanticColors
import com.example.ui.theme.GlassButton
import com.example.ui.theme.GlassButtonStyle
import com.example.ui.theme.GlassEmptyState
import com.example.ui.theme.GlassIconBubble
import com.example.ui.theme.GlassProgressBar
import com.example.ui.theme.GlassSectionHeader
import com.example.ui.theme.GlassStatusBadge
import com.example.ui.theme.SemanticTone
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
    onNavigateToPermissions: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToAchievementShare: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val semantic = FocusLockSemanticColors

    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val limits by viewModel.limitsWithUsage.collectAsStateWithLifecycle()
    val stats by viewModel.statsSummary.collectAsStateWithLifecycle()
    val milestoneInfo by viewModel.milestoneInfo.collectAsStateWithLifecycle()

    var editingLimit by remember { mutableStateOf<AppLimitUIModel?>(null) }
    var pendingEditPackage by rememberSaveable { mutableStateOf<String?>(null) }

    var hasUsageAccess by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }
    var isAccessibilityActive by remember { mutableStateOf(PermissionHelper.hasAccessibilityPermission(context)) }
    var isOverlayActive by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }

    val allRequiredPermissionsGranted = hasUsageAccess && isAccessibilityActive && isOverlayActive
    val missingPermissionNames = remember(hasUsageAccess, isAccessibilityActive, isOverlayActive) {
        PermissionHelper.getMissingRequiredPermissionNames(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = PermissionHelper.hasUsageAccess(context)
                isAccessibilityActive = PermissionHelper.hasAccessibilityPermission(context)
                isOverlayActive = PermissionHelper.hasOverlayPermission(context)
                if (hasUsageAccess && isAccessibilityActive && isOverlayActive) {
                    AppMonitorService.startService(context)
                }
                viewModel.syncUsageData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Automatically resume editing the selected limit once all required permissions are granted
    LaunchedEffect(allRequiredPermissionsGranted, limits, pendingEditPackage) {
        if (allRequiredPermissionsGranted) {
            val pkg = pendingEditPackage
            if (pkg != null && limits.isNotEmpty()) {
                val found = limits.find { it.packageName == pkg }
                if (found != null) {
                    pendingEditPackage = null
                    editingLimit = found
                }
            }
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
            HeaderSection(
                settings = settings,
                onNavigateToSettings = onNavigateToSettings,
                onStreakClick = onNavigateToAchievementShare ?: onNavigateToStats
            )
        }

        // 320x50 Fixed Mobile Banner (Prominently placed at top from the beginning)
        item(key = "home_ad_banner_top") {
            LiquidGlassAdaptiveBanner(
                isPremium = settings?.isPremiumActive ?: false,
                slotKey = "banner_top"
            )
        }

        // Primary Hero Action: Quick Focus Session (Blue/Teal active focus psychology)
        item {
            PrimaryFocusActionCard(
                onStartFocus = { onNavigateToFocus?.invoke() }
            )
        }

        // 4 Core Metrics Dashboard with Context-Based Color Psychology
        item {
            CoreMetricsSection(
                settings = settings,
                stats = stats,
                limits = limits,
                allRequiredPermissionsGranted = allRequiredPermissionsGranted,
                onNavigateToStats = onNavigateToStats,
                onNavigateToApps = onNavigateToApps
            )
        }

        // Focus Streak & Milestones Card with Share Achievements CTA
        item(key = "home_streak_milestones") {
            FocusStreakMilestonesCard(
                milestoneInfo = milestoneInfo,
                onShareClick = onNavigateToAchievementShare
            )
        }

        // Permission Status Banner when any of the 3 required permissions is missing
        if (!allRequiredPermissionsGranted) {
            item {
                ProtectionStatusBanner(
                    isAccessibilityActive = isAccessibilityActive,
                    isOverlayActive = isOverlayActive,
                    hasUsageAccess = hasUsageAccess,
                    hasConfiguredLimits = limits.isNotEmpty(),
                    missingNames = missingPermissionNames,
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

        // 1:1 Square Native Banner Ad (Premium users see zero ads)
        item(key = "home_ad_native_main") {
            LiquidGlassNativeAdCard(
                isPremium = settings?.isPremiumActive ?: false,
                slotKey = "native_main"
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
                    subtitle = if (!allRequiredPermissionsGranted && limits.isNotEmpty()) {
                        "${limits.size} saved • Enforcement paused (permissions missing)"
                    } else {
                        "${limits.size} ${stringResource(R.string.monitored_suffix)}"
                    },
                    semanticTone = when {
                        !allRequiredPermissionsGranted && limits.isNotEmpty() -> SemanticTone.RED
                        limits.any { it.remainingMinutes <= 0 } -> SemanticTone.RED
                        limits.isNotEmpty() -> SemanticTone.GREEN
                        else -> SemanticTone.INFO
                    }
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
            items(limits, key = { it.packageName }) { limit ->
                AppLimitCard(
                    limit = limit,
                    arePermissionsGranted = allRequiredPermissionsGranted,
                    onEditClick = {
                        if (PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                            editingLimit = limit
                        } else {
                            pendingEditPackage = limit.packageName
                            onNavigateToPermissions?.invoke()
                        }
                    },
                    onDeleteClick = { viewModel.removeLimit(limit.packageName) }
                )
            }
        }

        // Bottom 320x50 Banner & Social Bar (Zero for premium)
        item(key = "home_ad_banner_bottom") {
            LiquidGlassAdaptiveBanner(
                isPremium = settings?.isPremiumActive ?: false,
                slotKey = "banner_bottom"
            )
        }

        item(key = "home_ad_social_bar") {
            AdsterraSocialBar(
                isPremium = settings?.isPremiumActive ?: false,
                isFocusActive = settings?.isFocusModeActive ?: false,
                slotKey = "social_bar"
            )
        }
    }

    editingLimit?.let { limit ->
        EditLimitDialog(
            limit = limit,
            onDismiss = { editingLimit = null },
            onConfirm = { minutes ->
                editingLimit = null
                if (PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                    viewModel.updateLimit(limit.packageName, limit.appName, minutes)
                } else {
                    pendingEditPackage = limit.packageName
                    onNavigateToPermissions?.invoke()
                }
            }
        )
    }
}

@Composable
private fun HeaderSection(
    settings: UserSettings?,
    onNavigateToSettings: (() -> Unit)? = null,
    onStreakClick: (() -> Unit)? = null
) {
    val semantic = FocusLockSemanticColors

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
                color = semantic.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(semantic.info.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.digital_equilibrium).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    ),
                    color = semantic.info.text
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Streak Flame Chip (Amber/Orange warm energy badge)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(semantic.amber.container)
                    .border(1.dp, semantic.amber.border, RoundedCornerShape(16.dp))
                    .clickable(enabled = onStreakClick != null) { onStreakClick?.invoke() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = semantic.amber.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "${settings?.currentStreak ?: 0}d",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = semantic.amber.text
                    )
                }
            }

            if (onNavigateToSettings != null) {
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xE6142238))
                        .border(1.dp, semantic.outlineSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = semantic.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryFocusActionCard(onStartFocus: () -> Unit) {
    val semantic = FocusLockSemanticColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(26.dp),
                isElevated = true,
                isHighlight = true,
                semanticTone = SemanticTone.INFO
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
                    semanticTone = SemanticTone.INFO
                )

                Column {
                    Text(
                        text = stringResource(R.string.start_focus),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = semantic.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Zero distractions. Pure flow state ✨",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = semantic.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            GlassButton(
                onClick = onStartFocus,
                text = "Lock In",
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
    allRequiredPermissionsGranted: Boolean = true,
    onNavigateToStats: (() -> Unit)? = null,
    onNavigateToApps: (() -> Unit)? = null
) {
    val semantic = FocusLockSemanticColors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metric 1: Today's Usage (Neutral / Informational)
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
                        color = semantic.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = com.example.util.FormatUtils.formatHoursMinutes(stats.totalUsedMinutes),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = semantic.textPrimary
                    )
                }
            }

            // Metric 2: Focus Score (Blue/Teal active focus metric)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        isHighlight = true,
                        semanticTone = SemanticTone.INFO
                    )
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
                        color = semantic.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${settings?.focusScore ?: 0}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = semantic.info.primary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metric 3: Time Saved (Green positive achievement metric)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        semanticTone = SemanticTone.GREEN
                    )
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
                        color = semantic.green.text
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+${com.example.util.FormatUtils.formatHoursMinutes(stats.totalSavedMinutes)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = semantic.green.text
                    )
                }
            }

            // Metric 4: Active Limits & Real Blocked Apps
            val activeLimits = limits.filter { it.isEnabled }
            val blockedCount = activeLimits.count { it.remainingMinutes <= 0 }
            val isPausedByPermissions = activeLimits.isNotEmpty() && !allRequiredPermissionsGranted

            val metric4Tone: SemanticTone? = when {
                isPausedByPermissions -> SemanticTone.RED
                blockedCount > 0 -> SemanticTone.RED
                activeLimits.isNotEmpty() -> SemanticTone.GREEN
                else -> null
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        isHighlight = blockedCount > 0 || isPausedByPermissions,
                        semanticTone = metric4Tone
                    )
                    .clickable { onNavigateToApps?.invoke() }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = when {
                            isPausedByPermissions -> "LIMITS PAUSED"
                            blockedCount > 0 -> "BLOCKED NOW"
                            else -> "ACTIVE LIMITS"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = when {
                            isPausedByPermissions || blockedCount > 0 -> semantic.red.text
                            activeLimits.isNotEmpty() -> semantic.green.text
                            else -> semantic.textSecondary
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (blockedCount > 0) "$blockedCount" else "${activeLimits.size}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            ),
                            color = when {
                                isPausedByPermissions || blockedCount > 0 -> semantic.red.text
                                activeLimits.isNotEmpty() -> semantic.green.text
                                else -> semantic.textPrimary
                            }
                        )
                        Text(
                            text = when {
                                isPausedByPermissions -> "missing perm"
                                blockedCount > 0 -> "of ${activeLimits.size}"
                                else -> "monitored"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = semantic.textSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
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
    arePermissionsGranted: Boolean = true,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val semantic = FocusLockSemanticColors
    val isExceeded = limit.remainingMinutes <= 0
    val progress = limit.progress.coerceIn(0f, 1f)
    val isApproaching = !isExceeded && progress >= 0.75f
    val isEnforcementPaused = !arePermissionsGranted

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    // Semantic Tone per limit state:
    // - RED: Enforcement paused due to revoked/missing permissions OR limit expired/blocked
    // - AMBER: Approaching time limit (>= 75% used)
    // - GREEN: Healthy active limit (< 75% used)
    val cardTone = when {
        isEnforcementPaused || isExceeded -> SemanticTone.RED
        isApproaching -> SemanticTone.AMBER
        else -> SemanticTone.GREEN
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isElevated = false,
                isHighlight = isExceeded || isEnforcementPaused,
                semanticTone = cardTone
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
                        color = semantic.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnforcementPaused) {
                            "Saved (${com.example.util.FormatUtils.formatHoursMinutes(limit.dailyLimitMinutes)}) • Enforcement paused until permissions granted"
                        } else {
                            com.example.util.FormatUtils.formatUsedVsLimit(limit.usedMinutes, limit.dailyLimitMinutes)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = when {
                            isEnforcementPaused || isExceeded -> semantic.red.text
                            isApproaching -> semantic.amber.text
                            else -> semantic.textSecondary
                        }
                    )
                }

                val statusText = when {
                    isEnforcementPaused -> "PAUSED"
                    isExceeded -> stringResource(R.string.blocked_status)
                    else -> com.example.util.FormatUtils.formatRemaining(limit.remainingMinutes)
                }

                GlassStatusBadge(
                    text = statusText,
                    semanticTone = cardTone,
                    icon = when (cardTone) {
                        SemanticTone.RED -> Icons.Default.ErrorOutline
                        SemanticTone.AMBER -> Icons.Default.WarningAmber
                        else -> Icons.Default.CheckCircle
                    }
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Limit",
                        tint = semantic.info.primary,
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
                        tint = semantic.red.primary.copy(alpha = 0.90f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Semantic progress bar (Green -> Amber -> Red)
            GlassProgressBar(
                progress = animatedProgress,
                height = 7.dp,
                semanticTone = cardTone
            )
        }
    }
}

@Composable
private fun EditLimitDialog(
    limit: AppLimitUIModel,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val semantic = FocusLockSemanticColors
    var sliderValue by remember { mutableStateOf(limit.dailyLimitMinutes.toFloat().coerceAtLeast(5f)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = semantic.surfaceElevated,
        modifier = Modifier.border(1.dp, semantic.info.border, RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = stringResource(R.string.adjust_limit_title, limit.appName),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = semantic.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.daily_allowance, sliderValue.toInt()),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = semantic.info.primary
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 5f..240f,
                    steps = 46,
                    colors = SliderDefaults.colors(
                        thumbColor = semantic.info.primary,
                        activeTrackColor = semantic.info.primary,
                        inactiveTrackColor = Color(0xFF1E2F47)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.min_5),
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.textSecondary
                    )
                    Text(
                        text = stringResource(R.string.hours_4),
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.textSecondary
                    )
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { onConfirm(sliderValue.toInt()) },
                text = stringResource(R.string.save),
                style = GlassButtonStyle.SUCCESS
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
    hasUsageAccess: Boolean = true,
    hasConfiguredLimits: Boolean = false,
    missingNames: List<String> = emptyList(),
    onOpenPermissionCenter: (() -> Unit)? = null,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    val semantic = FocusLockSemanticColors
    val isFullyActive = isAccessibilityActive && isOverlayActive && hasUsageAccess

    // Semantic Tone:
    // - GREEN when all required permissions are active
    // - RED when user has configured limits and a required permission is missing/revoked
    // - AMBER when permissions are pending initial setup
    val bannerTone = when {
        isFullyActive -> SemanticTone.GREEN
        hasConfiguredLimits -> SemanticTone.RED
        else -> SemanticTone.AMBER
    }
    val palette = semantic.forTone(bannerTone)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isHighlight = !isFullyActive,
                semanticTone = bannerTone
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
                icon = when {
                    isFullyActive -> Icons.Default.Shield
                    hasConfiguredLimits -> Icons.Default.GppMaybe
                    else -> Icons.Default.WarningAmber
                },
                size = 46.dp,
                iconSize = 24.dp,
                semanticTone = bannerTone
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isFullyActive -> stringResource(R.string.active_protection)
                        hasConfiguredLimits -> "Enforcement Paused • Permission Missing"
                        else -> stringResource(R.string.protection_limited)
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    ),
                    color = palette.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        isFullyActive -> stringResource(R.string.protection_active_desc)
                        missingNames.isNotEmpty() ->
                            "Missing: ${missingNames.joinToString(", ")}. Tap to grant required permissions."
                        !isAccessibilityActive -> stringResource(R.string.accessibility_needed_desc)
                        else -> stringResource(R.string.overlay_needed_desc)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = semantic.textPrimary.copy(alpha = 0.88f)
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
                    text = if (onOpenPermissionCenter != null) "Grant" else stringResource(R.string.enable_btn),
                    style = if (hasConfiguredLimits) GlassButtonStyle.DESTRUCTIVE else GlassButtonStyle.WARNING
                )
            }
        }
    }
}
