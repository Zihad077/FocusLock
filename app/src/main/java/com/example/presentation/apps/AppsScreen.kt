package com.example.presentation.apps

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.database.isPremiumActive
import com.example.ui.theme.FocusLockSemanticColors
import com.example.ui.theme.GlassButton
import com.example.ui.theme.GlassButtonStyle
import com.example.ui.theme.GlassEmptyState
import com.example.ui.theme.GlassIconBubble
import com.example.ui.theme.GlassProgressBar
import com.example.ui.theme.GlassStatusBadge
import com.example.ui.theme.SemanticTone
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = viewModel(
        factory = AppsViewModel.Factory(LocalContext.current.applicationContext as Application)
    ),
    onNavigateToPermissions: ((targetAppName: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val semantic = FocusLockSemanticColors

    val appsList by viewModel.appsList.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle(initialValue = null)
    val arePermissionsGranted by viewModel.areRequiredPermissionsGranted.collectAsStateWithLifecycle()
    val missingPermissionNames by viewModel.missingPermissionNames.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, LIMITED, BLOCKED

    // Pending app/template preserved across navigation to the Permission Screen so user returns directly to setup
    var pendingPackageToConfigure by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTemplateToApply by rememberSaveable { mutableStateOf<String?>(null) }

    var appToConfigure by remember { mutableStateOf<AppItem?>(null) }
    var highImpactAppPending by remember { mutableStateOf<Triple<AppItem, Int, Int?>?>(null) }
    var showTemplatesDialog by remember { mutableStateOf(false) }

    // Recheck permissions on resume and automatically resume pending app-limit setup if all permissions are now granted
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.syncAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Listen for any ViewModel-level permission gate rejection
    LaunchedEffect(viewModel) {
        viewModel.permissionGateRequired.collect { targetName ->
            onNavigateToPermissions?.invoke(targetName)
        }
    }

    // Automatically resume the user's original limit-setup flow once all required permissions are granted
    LaunchedEffect(arePermissionsGranted, appsList, pendingPackageToConfigure, pendingTemplateToApply) {
        if (arePermissionsGranted) {
            val pkg = pendingPackageToConfigure
            if (pkg != null && appsList.isNotEmpty()) {
                val matchedApp = appsList.find { it.packageName == pkg }
                if (matchedApp != null) {
                    pendingPackageToConfigure = null
                    appToConfigure = matchedApp
                }
            }
            val pendingTemplate = pendingTemplateToApply
            if (pendingTemplate != null && appsList.isNotEmpty()) {
                pendingTemplateToApply = null
                viewModel.applyTemplate(pendingTemplate)
            }
        }
    }

    // Helper that gates any app selection or limit configuration behind required permissions
    val requestAppLimitSetup: (AppItem) -> Unit = { app ->
        if (PermissionHelper.areAllRequiredPermissionsGranted(context)) {
            viewModel.refreshPermissions()
            appToConfigure = app
        } else {
            viewModel.refreshPermissions()
            pendingPackageToConfigure = app.packageName
            onNavigateToPermissions?.invoke(app.appName)
        }
    }

    val filteredApps = remember(appsList, searchQuery, selectedFilter) {
        appsList.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "LIMITED" -> app.isLimited
                "BLOCKED" -> app.isLimited && (app.dailyLimitMinutes == 0 || (app.dailyLimitMinutes > 0 && app.usedTodayMinutes >= app.dailyLimitMinutes))
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val existingLimitedCount = remember(appsList) { appsList.count { it.isLimited } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Limits",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = semantic.textPrimary
                        )
                        Text(
                            text = "MONITOR & RESTRICT APPS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.5.sp
                            ),
                            color = semantic.info.text
                        )
                    }
                },
                actions = {
                    // Templates button (Purple semantic style for special/exclusive quick presets)
                    GlassButton(
                        onClick = {
                            if (PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                                showTemplatesDialog = true
                            } else {
                                onNavigateToPermissions?.invoke("Quick Templates")
                            }
                        },
                        text = "Templates",
                        icon = Icons.Default.Tune,
                        style = GlassButtonStyle.PREMIUM,
                        modifier = Modifier.testTag("templates_button")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = semantic.textPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Context-Based Permission Status Banner when required permissions are missing
            if (!arePermissionsGranted) {
                item(key = "apps_permission_banner") {
                    val hasExistingLimits = existingLimitedCount > 0
                    val bannerTone = if (hasExistingLimits) SemanticTone.RED else SemanticTone.AMBER
                    val palette = semantic.forTone(bannerTone)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(22.dp),
                                semanticTone = bannerTone
                            )
                            .clickable {
                                onNavigateToPermissions?.invoke(null)
                            }
                            .padding(15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassIconBubble(
                                icon = if (hasExistingLimits) Icons.Default.GppMaybe else Icons.Default.WarningAmber,
                                size = 46.dp,
                                iconSize = 24.dp,
                                semanticTone = bannerTone
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasExistingLimits) {
                                        "Enforcement Paused • Permission Missing"
                                    } else {
                                        "Permissions Needed for App Limits"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = palette.accent
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (hasExistingLimits) {
                                        "Your $existingLimitedCount configured limit${if (existingLimitedCount == 1) " is" else "s are"} saved, but blocking is paused until ${missingPermissionNames.joinToString(", ")} ${if (missingPermissionNames.size == 1) "is" else "are"} enabled."
                                    } else {
                                        "Missing: ${missingPermissionNames.joinToString(", ")}. Grant required permissions to configure and activate app limits."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.2.sp,
                                        lineHeight = 16.5.sp
                                    ),
                                    color = semantic.textPrimary.copy(alpha = 0.88f)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            GlassButton(
                                onClick = { onNavigateToPermissions?.invoke(null) },
                                text = "Grant",
                                style = if (hasExistingLimits) GlassButtonStyle.DESTRUCTIVE else GlassButtonStyle.WARNING,
                                modifier = Modifier.testTag("apps_grant_permissions_button")
                            )
                        }
                    }
                }
            }

            // Top 320x50 Banner Ad
            item(key = "apps_ad_banner_top") {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false,
                    slotKey = "banner_top"
                )
            }

            // Search Bar & Semantic Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search apps...",
                            color = semantic.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = semantic.info.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = semantic.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = semantic.textPrimary,
                        unfocusedTextColor = semantic.textPrimary,
                        focusedBorderColor = semantic.info.primary,
                        unfocusedBorderColor = semantic.outlineSubtle,
                        focusedContainerColor = Color(0xEB101B2D),
                        unfocusedContainerColor = Color(0xE00D1626)
                    ),
                    modifier = Modifier
                        .testTag("app_search_input")
                        .fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented Filter Bar with Context-Based Semantic Colors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xE00B1322))
                        .border(1.dp, semantic.outlineSubtle, RoundedCornerShape(18.dp))
                        .padding(5.dp)
                ) {
                    val allSelected = selectedFilter == "ALL"
                    val limitedSelected = selectedFilter == "LIMITED"
                    val blockedSelected = selectedFilter == "BLOCKED"
                    val blockedCount = appsList.count {
                        it.isLimited && (it.dailyLimitMinutes == 0 || (it.dailyLimitMinutes > 0 && it.usedTodayMinutes >= it.dailyLimitMinutes))
                    }

                    // All Apps (Blue/Teal interactive info tone)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (allSelected) semantic.info.container else Color.Transparent)
                            .then(
                                if (allSelected) Modifier.border(1.dp, semantic.info.border, RoundedCornerShape(13.dp))
                                else Modifier
                            )
                            .clickable { selectedFilter = "ALL" }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All (${appsList.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = if (allSelected) semantic.info.primary else semantic.textSecondary
                        )
                    }

                    // Active Limits (Green when active & permissions granted, Amber when permissions missing)
                    val limitsTone = if (arePermissionsGranted) SemanticTone.GREEN else SemanticTone.AMBER
                    val limitsPalette = semantic.forTone(limitsTone)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (limitedSelected) limitsPalette.container else Color.Transparent)
                            .then(
                                if (limitedSelected) Modifier.border(1.dp, limitsPalette.border, RoundedCornerShape(13.dp))
                                else Modifier
                            )
                            .clickable { selectedFilter = "LIMITED" }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (limitedSelected) limitsPalette.accent else semantic.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limits ($existingLimitedCount)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (limitedSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (limitedSelected) limitsPalette.accent else semantic.textSecondary
                            )
                        }
                    }

                    // Blocked Apps (Red semantic tone for expired/blocked limits)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (blockedSelected) semantic.red.container else Color.Transparent)
                            .then(
                                if (blockedSelected) Modifier.border(1.dp, semantic.red.border, RoundedCornerShape(13.dp))
                                else Modifier
                            )
                            .clickable { selectedFilter = "BLOCKED" }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (blockedCount > 0) semantic.red.primary else semantic.textMuted)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Blocked ($blockedCount)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (blockedSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (blockedSelected) semantic.red.text else semantic.textSecondary
                            )
                        }
                    }
                }
            }

            // App List
            if (filteredApps.isEmpty()) {
                item {
                    GlassEmptyState(
                        icon = if (selectedFilter == "LIMITED") Icons.Default.LockOpen else Icons.Default.SearchOff,
                        title = if (selectedFilter == "LIMITED") "No Limited Apps" else "No Apps Found",
                        subtitle = if (selectedFilter == "LIMITED") "Select 'All' to choose an app and configure a daily usage limit."
                        else "No apps match your current filter.",
                        actionText = if (selectedFilter == "LIMITED") "View All Apps" else null,
                        onAction = if (selectedFilter == "LIMITED") { { selectedFilter = "ALL" } } else null
                    )
                }
            } else {
                val halfSize = (filteredApps.size / 2).coerceAtLeast(1)
                val firstBatch = filteredApps.take(halfSize)
                val secondBatch = filteredApps.drop(halfSize)

                items(firstBatch, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        arePermissionsGranted = arePermissionsGranted,
                        onItemClick = { requestAppLimitSetup(app) },
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Permission check before enabling or configuring any app limit
                                if (!PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                                    viewModel.refreshPermissions()
                                    pendingPackageToConfigure = app.packageName
                                    onNavigateToPermissions?.invoke(app.appName)
                                } else if (!app.isLimited) {
                                    appToConfigure = app
                                } else {
                                    viewModel.toggleLimit(app, true)
                                }
                            } else {
                                viewModel.toggleLimit(app, false)
                            }
                        }
                    )
                }

                // 1:1 Square Native Banner Ad Card
                item(key = "apps_ad_native_main") {
                    LiquidGlassNativeAdCard(
                        isPremium = userSettings?.isPremiumActive ?: false,
                        slotKey = "native_main"
                    )
                }

                items(secondBatch, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        arePermissionsGranted = arePermissionsGranted,
                        onItemClick = { requestAppLimitSetup(app) },
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (!PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                                    viewModel.refreshPermissions()
                                    pendingPackageToConfigure = app.packageName
                                    onNavigateToPermissions?.invoke(app.appName)
                                } else if (!app.isLimited) {
                                    appToConfigure = app
                                } else {
                                    viewModel.toggleLimit(app, true)
                                }
                            } else {
                                viewModel.toggleLimit(app, false)
                            }
                        }
                    )
                }
            }

            // Bottom 320x50 Banner Ad
            item(key = "apps_ad_banner_bottom") {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false,
                    slotKey = "banner_bottom"
                )
            }

            // Social Bar
            item(key = "apps_ad_social_bar") {
                AdsterraSocialBar(
                    isPremium = userSettings?.isPremiumActive ?: false,
                    isFocusActive = userSettings?.isFocusModeActive ?: false,
                    slotKey = "social_bar"
                )
            }
        }
    }

    // --- TEMPLATES DIALOG (Permission-Gated) ---
    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = semantic.surfaceElevated,
            modifier = Modifier.border(1.dp, semantic.purple.border, RoundedCornerShape(24.dp)),
            title = {
                Text(
                    text = "Quick Restriction Templates",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = semantic.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val templates = listOf(
                        Triple("Social Media", "30m daily allowance on social apps", Icons.Default.Lock),
                        Triple("Gaming", "45m daily limit on games", Icons.Default.SportsEsports),
                        Triple("Entertainment", "60m daily limit on video apps", Icons.Default.Movie)
                    )

                    templates.forEach { (title, subtitle, icon) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(semantic.purple.container)
                                .border(1.dp, semantic.purple.border, RoundedCornerShape(16.dp))
                                .clickable {
                                    showTemplatesDialog = false
                                    if (PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                                        viewModel.applyTemplate(title)
                                    } else {
                                        pendingTemplateToApply = title
                                        onNavigateToPermissions?.invoke(title)
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassIconBubble(
                                    icon = icon,
                                    size = 40.dp,
                                    iconSize = 20.dp,
                                    semanticTone = SemanticTone.PURPLE
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = semantic.textPrimary
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                        color = semantic.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GlassButton(
                    onClick = { showTemplatesDialog = false },
                    text = "Close",
                    style = GlassButtonStyle.SECONDARY
                )
            }
        )
    }

    // --- CONFIGURE TIME LIMIT DIALOG (Permission-Gated) ---
    if (appToConfigure != null) {
        val app = appToConfigure!!
        CustomTimeLimitDialog(
            app = app,
            onDismiss = { appToConfigure = null },
            onSaveLimit = { customMinutes, sessionMinutes ->
                appToConfigure = null
                if (!PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                    pendingPackageToConfigure = app.packageName
                    onNavigateToPermissions?.invoke(app.appName)
                } else if (app.isHighImpact && !app.isLimited) {
                    highImpactAppPending = Triple(app, customMinutes, sessionMinutes)
                } else {
                    viewModel.setCustomLimit(app, customMinutes, sessionMinutes, isEnabled = true)
                }
            },
            onRemoveLimit = {
                viewModel.removeLimit(app.packageName)
                appToConfigure = null
            }
        )
    }

    // --- HIGH-IMPACT CONFIRMATION DIALOG ---
    if (highImpactAppPending != null) {
        val (app, minutes, sessionMinutes) = highImpactAppPending!!
        AlertDialog(
            onDismissRequest = { highImpactAppPending = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = semantic.surfaceElevated,
            modifier = Modifier.border(1.dp, semantic.amber.border, RoundedCornerShape(24.dp)),
            title = {
                Text(
                    text = "Confirm App Limit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = semantic.textPrimary
                )
            },
            text = {
                Text(
                    text = "Activate limit for \"${app.appName}\" with ${if (minutes == 0) "strict block (0m)" else "$minutes min/day allowance"}?",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = semantic.textSecondary
                )
            },
            confirmButton = {
                GlassButton(
                    onClick = {
                        highImpactAppPending = null
                        if (!PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                            pendingPackageToConfigure = app.packageName
                            onNavigateToPermissions?.invoke(app.appName)
                        } else {
                            viewModel.setCustomLimit(app, minutes, sessionMinutes, isEnabled = true)
                        }
                    },
                    text = "Activate Limit",
                    style = GlassButtonStyle.SUCCESS
                )
            },
            dismissButton = {
                GlassButton(
                    onClick = { highImpactAppPending = null },
                    text = "Cancel",
                    style = GlassButtonStyle.SECONDARY
                )
            }
        )
    }
}

@Composable
fun AppListItem(
    app: AppItem,
    arePermissionsGranted: Boolean = true,
    onItemClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    val semantic = FocusLockSemanticColors

    val isBlockedOrExpired = app.isLimited &&
            (app.dailyLimitMinutes == 0 || (app.dailyLimitMinutes > 0 && app.usedTodayMinutes >= app.dailyLimitMinutes))
    val usageRatio = if (app.isLimited && app.dailyLimitMinutes > 0) {
        (app.usedTodayMinutes.toFloat() / app.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isApproachingLimit = app.isLimited && !isBlockedOrExpired && usageRatio >= 0.75f
    val isEnforcementPaused = app.isLimited && !arePermissionsGranted

    // Context-based card semantic tone:
    // - RED: Enforcement paused due to missing permission, or limit expired/blocked
    // - AMBER: Approaching time limit (>= 75% used)
    // - GREEN: Active healthy limit (< 75% used)
    // - NEUTRAL: Unrestricted app
    val itemSemanticTone: SemanticTone? = when {
        isEnforcementPaused -> SemanticTone.RED
        isBlockedOrExpired -> SemanticTone.RED
        isApproachingLimit -> SemanticTone.AMBER
        app.isLimited -> SemanticTone.GREEN
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isHighlight = app.isLimited,
                semanticTone = itemSemanticTone
            )
            .clickable(onClick = onItemClick)
            .padding(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.presentation.common.RealAppIcon(
                    packageName = app.packageName,
                    appName = app.appName,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = semantic.textPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isEnforcementPaused) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(semantic.red.container)
                                    .border(1.dp, semantic.red.border, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "PAUSED • PERMISSION MISSING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = semantic.red.text
                                )
                            }
                        } else if (app.isHighImpact) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(semantic.amber.container)
                                    .border(1.dp, semantic.amber.border, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "High Distraction",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = semantic.amber.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val subtitle = when {
                        isEnforcementPaused ->
                            "Saved limit (${app.dailyLimitMinutes}m) • Grant permissions to enforce"
                        app.isLimited && app.dailyLimitMinutes == 0 ->
                            "Strict Block (0m allowed) • Blocked"
                        isBlockedOrExpired ->
                            "Limit Reached: ${app.usedTodayMinutes}m / ${app.dailyLimitMinutes}m used"
                        isApproachingLimit ->
                            "Approaching Limit: ${app.usedTodayMinutes}m / ${app.dailyLimitMinutes}m (${(app.dailyLimitMinutes - app.usedTodayMinutes).coerceAtLeast(0)}m left)"
                        app.isLimited ->
                            "Active Limit: ${app.dailyLimitMinutes}m/day (Used: ${app.usedTodayMinutes}m)"
                        app.usedTodayMinutes > 0 ->
                            "Today: ${app.usedTodayMinutes}m • Tap to set limit"
                        else ->
                            "No limit configured • Tap to set limit"
                    }

                    val subtitleColor = when {
                        isEnforcementPaused || isBlockedOrExpired -> semantic.red.text
                        isApproachingLimit -> semantic.amber.text
                        app.isLimited -> semantic.green.text
                        else -> semantic.textSecondary
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = subtitleColor
                    )
                }

                Switch(
                    checked = app.isLimited,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = when {
                            isEnforcementPaused || isBlockedOrExpired -> Color.White
                            isApproachingLimit -> semantic.amber.onPrimary
                            else -> semantic.green.onPrimary
                        },
                        checkedTrackColor = when {
                            isEnforcementPaused || isBlockedOrExpired -> semantic.red.primary
                            isApproachingLimit -> semantic.amber.primary
                            else -> semantic.green.primary
                        },
                        uncheckedThumbColor = Color(0xFFCBD5E1),
                        uncheckedTrackColor = Color(0xFF18263A),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.22f)
                    )
                )
            }

            if (app.isLimited && app.dailyLimitMinutes > 0) {
                GlassProgressBar(
                    progress = usageRatio,
                    height = 6.dp,
                    semanticTone = when {
                        isEnforcementPaused || isBlockedOrExpired -> SemanticTone.RED
                        isApproachingLimit -> SemanticTone.AMBER
                        else -> SemanticTone.GREEN
                    }
                )
            }
        }
    }
}

