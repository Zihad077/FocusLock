package com.example.presentation.settings
import com.example.database.isPremiumActive

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ui.theme.BackgroundThemeType
import com.example.ui.theme.ThemePreviewCard
import com.example.ui.theme.liquidGlass
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.DataBackupManager
import android.widget.Toast
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApps: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToEscapePrevention: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToDataBackup: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
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

    val currentBgTheme = remember(settings.theme) { BackgroundThemeType.fromKey(settings.theme) }
    val themeLabel = "${currentBgTheme.titleEn} • ${currentBgTheme.titleBn}"

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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
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
            contentPadding = PaddingValues(top = 4.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(stringResource(R.string.account_section)) {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.profile_level),
                        subtitle = "Level ${settings.level} • ${settings.xp} XP",
                        onClick = { showProfileDialog = true }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.preferences_section)) {
                    SettingsRow(
                        icon = Icons.Default.Wallpaper,
                        title = stringResource(R.string.appearance),
                        subtitle = themeLabel,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.notifications),
                        subtitle = stringResource(R.string.notifications_subtitle),
                        onClick = { showNotificationDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.language),
                        subtitle = languageLabel,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.features_section)) {
                    SettingsRow(
                        icon = Icons.Default.FitnessCenter,
                        title = stringResource(R.string.anti_distraction_challenges),
                        subtitle = stringResource(R.string.anti_distraction_challenges_subtitle),
                        onClick = { showChallengeDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = stringResource(R.string.focus_mode_setting),
                        subtitle = if (settings.isFocusModeActive) stringResource(R.string.focus_mode_subtitle_active) else stringResource(R.string.focus_mode_subtitle_idle),
                        onClick = onNavigateToFocus
                    )
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.app_limits_schedules),
                        subtitle = stringResource(R.string.app_limits_schedules_subtitle),
                        onClick = onNavigateToApps
                    )
                    SettingsRow(
                        icon = Icons.Default.MedicalServices,
                        title = stringResource(R.string.emergency_unlock_setting),
                        subtitle = "${settings.emergencyUnlocksRemaining} of ${settings.maxEmergencyUnlocks} remaining today",
                        onClick = { showEmergencyDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.AdminPanelSettings,
                        title = stringResource(R.string.permission_center),
                        subtitle = stringResource(R.string.permission_center_subtitle),
                        onClick = onNavigateToPermissions
                    )
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.escape_prevention),
                        subtitle = stringResource(R.string.escape_prevention_subtitle),
                        onClick = { onNavigateToEscapePrevention() }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.premium_section)) {
                    SettingsRow(
                        icon = Icons.Default.Star,
                        title = stringResource(R.string.focuslock_premium),
                        subtitle = if (settings.isPremiumActive) stringResource(R.string.premium_active) else stringResource(R.string.premium_inactive),
                        onClick = onNavigateToPremium
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.data_management_section)) {
                    SettingsRow(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.data_backup),
                        subtitle = stringResource(R.string.data_backup_subtitle),
                        onClick = { onNavigateToDataBackup() }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.about_section)) {
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.privacy_policy_subtitle),
                        onClick = { showPrivacyDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about_app),
                        subtitle = stringResource(R.string.about_app_subtitle),
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            // 320x50 Banner near bottom (Zero ads for premium)
            item {
                LiquidGlassAdaptiveBanner(isPremium = settings.isPremiumActive)
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // --- DIALOGS ---

    // 1. Background Theme Chooser Dialog (Visual Animated Live Preview Gallery)
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            modifier = Modifier.liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true),
            containerColor = Color.Transparent,
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Animated Themes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "লাইভ অ্যানিমেটেড দৃশ্য নির্বাচন করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00E5FF).copy(alpha = 0.9f)
                    )
                }
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(BackgroundThemeType.entries) { theme ->
                        ThemePreviewCard(
                            theme = theme,
                            isSelected = currentBgTheme == theme,
                            onClick = {
                                viewModel.updateTheme(theme.key)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(
                        text = stringResource(R.string.done),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontSize = 15.sp
                    )
                }
            }
        )
    }

    // 2. Language Dialog (Liquid Glass Style with Instant Locale Switcher)
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            modifier = Modifier.liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true),
            containerColor = Color.Transparent,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.choose_language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            text = {
                val languages = listOf(
                    Triple("bn", "বাংলা", "Bengali"),
                    Triple("en", "English", "English (US/UK)"),
                    Triple("es", "Español", "Spanish"),
                    Triple("fr", "Français", "French"),
                    Triple("de", "Deutsch", "German"),
                    Triple("pt", "Português", "Portuguese"),
                    Triple("hi", "हिन्दी", "Hindi"),
                    Triple("ar", "العربية", "Arabic")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(languages.size) { index ->
                        val (code, nativeName, englishName) = languages[index]
                        val isSelected = (settings.language == code)

                        Surface(
                            onClick = {
                                viewModel.updateLanguage(code)
                                com.example.util.LocaleHelper.applyLocale(context, code, recreateActivity = true)
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0x3300E5FF) else Color(0x22132338),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = nativeName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = englishName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateLanguage(code)
                                        com.example.util.LocaleHelper.applyLocale(context, code, recreateActivity = true)
                                        showLanguageDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF00E5FF)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(
                        text = stringResource(R.string.close),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        )
    }

    // 3. Notification Dialog
    if (showNotificationDialog) {
        val primaryCyan = Color(0xFF24DFEC)
        var notificationSentMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            containerColor = Color(0xFF162534),
            title = { Text("Notification Settings", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "FocusLock uses notifications to alert you when daily limits are approached or when deep focus sessions complete.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Button(
                        onClick = {
                            viewModel.sendTestNotification()
                            notificationSentMessage = "Test notification dispatched!"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryCyan,
                            contentColor = Color(0xFF0C1929)
                        )
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("System Notification Settings")
                    }

                    if (notificationSentMessage != null) {
                        Text(
                            text = notificationSentMessage!!,
                            color = primaryCyan,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Done", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 4. Emergency Unlock Dialog
    if (showEmergencyDialog) {
        val primaryCyan = Color(0xFF24DFEC)
        var successNotice by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            containerColor = Color(0xFF162534),
            title = { Text("Emergency Unlock", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Emergency unlocks provide immediate 5-minute access to a blocked app without completing a challenge. Daily allowances prevent habit regression.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x283E4C5E)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Today's Unlocks Remaining",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                            Text(
                                "${settings.emergencyUnlocksRemaining} / ${settings.maxEmergencyUnlocks}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryCyan
                            )
                        }
                    }

                    Text("Max Unlocks Per Day:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(1, 2, 3, 5).forEach { count ->
                            val isSelected = (settings.maxEmergencyUnlocks == count)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateMaxEmergencyUnlocks(count) },
                                label = { Text("$count / day") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = primaryCyan,
                                    labelColor = Color.White.copy(alpha = 0.75f)
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.resetEmergencyUnlocksToday()
                            successNotice = "Unlocks replenished for today!"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryCyan,
                            contentColor = Color(0xFF0C1929)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Replenish Today's Unlocks", fontWeight = FontWeight.Bold)
                    }

                    if (successNotice != null) {
                        Text(
                            text = successNotice!!,
                            color = primaryCyan,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Close", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val profileImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val validation = DataBackupManager.validateBackupFromUri(context, uri)
                    if (validation.isValid && validation.parsedBackup != null) {
                        val success = DataBackupManager.restoreBackup(viewModel.repository, validation.parsedBackup)
                        if (success) {
                            Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to restore backup data", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, validation.errorMessage ?: "Invalid backup file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 5. Profile & Gamification Dialog
    if (showProfileDialog) {
        val primaryCyan = Color(0xFF24DFEC)
        var showResetConfirm by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = Color(0xFF162534),
            title = { Text("Profile & Progress", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(primaryCyan.copy(alpha = 0.2f))
                                .border(1.dp, primaryCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Level ${settings.level} Achiever",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "${settings.currentStreak} day streak 🔥",
                                style = MaterialTheme.typography.bodyMedium,
                                color = primaryCyan
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Experience (XP)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                            Text("${settings.xp} / ${settings.level * 100} XP", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        val progress = (settings.xp.toFloat() / (settings.level * 100).toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = primaryCyan,
                            trackColor = Color(0x303E4F63)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Text(
                        "Data Backup & Transfer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val file = DataBackupManager.exportBackupFile(context, viewModel.repository)
                                        DataBackupManager.shareBackupFile(context, file)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryCyan,
                                contentColor = Color(0xFF0C1929)
                            )
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                profileImportLauncher.launch("application/json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import")
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    if (!showResetConfirm) {
                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Gamification Stats")
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x35551822)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80EF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Reset all XP, streak, and level to 1?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.resetProgress()
                                            showResetConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                                    ) {
                                        Text("Confirm Reset")
                                    }
                                    TextButton(onClick = { showResetConfirm = false }) {
                                        Text("Cancel", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 6. Verification Settings Dialog
    if (showChallengeDialog) {
        val primaryCyan = Color(0xFF24DFEC)

        AlertDialog(
            onDismissRequest = { showChallengeDialog = false },
            containerColor = Color(0xFF162534),
            title = { Text("Verification Settings", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Text("Enable verification methods for temporary unlocks.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Quick Mind Challenge", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Switch(
                                checked = settings.mindChallengeEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(mindChallengeEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0C1929), checkedTrackColor = primaryCyan)
                            )
                        }
                        Text("Math and logic questions.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.12f))
                    }
                    
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Focus Challenge", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Switch(
                                checked = settings.focusChallengeEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(focusChallengeEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0C1929), checkedTrackColor = primaryCyan)
                            )
                        }
                        Text("Wait on screen for a countdown.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.12f))
                    }
                    
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Typing Challenge", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Switch(
                                checked = settings.typingChallengeEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(typingChallengeEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0C1929), checkedTrackColor = primaryCyan)
                            )
                        }
                        Text("Type a phrase correctly.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.12f))
                    }
                    
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("PIN Unlock", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Switch(
                                checked = settings.pinUnlockEnabled,
                                onCheckedChange = { viewModel.updateSettings(settings.copy(pinUnlockEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0C1929), checkedTrackColor = primaryCyan)
                            )
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
                                label = { Text("Set PIN", color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.12f))
                    }
                    
                    item {
                        Text("Challenge Difficulty", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        val difficulties = listOf("EASY", "NORMAL", "HARD")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            difficulties.forEach { diff ->
                                FilterChip(
                                    selected = settings.difficulty == diff,
                                    onClick = { viewModel.updateSettings(settings.copy(difficulty = diff)) },
                                    label = { Text(diff) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryCyan.copy(alpha = 0.25f),
                                        selectedLabelColor = primaryCyan,
                                        labelColor = Color.White.copy(alpha = 0.75f)
                                    )
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.12f))
                    }
                    
                    item {
                        Text("Temporary Unlock Duration (Minutes)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        val durations = listOf(5, 10, 15)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            durations.forEach { duration ->
                                FilterChip(
                                    selected = settings.tempUnlockDurationMinutes == duration,
                                    onClick = { viewModel.updateSettings(settings.copy(tempUnlockDurationMinutes = duration)) },
                                    label = { Text("$duration") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryCyan.copy(alpha = 0.25f),
                                        selectedLabelColor = primaryCyan,
                                        labelColor = Color.White.copy(alpha = 0.75f)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChallengeDialog = false }) {
                    Text("Close", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 7. Privacy Dialog
    if (showPrivacyDialog) {
        val primaryCyan = Color(0xFF24DFEC)

        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = Color(0xFF162534),
            title = { Text("Privacy & Data Safety", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "• 100% Offline & Private: FocusLock does not collect or upload personal usage data, keystrokes, or screen contents to remote servers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        "• Local Storage: All time limits, schedules, and usage statistics are stored strictly on-device in a secure SQLite/Room database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        "• Accessibility Service: Used exclusively to identify the current foreground application package in order to display the block screen when limits are exceeded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        "• Zero telemetry or third-party ad tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("I Understand", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 8. About Dialog
    if (showAboutDialog) {
        val primaryCyan = Color(0xFF24DFEC)

        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color(0xFF162534),
            title = { Text("About FocusLock", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryCyan.copy(alpha = 0.2f))
                                .border(1.dp, primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = primaryCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("FocusLock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Version 1.0.0 (Build 1)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                        }
                    }

                    Text(
                        "A privacy-first digital wellbeing companion built to help you overcome doomscrolling, build healthy digital boundaries, and protect deep focus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryCyan,
                            contentColor = Color(0xFF0C1929)
                        )
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accessibility Settings", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.QueryStats, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Usage Access Settings")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 9. System Permissions Manager Dialog
    if (showPermissionsDialog) {
        val primaryCyan = Color(0xFF24DFEC)
        val hasUsage = com.example.util.PermissionHelper.hasUsageAccess(context)
        val hasOverlay = com.example.util.PermissionHelper.hasOverlayPermission(context)
        val hasAccessibility = com.example.util.PermissionHelper.hasAccessibilityPermission(context)

        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            containerColor = Color(0xFF162534),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = primaryCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permissions Manager", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "FocusLock requires these system permissions to enforce mindful boundaries and protect deep work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    // 1. Overlay
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasOverlay) Color(0x283E4C5E)
                            else Color(0x35551822)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasOverlay) Color.White.copy(alpha = 0.15f) else Color(0x80EF4444)
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
                                Text("Display Over Other Apps", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text(
                                    if (hasOverlay) "Active & Granted" else "Required for lock screen overlay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasOverlay) primaryCyan else Color(0xFFFF5252)
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
                                    Text("Grant", color = primaryCyan, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryCyan)
                            }
                        }
                    }

                    // 2. Usage Access
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasUsage) Color(0x283E4C5E)
                            else Color(0x35551822)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasUsage) Color.White.copy(alpha = 0.15f) else Color(0x80EF4444)
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
                                Text("Usage Access", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text(
                                    if (hasUsage) "Active & Granted" else "Required to calculate screen time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasUsage) primaryCyan else Color(0xFFFF5252)
                                )
                            }
                            if (!hasUsage) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Grant", color = primaryCyan, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryCyan)
                            }
                        }
                    }

                    // 3. Accessibility Service
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasAccessibility) Color(0x283E4C5E)
                            else Color(0x35551822)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasAccessibility) Color.White.copy(alpha = 0.15f) else Color(0x80EF4444)
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
                                Text("Accessibility Service", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                Text(
                                    if (hasAccessibility) "Active & Enforcing" else "Zero-latency instant app detection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasAccessibility) primaryCyan else Color(0xFFFF5252)
                                )
                            }
                            if (!hasAccessibility) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Enable", color = primaryCyan, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text("Done", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun applyLocale(context: Context, languageCode: String) {
    com.example.util.LocaleHelper.applyLocale(context, languageCode, recreateActivity = true)
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val primaryCyan = Color(0xFF24DFEC)
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(primaryCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = primaryCyan
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(26.dp),
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x283E4C5E))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF24DFEC),
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                ),
                color = Color.White
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}
