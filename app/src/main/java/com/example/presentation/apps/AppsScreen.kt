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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.blocking.BlockActivity
import com.example.service.BlockOverlayManager
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, RESTRICTED, HIGH_IMPACT

    // App being configured in Custom Time Dialog
    var appToConfigure by remember { mutableStateOf<AppItem?>(null) }
    
    // App pending confirmation before being added to high-impact blocklist: (AppItem, dailyMinutes, sessionMinutes?)
    var highImpactAppPending by remember { mutableStateOf<Triple<AppItem, Int, Int?>?>(null) }

    val filteredApps = remember(appsList, searchQuery, selectedFilter) {
        appsList.filter { app ->
            val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "RESTRICTED" -> app.isLimited
                "HIGH_IMPACT" -> app.isHighImpact
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "App Blocklist & Limits",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (hasOverlay) {
                                BlockOverlayManager.getInstance(context.applicationContext)
                                    .showOverlay("Sample Distracting App", "com.example.sample", 45, 30)
                            } else {
                                val testIntent = Intent(context, BlockActivity::class.java).apply {
                                    putExtra("APP_NAME", "Sample Distracting App")
                                    putExtra("PACKAGE_NAME", "com.example.sample")
                                    putExtra("USED_MINUTES", 45)
                                    putExtra("LIMIT_MINUTES", 30)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                context.startActivity(testIntent)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Screen", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        var showTemplatesDialog by remember { mutableStateOf(false) }
        if (showTemplatesDialog) {
            AlertDialog(
                onDismissRequest = { showTemplatesDialog = false },
                title = { Text("App Limit Templates") },
                text = {
                    Column {
                        Text("Select a template to auto-apply limits:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.applyTemplate("Social Media"); showTemplatesDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Social Media (30 min)") }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { viewModel.applyTemplate("Gaming"); showTemplatesDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Gaming (45 min)") }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { viewModel.applyTemplate("Entertainment"); showTemplatesDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Entertainment (60 min)") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTemplatesDialog = false }) { Text("Close") }
                }
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!hasAccessibility || !hasOverlay) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(18.dp),
                                isHighlight = true
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Blocking Inactive",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (!hasAccessibility) "Enable Accessibility to detect and block apps."
                                    else "Enable Display Over Other Apps to show the block screen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Enable", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(2.dp))
                
                // Search Input with frosted background
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed applications...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            isElevated = false
                        ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${appsList.size})") },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "RESTRICTED",
                            onClick = { selectedFilter = "RESTRICTED" },
                            label = { Text("Restricted (${appsList.count { it.isLimited }})") },
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "HIGH_IMPACT",
                            onClick = { selectedFilter = "HIGH_IMPACT" },
                            label = { Text("High-Impact (${appsList.count { it.isHighImpact }})") },
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filteredApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No applications match your filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        onItemClick = {
                            appToConfigure = app
                        },
                        onConfigureClick = {
                            appToConfigure = app
                        },
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (!app.isLimited) {
                                    // Open custom time dialog directly so user can choose limit or Always Block
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // --- CUSTOM TIME LIMIT DIALOG ---
    if (appToConfigure != null) {
        val app = appToConfigure!!
        CustomTimeLimitDialog(
            app = app,
            onDismiss = { appToConfigure = null },
            onSaveLimit = { customMinutes, sessionMinutes ->
                appToConfigure = null
                if (app.isHighImpact && !app.isLimited) {
                    // Trigger confirmation dialog for high-impact app before adding
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

    // --- HIGH-IMPACT APP CONFIRMATION DIALOG ---
    if (highImpactAppPending != null) {
        val (app, minutes, sessionMinutes) = highImpactAppPending!!
        AlertDialog(
            onDismissRequest = { highImpactAppPending = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm High-Impact Block",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "You are about to add \"${app.appName}\" to your active blocklist.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "This application has been identified as a high-impact distraction. Once added, access will be restricted according to your custom daily allowance of ${if (minutes == 0) "0 minutes (Always Blocked)" else "$minutes minutes/day"}${if (sessionMinutes != null) " (Session limit: ${sessionMinutes}m)" else ""} and active focus schedules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Are you sure you want to permanently add this high-impact app to your active blocklist?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomLimit(app, minutes, sessionMinutes, isEnabled = true)
                        highImpactAppPending = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Add to Blocklist")
                }
            },
            dismissButton = {
                TextButton(onClick = { highImpactAppPending = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppListItem(
    app: AppItem,
    onItemClick: () -> Unit,
    onConfigureClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                isHighlight = app.isHighImpact || app.isLimited
            )
            .clickable(onClick = onItemClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Avatar / Initial with Glass gradient rim
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (app.isHighImpact) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(androidx.compose.ui.graphics.Color(0xFFFF5252), androidx.compose.ui.graphics.Color(0xFFD50000))
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(androidx.compose.ui.graphics.Color(0xFF0077D6), androidx.compose.ui.graphics.Color(0xFF00B4D8))
                            )
                        }
                    )
                    .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isHighImpact) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "High-Impact",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                if (app.activeUnlockMethod != null && app.activeUnlockRemainingMinutes != null) {
                    Text(
                        text = "Unlocked: ${app.activeUnlockRemainingMinutes}m left (${app.activeUnlockMethod})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (app.isLimited) {
                    val limitText = if (app.dailyLimitMinutes == 0) {
                        "Always Blocked (0 min/day)"
                    } else {
                        val sessionInfo = if (app.sessionLimitMinutes != null) " • Session: ${app.sessionLimitMinutes}m" else ""
                        "Limit: ${app.dailyLimitMinutes} min/day$sessionInfo"
                    }
                    Text(
                        text = limitText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Tap to set custom time limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
            }
            
            IconButton(onClick = onConfigureClick) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configure custom limit",
                    tint = if (app.isLimited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            
            Switch(
                checked = app.isLimited,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Custom Time Limit Dialog allowing users to configure any custom minutes
 * or choose from convenient presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    
    var hasSessionLimit by remember { mutableStateOf(app.sessionLimitMinutes != null && app.sessionLimitMinutes > 0) }
    var sessionMinutes by remember { mutableIntStateOf(app.sessionLimitMinutes ?: 15) }
    var sessionTextInput by remember { mutableStateOf((app.sessionLimitMinutes ?: 15).toString()) }

    val presetMinutes = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Configure App Restriction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (app.isHighImpact) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High-impact distracting app. Setting a strict limit or full block helps reclaim your focus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Dedicated "Always Block" Card
                Surface(
                    onClick = {
                        minutes = 0
                        textInput = "0"
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (minutes == 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = if (minutes == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Always Block (0 min)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (minutes == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Blocks app immediately when opened",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = minutes == 0,
                            onClick = {
                                minutes = 0
                                textInput = "0"
                            }
                        )
                    }
                }

                Text(
                    text = "Or Set Daily Usage Allowance",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.take(3).forEach { presetMin ->
                        FilterChip(
                            selected = (minutes == presetMin),
                            onClick = {
                                minutes = presetMin
                                textInput = presetMin.toString()
                            },
                            label = { Text("${presetMin}m") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.drop(3).forEach { presetMin ->
                        FilterChip(
                            selected = (minutes == presetMin),
                            onClick = {
                                minutes = presetMin
                                textInput = presetMin.toString()
                            },
                            label = { Text(if (presetMin >= 60) "${presetMin / 60}h" else "${presetMin}m") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Divider()

                Text(
                    text = "Custom Daily Minutes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Stepper + Custom Numeric Input
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
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease 5 minutes")
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
                        label = { Text("Minutes / day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val newMin = minutes + 5
                            minutes = newMin
                            textInput = newMin.toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase 5 minutes")
                    }
                }

                Text(
                    text = if (minutes == 0) "Access will be completely blocked all day."
                    else "Access will be restricted after $minutes minutes of foreground usage each day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                // Session Limit (Continuous usage in single session)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enforce Session Limit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Limits uninterrupted continuous use per session",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hasSessionLimit,
                        onCheckedChange = { hasSessionLimit = it }
                    )
                }

                if (hasSessionLimit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val newSession = (sessionMinutes - 5).coerceAtLeast(5)
                                sessionMinutes = newSession
                                sessionTextInput = newSession.toString()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease session limit")
                        }

                        OutlinedTextField(
                            value = sessionTextInput,
                            onValueChange = { input ->
                                sessionTextInput = input
                                val parsed = input.toIntOrNull()
                                if (parsed != null && parsed > 0) {
                                    sessionMinutes = parsed
                                }
                            },
                            label = { Text("Session Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val newSession = sessionMinutes + 5
                                sessionMinutes = newSession
                                sessionTextInput = newSession.toString()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase session limit")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSession = if (hasSessionLimit) sessionMinutes else null
                    onSaveLimit(minutes, finalSession)
                }
            ) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (app.isLimited) {
                    TextButton(
                        onClick = onRemoveLimit,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
