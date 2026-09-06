package com.example.presentation.apps

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = viewModel(
        factory = AppsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val appsList by viewModel.appsList.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, RESTRICTED, HIGH_IMPACT

    // App being configured in Custom Time Dialog
    var appToConfigure by remember { mutableStateOf<AppItem?>(null) }
    
    // App pending confirmation before being added to high-impact blocklist
    var highImpactAppPending by remember { mutableStateOf<Pair<AppItem, Int>?>(null) }

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
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "App Blocklist & Limits",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                
                // Search Input
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
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
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
                            label = { Text("All (${appsList.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "RESTRICTED",
                            onClick = { selectedFilter = "RESTRICTED" },
                            label = { Text("Restricted (${appsList.count { it.isLimited }})") },
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
                                if (app.isHighImpact && !app.isLimited) {
                                    // High-impact app verification before adding to active blocklist
                                    highImpactAppPending = Pair(app, if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes else 30)
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
            onSaveLimit = { customMinutes ->
                appToConfigure = null
                if (app.isHighImpact && !app.isLimited) {
                    // Trigger confirmation dialog for high-impact app before adding
                    highImpactAppPending = Pair(app, customMinutes)
                } else {
                    viewModel.setCustomLimit(app, customMinutes, isEnabled = true)
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
        val (app, minutes) = highImpactAppPending!!
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
                        text = "This application has been identified as a high-impact distraction. Once added, access will be restricted according to your custom daily allowance of ${if (minutes == 0) "0 minutes (Always Blocked)" else "$minutes minutes/day"} and active focus schedules.",
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
                        viewModel.setCustomLimit(app, minutes, isEnabled = true)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isLimited) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Avatar / Initial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (app.isHighImpact) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = if (app.isHighImpact) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isHighImpact) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("High-Impact", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.error
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (app.isLimited) {
                    val limitText = if (app.dailyLimitMinutes == 0) {
                        "Always Blocked (0 min/day)"
                    } else {
                        "Limit: ${app.dailyLimitMinutes} min/day"
                    }
                    Text(
                        text = limitText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Tap to set custom time limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            IconButton(onClick = onConfigureClick) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configure custom limit",
                    tint = if (app.isLimited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
    onSaveLimit: (Int) -> Unit,
    onRemoveLimit: () -> Unit
) {
    val initialMinutes = if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes else 30
    var minutes by remember { mutableIntStateOf(initialMinutes) }
    var textInput by remember { mutableStateOf(initialMinutes.toString()) }

    val presetOptions = listOf(
        0 to "Always Block (0m)",
        15 to "15m",
        30 to "30m",
        45 to "45m",
        60 to "1h",
        90 to "1.5h",
        120 to "2h"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Custom Time Limit",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                text = "High-impact distracting app. Setting a strict daily limit helps reclaim focused time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Presets Flow / Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetOptions.take(4).forEach { (presetMin, label) ->
                            FilterChip(
                                selected = (minutes == presetMin),
                                onClick = {
                                    minutes = presetMin
                                    textInput = presetMin.toString()
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetOptions.drop(4).forEach { (presetMin, label) ->
                            FilterChip(
                                selected = (minutes == presetMin),
                                onClick = {
                                    minutes = presetMin
                                    textInput = presetMin.toString()
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveLimit(minutes)
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
