package com.example.presentation.apps

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.theme.GlassButton
import com.example.ui.theme.GlassButtonStyle
import com.example.ui.theme.GlassEmptyState
import com.example.ui.theme.GlassIconBubble
import com.example.ui.theme.GlassSectionHeader
import com.example.ui.theme.GlassStatusBadge
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = viewModel(
        factory = AppsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccessibility by remember { mutableStateOf(PermissionHelper.hasAccessibilityPermission(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = PermissionHelper.hasAccessibilityPermission(context)
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val appsList by viewModel.appsList.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle(initialValue = null)
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LIMITED

    var appToConfigure by remember { mutableStateOf<AppItem?>(null) }
    var highImpactAppPending by remember { mutableStateOf<Triple<AppItem, Int, Int?>?>(null) }
    var showTemplatesDialog by remember { mutableStateOf(false) }

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
                            color = Color.White
                        )
                        Text(
                            text = "MONITOR & RESTRICT APPS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.5.sp
                            ),
                            color = Color(0xFF24DFEC)
                        )
                    }
                },
                actions = {
                    // Secondary action: Templates in clean frosted pill
                    GlassButton(
                        onClick = { showTemplatesDialog = true },
                        text = "Templates",
                        icon = Icons.Default.Tune,
                        style = GlassButtonStyle.SECONDARY,
                        modifier = Modifier.testTag("templates_button")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
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
            // Permission warning if needed
            if (!hasAccessibility || !hasOverlay) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(22.dp),
                                isHighlight = true
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassIconBubble(
                                icon = Icons.Default.WarningAmber,
                                size = 44.dp,
                                iconSize = 22.dp,
                                isHighlight = false
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Setup Needed",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = Color(0xFFFF5252)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (!hasAccessibility) "Grant accessibility to detect apps."
                                    else "Grant overlay to show lock screen.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            GlassButton(
                                onClick = {
                                    if (!hasAccessibility) {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                        }
                                    } else {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                },
                                text = "Fix",
                                style = GlassButtonStyle.PRIMARY
                            )
                        }
                    }
                }
            }

            // Top 320x50 Banner Ad (Visible immediately from the start)
            item {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // Search Bar & Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search apps...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF24DFEC).copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                        focusedContainerColor = Color(0x3538485B),
                        unfocusedContainerColor = Color(0x2838485B)
                    ),
                    modifier = Modifier
                        .testTag("app_search_input")
                        .fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Prioritized Filters: All vs Limited vs Blocked
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val allSelected = selectedFilter == "ALL"
                    val limitedSelected = selectedFilter == "LIMITED"
                    val blockedSelected = selectedFilter == "BLOCKED"
                    val blockedCount = appsList.count { it.isLimited && (it.dailyLimitMinutes == 0 || (it.dailyLimitMinutes > 0 && it.usedTodayMinutes >= it.dailyLimitMinutes)) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (allSelected) Color(0x3524DFEC) else Color(0x303E4C5E))
                            .border(
                                1.dp,
                                if (allSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.30f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = "ALL" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All (${appsList.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = if (allSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (limitedSelected) Color(0x3524DFEC) else Color(0x303E4C5E))
                            .border(
                                1.dp,
                                if (limitedSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.30f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = "LIMITED" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (limitedSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limits (${appsList.count { it.isLimited }})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (limitedSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (limitedSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (blockedSelected) Color(0x35FF5252) else Color(0x303E4C5E))
                            .border(
                                1.dp,
                                if (blockedSelected) Color(0xFFFF5252) else Color.White.copy(alpha = 0.30f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = "BLOCKED" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (blockedCount > 0) Color(0xFFFF5252) else Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Blocked ($blockedCount)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (blockedSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (blockedSelected) Color(0xFFFF5252) else Color.White.copy(alpha = 0.8f)
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
                        subtitle = if (selectedFilter == "LIMITED") "Select 'All Apps' to configure limits on your installed apps."
                        else "No apps match your search filter.",
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
                        onItemClick = { appToConfigure = app },
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (!app.isLimited) {
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

                // Native Ad Card
                item {
                    LiquidGlassNativeAdCard(
                        isPremium = userSettings?.isPremiumActive ?: false
                    )
                }

                items(secondBatch, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        onItemClick = { appToConfigure = app },
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (!app.isLimited) {
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

            // Banner Ad
            item {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // Social Bar
            item {
                AdsterraSocialBar(
                    isPremium = userSettings?.isPremiumActive ?: false,
                    isFocusActive = userSettings?.isFocusModeActive ?: false
                )
            }
        }
    }

    // --- TEMPLATES DIALOG (Secondary Action) ---
    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            modifier = Modifier.liquidGlass(shape = RoundedCornerShape(26.dp), isElevated = true),
            containerColor = Color.Transparent,
            title = {
                Text(
                    text = "Quick Restriction Templates",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
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
                                .liquidGlass(shape = RoundedCornerShape(18.dp))
                                .clickable {
                                    viewModel.applyTemplate(title)
                                    showTemplatesDialog = false
                                }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassIconBubble(icon = icon, size = 40.dp, iconSize = 20.dp, isHighlight = true)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                        color = Color.White.copy(alpha = 0.65f)
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

    // --- CONFIGURE TIME LIMIT DIALOG ---
    if (appToConfigure != null) {
        val app = appToConfigure!!
        CustomTimeLimitDialog(
            app = app,
            onDismiss = { appToConfigure = null },
            onSaveLimit = { customMinutes, sessionMinutes ->
                appToConfigure = null
                if (app.isHighImpact && !app.isLimited) {
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
            modifier = Modifier.liquidGlass(shape = RoundedCornerShape(26.dp), isElevated = true),
            containerColor = Color.Transparent,
            title = {
                Text(
                    text = "Confirm Restriction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Add \"${app.appName}\" to active limits with ${if (minutes == 0) "strict block (0m)" else "$minutes min/day allowance"}?",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                GlassButton(
                    onClick = {
                        viewModel.setCustomLimit(app, minutes, sessionMinutes, isEnabled = true)
                        highImpactAppPending = null
                    },
                    text = "Confirm",
                    style = GlassButtonStyle.PRIMARY
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
    onItemClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isHighlight = app.isLimited
            )
            .clickable(onClick = onItemClick)
            .padding(15.dp)
    ) {
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
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isHighImpact) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x35EF4444))
                                .border(1.dp, Color(0x60EF4444), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Distracting",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFFF5252)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val subtitle = if (app.isLimited) {
                    if (app.dailyLimitMinutes == 0) "Strict Block (0m)"
                    else "Limit: ${app.dailyLimitMinutes}m/day (Used: ${app.usedTodayMinutes}m)"
                } else {
                    if (app.usedTodayMinutes > 0) "Today: ${app.usedTodayMinutes}m"
                    else "No limit configured"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = if (app.isLimited) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.65f)
                )
            }

            Switch(
                checked = app.isLimited,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF061820),
                    checkedTrackColor = Color(0xFF24DFEC),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color(0x303E4C5E),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )
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
    val initialMinutes = if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes else 30
    var minutes by remember { mutableIntStateOf(initialMinutes) }
    var textInput by remember { mutableStateOf(initialMinutes.toString()) }

    val presetMinutes = listOf(0, 15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.liquidGlass(shape = RoundedCornerShape(26.dp), isElevated = true),
        containerColor = Color.Transparent,
        title = {
            Column {
                Text(
                    text = "Set Limit",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF24DFEC)
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
                    color = Color.White.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.take(3).forEach { presetMin ->
                        val isSelected = minutes == presetMin
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x3524DFEC) else Color(0x253E4C5E))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    minutes = presetMin
                                    textInput = presetMin.toString()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (presetMin == 0) "Block" else "${presetMin}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color(0xFF24DFEC) else Color.White
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
                                .background(if (isSelected) Color(0x3524DFEC) else Color(0x253E4C5E))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    minutes = presetMin
                                    textInput = presetMin.toString()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (presetMin >= 60) "${presetMin / 60}h" else "${presetMin}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color(0xFF24DFEC) else Color.White
                            )
                        }
                    }
                }

                Text(
                    text = "Daily Allowance: ${if (minutes == 0) "Strict Block (0m)" else "$minutes minutes"}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF24DFEC)
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x303E4C5E))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease 5m", tint = Color.White)
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF24DFEC),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    IconButton(
                        onClick = {
                            val newMin = minutes + 5
                            minutes = newMin
                            textInput = newMin.toString()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x303E4C5E))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase 5m", tint = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = { onSaveLimit(minutes, null) },
                text = "Save",
                style = GlassButtonStyle.PRIMARY
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
