package com.example.presentation.escape

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.UserSettings
import com.example.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscapeScreen(
    onNavigateBack: () -> Unit,
    viewModel: EscapeViewModel = viewModel(
        factory = EscapeViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Escape Prevention", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Strengthen FocusLock against bypass attempts. Some features depend on system permissions or OS limits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            settings?.let { s ->
                item {
                    val hasDndAccess = PermissionHelper.hasNotificationPolicyAccess(context)
                    PreventionCard(
                        title = "Notification Protection",
                        description = "Suppress distracting notifications when Focus Mode is active using Do Not Disturb.",
                        icon = Icons.Default.NotificationsOff,
                        checked = s.notificationProtectionEnabled,
                        onCheckedChange = { 
                            if (!hasDndAccess && it) {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } else {
                                viewModel.updateSettings(s.copy(notificationProtectionEnabled = it))
                            }
                        },
                        statusText = if (s.notificationProtectionEnabled && !hasDndAccess) "Permission Required" 
                                     else if (s.notificationProtectionEnabled) "Enabled" 
                                     else "Disabled",
                        statusColor = if (s.notificationProtectionEnabled && !hasDndAccess) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    PreventionCard(
                        title = "Anti-Delete Protection",
                        description = "Require the configured verification method before allowing app settings to be changed.",
                        icon = Icons.Default.VpnKey,
                        checked = s.antiDeleteProtectionEnabled,
                        onCheckedChange = { viewModel.updateSettings(s.copy(antiDeleteProtectionEnabled = it)) },
                        statusText = if (s.antiDeleteProtectionEnabled) "Protected" else "Disabled"
                    )
                }

                item {
                    PreventionCard(
                        title = "Stable Lock Mode",
                        description = "Ensure limits resume correctly after device restarts or process interruptions.",
                        icon = Icons.Default.Security,
                        checked = s.stableLockModeEnabled,
                        onCheckedChange = { viewModel.updateSettings(s.copy(stableLockModeEnabled = it)) },
                        statusText = if (s.stableLockModeEnabled) "Active" else "Disabled"
                    )
                }

                item {
                    PreventionCard(
                        title = "Permission Protection",
                        description = "Alert immediately if a critical system permission is revoked by the OS.",
                        icon = Icons.Default.Shield,
                        checked = s.permissionProtectionEnabled,
                        onCheckedChange = { viewModel.updateSettings(s.copy(permissionProtectionEnabled = it)) },
                        statusText = if (s.permissionProtectionEnabled) "Monitoring" else "Disabled"
                    )
                }

                item {
                    PreventionCard(
                        title = "Escape Attempt Detection",
                        description = "Log bypass attempts (e.g. force stops, permission revokes) in Statistics.",
                        icon = Icons.Default.Warning,
                        checked = s.escapeAttemptDetectionEnabled,
                        onCheckedChange = { viewModel.updateSettings(s.copy(escapeAttemptDetectionEnabled = it)) },
                        statusText = if (s.escapeAttemptDetectionEnabled) "Enabled" else "Disabled"
                    )
                }

                item {
                    PreventionCard(
                        title = "Automatic Service Recovery",
                        description = "Attempt to restart monitoring automatically if closed unexpectedly.",
                        icon = Icons.Default.Sync,
                        checked = s.autoServiceRecoveryEnabled,
                        onCheckedChange = { viewModel.updateSettings(s.copy(autoServiceRecoveryEnabled = it)) },
                        statusText = if (s.autoServiceRecoveryEnabled) "Enabled" else "Disabled"
                    )
                }
                
                item {
                    PreventionCard(
                        title = "Focus Protection",
                        description = "Automatically enforce all the above protections whenever Focus Mode is active.",
                        icon = Icons.Default.VerifiedUser,
                        checked = s.focusProtectionEnabled,
                        onCheckedChange = { viewModel.updateSettings(s.copy(focusProtectionEnabled = it)) },
                        statusText = if (s.focusProtectionEnabled) "Enabled" else "Disabled"
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun PreventionCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = statusText, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