@Composable
fun CustomTimeLimitDialog(
    app: AppItem,
    onDismiss: () -> Unit,
    onSaveLimit: (dailyMinutes: Int, sessionMinutes: Int?) -> Unit,
    onRemoveLimit: () -> Unit
) {
    val semantic = FocusLockSemanticColors
    val initialMinutes = if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes else 30
    var minutes by remember { mutableIntStateOf(initialMinutes) }
    var textInput by remember { mutableStateOf(initialMinutes.toString()) }

    val presetMinutes = listOf(0, 15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = semantic.surfaceElevated,
        modifier = Modifier.border(1.dp, semantic.info.border, RoundedCornerShape(24.dp)),
        title = {
            Column {
                Text(
                    text = "Set App Limit",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = semantic.textPrimary
                )
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = semantic.info.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "QUICK PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = semantic.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.take(3).forEach { presetMin ->
                        val isSelected = minutes == presetMin
                        val isStrictBlock = presetMin == 0
                        val chipTone = if (isStrictBlock) semantic.red else semantic.info
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) chipTone.container else Color(0xFF162438))
                                .border(
                                    1.dp,
                                    if (isSelected) chipTone.primary else semantic.outlineSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    minutes = presetMin
                                    textInput = presetMin.toString()
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (presetMin == 0) "Block" else "${presetMin}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) chipTone.text else semantic.textPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.drop(3).forEach { presetMin ->
                        val isSelected = minutes == presetMin
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) semantic.info.container else Color(0xFF162438))
                                .border(
                                    1.dp,
                                    if (isSelected) semantic.info.primary else semantic.outlineSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    minutes = presetMin
                                    textInput = presetMin.toString()
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (presetMin >= 60) "${presetMin / 60}h" else "${presetMin}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) semantic.info.primary else semantic.textPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "Daily Allowance: ${if (minutes == 0) "Strict Block (0m)" else "$minutes minutes"}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (minutes == 0) semantic.red.text else semantic.info.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newMin = (minutes - 5).coerceAtLeast(0)
                            minutes = newMin
                            textInput = newMin.toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF162438))
                            .border(1.dp, semantic.outlineSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease 5m", tint = semantic.textPrimary)
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { input ->
                            textInput = input
                            val parsed = input.toIntOrNull()
                            if (parsed != null && parsed >= 0) {
                                minutes = parsed
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = semantic.textPrimary,
                            unfocusedTextColor = semantic.textPrimary,
                            focusedBorderColor = semantic.info.primary,
                            unfocusedBorderColor = semantic.outlineSubtle,
                            focusedContainerColor = Color(0xFF0B1422),
                            unfocusedContainerColor = Color(0xFF0B1422)
                        )
                    )

                    IconButton(
                        onClick = {
                            val newMin = minutes + 5
                            minutes = newMin
                            textInput = newMin.toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF162438))
                            .border(1.dp, semantic.outlineSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase 5m", tint = semantic.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { onSaveLimit(minutes, null) },
                text = "Save Limit",
                style = GlassButtonStyle.SUCCESS
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (app.isLimited) {
                    GlassButton(
                        onClick = onRemoveLimit,
                        text = "Remove",
                        style = GlassButtonStyle.DESTRUCTIVE
                    )
                }
                GlassButton(
                    onClick = onDismiss,
                    text = "Cancel",
                    style = GlassButtonStyle.SECONDARY
                )
            }
        }
    )
}
