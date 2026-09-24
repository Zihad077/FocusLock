package com.example.presentation.escape

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.liquidGlass
import com.example.database.UserSettings
import com.example.database.isPremiumActive
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
    val primaryCyan = Color(0xFF24DFEC)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Escape Prevention",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
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
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Strengthen FocusLock against bypass attempts. System permissions enforce real-time reliability.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                        statusColor = if (s.notificationProtectionEnabled && !hasDndAccess) Color(0xFFFF5252) else primaryCyan
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
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
    statusColor: Color = Color(0xFF24DFEC)
) {
    val primaryCyan = Color(0xFF24DFEC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), isHighlight = checked)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (checked) primaryCyan.copy(alpha = 0.2f)
                            else Color(0x283E4C5E)
                        )
                        .border(
                            1.dp,
                            if (checked) primaryCyan.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.25f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (checked) primaryCyan else Color.White,
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = statusColor
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = primaryCyan,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.25f)
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
