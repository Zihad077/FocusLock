package com.example.presentation.settings

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.ads.AdsManager
import com.example.ui.theme.liquidGlass
import com.example.util.DataExportHelper
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApps: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToEscapePrevention: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showChallengeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    val themeLabel = when (settings.theme) {
        "DARK" -> "Dark mode"
        "LIGHT" -> "Light mode"
        else -> "System default"
    }

    val languageLabel = when (settings.language) {
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "pt" -> "Português"
        "hi" -> "हिन्दी (Hindi)"
        "bn" -> "বাংলা (Bengali)"
        "ar" -> "العربية (Arabic)"
        else -> "English"
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
            item {
                SettingsSection("ACCOUNT") {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "Profile & Level",
                        subtitle = "Level ${settings.level} • ${settings.xp} XP",
                        onClick = { showProfileDialog = true }
                    )
                }
            }
            
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
            item {
                SettingsSection("PREFERENCES") {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "Appearance",
                        subtitle = themeLabel,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Alerts & reminders",
                        onClick = { showNotificationDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = languageLabel,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }
            
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
            item {
                SettingsSection("FEATURES") {
                    SettingsRow(
                        icon = Icons.Default.FitnessCenter,
                        title = "Anti-Distraction Challenges",
                        subtitle = "Practice mindful typing unlock",
                        onClick = { showChallengeDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = "Focus Mode",
                        subtitle = if (settings.isFocusModeActive) "Active session running" else "Configure deep work sessions",
                        onClick = onNavigateToFocus
                    )
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "App Limits & Schedules",
                        subtitle = "Manage restricted apps",
                        onClick = onNavigateToApps
                    )
                    SettingsRow(
                        icon = Icons.Default.MedicalServices,
                        title = "Emergency Unlock",
                        subtitle = "${settings.emergencyUnlocksRemaining} of ${settings.maxEmergencyUnlocks} remaining today",
                        onClick = { showEmergencyDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "System Permissions Manager",
                        subtitle = "Verify overlay, usage & accessibility permissions",
                        onClick = { showPermissionsDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = "Escape Prevention",
                        subtitle = "Configure anti-delete & stable lock modes",
                        onClick = { onNavigateToEscapePrevention() }
                    )
                }
            }
            
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                    SettingsRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Ad & Privacy Preferences",
                        subtitle = "Manage Google UMP consent & targeted ads preferences",
                        onClick = {
                            (context as? Activity)?.let { act ->
                                AdsManager.showPrivacyOptionsForm(act)
                            }
                        }
                    )
                }
            }
            item {
                SettingsSection("ABOUT") {
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = "Privacy & Data Safety",
                        subtitle = "100% on-device & private",
                        onClick = { showPrivacyDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "About FocusLock",
                        subtitle = "Version 1.0.0 (Build 1)",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // --- DIALOGS ---

    // 1. Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = listOf(
                        "SYSTEM" to "System Default",
                        "LIGHT" to "Light Mode",
                        "DARK" to "Dark Mode"
                    )
                    themes.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateTheme(code)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.theme == code),
                                onClick = {
                                    viewModel.updateTheme(code)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 2. Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose Language", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    val languages = listOf(
                        "en" to "English",
                        "es" to "Español (Spanish)",
                        "fr" to "Français (French)",
                        "de" to "Deutsch (German)",
                        "pt" to "Português (Portuguese)",
                        "hi" to "हिन्दी (Hindi)",
                        "bn" to "বাংলা (Bengali)",
                        "ar" to "العربية (Arabic)"
                    )
                    items(languages.size) { index ->
                        val (code, label) = languages[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateLanguage(code)
                                    applyLocale(context, code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.language == code),
                                onClick = {
                                    viewModel.updateLanguage(code)
                                    applyLocale(context, code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 3. Notification Dialog
    if (showNotificationDialog) {
        var notificationSentMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "FocusLock uses notifications to alert you when daily limits are approached or when deep focus sessions complete.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            viewModel.sendTestNotification()
                            notificationSentMessage = "Test notification dispatched!"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification")
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("System Notification Settings")
                    }

                    if (notificationSentMessage != null) {
                        Text(
                            text = notificationSentMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // 4. Emergency Unlock Dialog
    if (showEmergencyDialog) {
        var successNotice by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("Emergency Unlock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Emergency unlocks provide immediate 5-minute access to a blocked app without completing a challenge. Daily allowances prevent habit regression.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Today's Unlocks Remaining",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${settings.emergencyUnlocksRemaining} / ${settings.maxEmergencyUnlocks}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text("Max Unlocks Per Day:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(1, 2, 3, 5).forEach { count ->
                            val isSelected = (settings.maxEmergencyUnlocks == count)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateMaxEmergencyUnlocks(count) },
                                label = { Text("$count / day") }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.resetEmergencyUnlocksToday()
                            successNotice = "Unlocks replenished for today!"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Replenish Today's Unlocks")
                    }

                    if (successNotice != null) {
                        Text(
                            text = successNotice!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 5. Profile & Gamification Dialog
    if (showProfileDialog) {
        var showResetConfirm by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Profile & Progress", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Level ${settings.level} Achiever",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${settings.currentStreak} day streak 🔥",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Experience (XP)", style = MaterialTheme.typography.bodySmall)
                            Text("${settings.xp} / ${settings.level * 100} XP", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        val progress = (settings.xp.toFloat() / (settings.level * 100).toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    HorizontalDivider()

                    if (!showResetConfirm) {
                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Gamification Stats")
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Reset all XP, streak, and level to 1?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.resetProgress()
                                            showResetConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Confirm Reset")
                                    }
                                    TextButton(onClick = { showResetConfirm = false }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 6. Verification Settings Dialog
    if (showChallengeDialog) {
        AlertDialog(
            onDismissRequest = { showChallengeDialog = false },
            title = { Text("Verification Settings", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
                    item {
                        Text("Enable verification methods for temporary unlocks.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Quick Mind Challenge", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Switch(checked = settings.mindChallengeEnabled, onCheckedChange = { viewModel.updateSettings(settings.copy(mindChallengeEnabled = it)) })
                        }
                        Text("Math and logic questions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Focus Challenge", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Switch(checked = settings.focusChallengeEnabled, onCheckedChange = { viewModel.updateSettings(settings.copy(focusChallengeEnabled = it)) })
                        }
                        Text("Wait on screen for a countdown.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Typing Challenge", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Switch(checked = settings.typingChallengeEnabled, onCheckedChange = { viewModel.updateSettings(settings.copy(typingChallengeEnabled = it)) })
                        }
                        Text("Type a phrase correctly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("PIN Unlock", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Switch(checked = settings.pinUnlockEnabled, onCheckedChange = { viewModel.updateSettings(settings.copy(pinUnlockEnabled = it)) })
                        }
                        if (settings.pinUnlockEnabled) {
                            var pinInput by remember { mutableStateOf(settings.pinHash) }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { 
                                    pinInput = it
                                    viewModel.updateSettings(settings.copy(pinHash = it))
                                },
                                label = { Text("Set PIN") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
                    item {
                        Text("Challenge Difficulty", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        val difficulties = listOf("EASY", "NORMAL", "HARD")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            difficulties.forEach { diff ->
                                FilterChip(
                                    selected = settings.difficulty == diff,
                                    onClick = { viewModel.updateSettings(settings.copy(difficulty = diff)) },
                                    label = { Text(diff) }
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    
            item {
                SettingsSection("Data & Privacy") {
                    SettingsRow(
                        icon = androidx.compose.material.icons.Icons.Default.Share,
                        title = "Export Data (CSV)",
                        subtitle = "Save a local copy of your usage data",
                        onClick = { scope.launch { DataExportHelper.exportDataAsCsv(context, viewModel.repository) } }
                    )
                }
            }
                    item {
                        Text("Temporary Unlock Duration (Minutes)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        val durations = listOf(5, 10, 15)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            durations.forEach { duration ->
                                FilterChip(
                                    selected = settings.tempUnlockDurationMinutes == duration,
                                    onClick = { viewModel.updateSettings(settings.copy(tempUnlockDurationMinutes = duration)) },
                                    label = { Text("$duration") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChallengeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 7. Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Data Safety", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "• 100% Offline & Private: FocusLock does not collect or upload personal usage data, keystrokes, or screen contents to remote servers.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Local Storage: All time limits, schedules, and usage statistics are stored strictly on-device in a secure SQLite/Room database.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Accessibility Service: Used exclusively to identify the current foreground application package in order to display the block screen when limits are exceeded.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Zero telemetry or third-party ad tracking.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("I Understand")
                }
            }
        )
    }

    // 8. About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About FocusLock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("FocusLock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Version 1.0.0 (Build 1)", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text(
                        "A privacy-first digital wellbeing companion built to help you overcome doomscrolling, build healthy digital boundaries, and protect deep focus.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accessibility Settings")
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QueryStats, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Usage Access Settings")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 9. System Permissions Manager Dialog
    if (showPermissionsDialog) {
        val hasUsage = com.example.util.PermissionHelper.hasUsageAccess(context)
        val hasOverlay = com.example.util.PermissionHelper.hasOverlayPermission(context)
        val hasAccessibility = com.example.util.PermissionHelper.hasAccessibilityPermission(context)

        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permissions Manager", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "FocusLock requires these system permissions to enforce mindful boundaries and protect deep work.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // 1. Overlay
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasOverlay) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Display Over Other Apps", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (hasOverlay) "Active & Granted" else "Required for lock screen overlay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            if (!hasOverlay) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Grant")
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // 2. Usage Access
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasUsage) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Usage Access", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (hasUsage) "Active & Granted" else "Required to calculate screen time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasUsage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            if (!hasUsage) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Grant")
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // 3. Accessibility Service
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasAccessibility) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Accessibility Service", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (hasAccessibility) "Active & Enforcing" else "Zero-latency instant app detection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasAccessibility) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            if (!hasAccessibility) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Enable")
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

private fun applyLocale(context: Context, languageCode: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? android.app.LocaleManager
            localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(languageCode)
        } else {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val res = context.resources
            val config = res.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
        }
    } catch (e: Exception) {
        // Fallback
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(24.dp),
                    isElevated = false
                )
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Next",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
        )
    }
}
