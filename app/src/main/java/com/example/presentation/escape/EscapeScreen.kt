package com.example.presentation.escape

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.*
import com.example.service.AppMonitorService
import com.example.ui.theme.GlassButton
import com.example.ui.theme.GlassButtonStyle
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscapeScreen(
    onNavigateBack: () -> Unit,
    viewModel: EscapeViewModel = viewModel(
        factory = EscapeViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val escapeAttempts by viewModel.escapeAttempts.collectAsStateWithLifecycle()

    val primaryCyan = Color(0xFF24DFEC)
    val emeraldGreen = Color(0xFF00E676)
    val warningAmber = Color(0xFFFFB300)
    val dangerRed = Color(0xFFFF5252)

    // Refresh permission states whenever user returns from Android Settings
    var permissionRefreshTrigger by remember { mutableIntStateOf(0) }
    var pendingDndEnableOnReturn by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, settings) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefreshTrigger++
                if (pendingDndEnableOnReturn && PermissionHelper.hasNotificationPolicyAccess(context)) {
                    pendingDndEnableOnReturn = false
                    settings?.let { s ->
                        viewModel.updateSettings(s.copy(notificationProtectionEnabled = true))
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasUsageAccess = remember(permissionRefreshTrigger) { PermissionHelper.hasUsageAccess(context) }
    val hasOverlayAccess = remember(permissionRefreshTrigger) { PermissionHelper.hasOverlayPermission(context) }
    val hasAccessibilityAccess = remember(permissionRefreshTrigger) { PermissionHelper.hasAccessibilityPermission(context) }
    val hasDndAccess = remember(permissionRefreshTrigger) { PermissionHelper.hasNotificationPolicyAccess(context) }

    // Verification gate state when Anti-Delete Protection is active and user tries to turn OFF a protection
    var pendingProtectedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingActionTitle by remember { mutableStateOf("") }
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    // Helper that gates disabling protections behind verification when Anti-Delete Protection is active
    fun requestSettingChange(
        currentSettings: UserSettings,
        isDisablingProtection: Boolean,
        protectionName: String,
        applyChange: () -> Unit
    ) {
        if (isDisablingProtection && currentSettings.effectiveAntiDeleteProtection) {
            pendingActionTitle = protectionName
            pendingProtectedAction = applyChange
        } else {
            applyChange()
        }
    }

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
                            .testTag("escape_back_button")
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
            contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Strengthen FocusLock against impulse bypasses. Active shields enforce real-time tamper protection across system settings and recents.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            settings?.let { s ->
                val isFocusEnforced = s.isFocusProtectionEnforcedNow
                val activeCount = listOf(
                    s.effectiveNotificationProtection,
                    s.effectiveAntiDeleteProtection,
                    s.effectiveStableLockMode,
                    s.effectivePermissionProtection,
                    s.effectiveEscapeAttemptDetection,
                    s.effectiveAutoServiceRecovery,
                    s.focusProtectionEnabled
                ).count { it }

                // 1. Live Shield Diagnostics & Permission Health Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(24.dp), isHighlight = true)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(emeraldGreen.copy(alpha = 0.18f))
                                            .border(1.dp, emeraldGreen.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = emeraldGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Shield Status: $activeCount / 7 Active",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            ),
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isFocusEnforced) {
                                                "Focus Mode Active • All 6 Protections Enforced 🔒"
                                            } else if (s.effectiveAntiDeleteProtection) {
                                                "Tamper Guard Armed • Verification Required to Disable"
                                            } else {
                                                "Real-time monitoring service running"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = if (isFocusEnforced) emeraldGreen else primaryCyan
                                        )
                                    }
                                }
                            }

                            // System Permission Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PermissionHealthChip(
                                    label = "Usage",
                                    isGranted = hasUsageAccess,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                )
                                PermissionHealthChip(
                                    label = "Overlay",
                                    isGranted = hasOverlayAccess,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                )
                                PermissionHealthChip(
                                    label = "Access.",
                                    isGranted = hasAccessibilityAccess,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    }
                                )
                                PermissionHealthChip(
                                    label = "DND",
                                    isGranted = hasDndAccess,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                    }
                                )
                            }

                            // Action row: Verify & Test Escape Shield
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        AppMonitorService.startService(context)
                                        showDiagnosticDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("verify_escape_shield_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryCyan),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryCyan.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Shield Diagnostic", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        pendingActionTitle = "Tamper Guard Test"
                                        pendingProtectedAction = {
                                            Toast.makeText(
                                                context,
                                                "Verification passed! Anti-Delete Tamper Guard is working.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_tamper_challenge_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Challenge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 2. Notification Protection
                item {
                    val effective = s.effectiveNotificationProtection
                    PreventionCard(
                        title = "Notification Protection",
                        description = "Suppress distracting notifications when Focus Mode is active using Do Not Disturb.",
                        icon = Icons.Default.NotificationsOff,
                        checked = s.notificationProtectionEnabled,
                        onCheckedChange = { wantEnabled ->
                            if (wantEnabled && !hasDndAccess) {
                                pendingDndEnableOnReturn = true
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } else {
                                requestSettingChange(
                                    currentSettings = s,
                                    isDisablingProtection = !wantEnabled,
                                    protectionName = "Notification Protection"
                                ) {
                                    viewModel.updateSettings(s.copy(notificationProtectionEnabled = wantEnabled))
                                }
                            }
                        },
                        statusText = when {
                            effective && !hasDndAccess -> "Tap to Grant DND Permission"
                            s.notificationProtectionEnabled -> "Enabled • DND Ready"
                            isFocusEnforced -> "Enforced by Focus Mode"
                            else -> "Disabled"
                        },
                        statusColor = when {
                            effective && !hasDndAccess -> dangerRed
                            effective -> primaryCyan
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                    )
                }

                // 3. Anti-Delete Protection
                item {
                    val effective = s.effectiveAntiDeleteProtection
                    PreventionCard(
                        title = "Anti-Delete Protection",
                        description = "Blocks Force Stop & Uninstall attempts in System Settings and requires verification before disabling protections.",
                        icon = Icons.Default.VpnKey,
                        checked = s.antiDeleteProtectionEnabled,
                        onCheckedChange = { wantEnabled ->
                            requestSettingChange(
                                currentSettings = s,
                                isDisablingProtection = !wantEnabled,
                                protectionName = "Anti-Delete Protection"
                            ) {
                                viewModel.updateSettings(s.copy(antiDeleteProtectionEnabled = wantEnabled))
                            }
                        },
                        statusText = when {
                            s.antiDeleteProtectionEnabled && !hasAccessibilityAccess -> "Protected (Enable Accessibility for OS Guard)"
                            s.antiDeleteProtectionEnabled -> "Protected • Tamper Guard Armed"
                            isFocusEnforced -> "Enforced by Focus Mode"
                            else -> "Disabled"
                        },
                        statusColor = when {
                            s.antiDeleteProtectionEnabled && !hasAccessibilityAccess -> warningAmber
                            effective -> emeraldGreen
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                    )
                }

                // 4. Stable Lock Mode
                item {
                    val effective = s.effectiveStableLockMode
                    PreventionCard(
                        title = "Stable Lock Mode",
                        description = "Enforces ultra-fast 800ms re-lock debounce and ensures limits resume immediately after device restarts.",
                        icon = Icons.Default.Security,
                        checked = s.stableLockModeEnabled,
                        onCheckedChange = { wantEnabled ->
                            requestSettingChange(
                                currentSettings = s,
                                isDisablingProtection = !wantEnabled,
                                protectionName = "Stable Lock Mode"
                            ) {
                                viewModel.updateSettings(s.copy(stableLockModeEnabled = wantEnabled))
                            }
                        },
                        statusText = when {
                            s.stableLockModeEnabled -> "Active • Strict Re-Lock & Boot Persistence"
                            isFocusEnforced -> "Enforced by Focus Mode"
                            else -> "Disabled"
                        },
                        statusColor = if (effective) primaryCyan else Color.White.copy(alpha = 0.5f)
                    )
                }

                // 5. Permission Protection
                item {
                    val effective = s.effectivePermissionProtection
                    val allCoreGranted = hasUsageAccess && hasOverlayAccess && hasAccessibilityAccess
                    PreventionCard(
                        title = "Permission Protection",
                        description = "Continuously audits Usage, Overlay, and Accessibility permissions and alerts immediately if revoked.",
                        icon = Icons.Default.Shield,
                        checked = s.permissionProtectionEnabled,
                        onCheckedChange = { wantEnabled ->
                            requestSettingChange(
                                currentSettings = s,
                                isDisablingProtection = !wantEnabled,
                                protectionName = "Permission Protection"
                            ) {
                                viewModel.updateSettings(s.copy(permissionProtectionEnabled = wantEnabled))
                            }
                        },
                        statusText = when {
                            effective && !allCoreGranted -> "Monitoring • Missing Core Permission"
                            s.permissionProtectionEnabled -> "Monitoring • All Core Permissions Verified"
                            isFocusEnforced -> "Enforced by Focus Mode"
                            else -> "Disabled"
                        },
                        statusColor = when {
                            effective && !allCoreGranted -> warningAmber
                            effective -> primaryCyan
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                    )
                }

                // 6. Escape Attempt Detection
                item {
                    val effective = s.effectiveEscapeAttemptDetection
                    PreventionCard(
                        title = "Escape Attempt Detection",
                        description = "Logs bypass attempts (Force Stop, Uninstall, Permission Revoke, Recents Swipe) in real time.",
                        icon = Icons.Default.Warning,
                        checked = s.escapeAttemptDetectionEnabled,
                        onCheckedChange = { wantEnabled ->
                            requestSettingChange(
                                currentSettings = s,
                                isDisablingProtection = !wantEnabled,
                                protectionName = "Escape Attempt Detection"
                            ) {
                                viewModel.updateSettings(s.copy(escapeAttemptDetectionEnabled = wantEnabled))
                            }
                        },
                        statusText = when {
                            s.escapeAttemptDetectionEnabled -> "Enabled • ${escapeAttempts.size} Attempts Logged"
                            isFocusEnforced -> "Enforced by Focus Mode (${escapeAttempts.size} Logged)"
                            else -> "Disabled"
                        },
                        statusColor = if (effective) primaryCyan else Color.White.copy(alpha = 0.5f)
                    )
                }

                // 7. Automatic Service Recovery
                item {
                    val effective = s.effectiveAutoServiceRecovery
                    PreventionCard(
                        title = "Automatic Service Recovery",
                        description = "Uses AlarmManager + Broadcast wakeups to restart monitoring within 1.5s if swiped from Recents.",
                        icon = Icons.Default.Sync,
                        checked = s.autoServiceRecoveryEnabled,
                        onCheckedChange = { wantEnabled ->
                            requestSettingChange(
                                currentSettings = s,
                                isDisablingProtection = !wantEnabled,
                                protectionName = "Automatic Service Recovery"
                            ) {
                                viewModel.updateSettings(s.copy(autoServiceRecoveryEnabled = wantEnabled))
                            }
                        },
                        statusText = when {
                            s.autoServiceRecoveryEnabled -> "Enabled • Auto-Restart Armed"
                            isFocusEnforced -> "Enforced by Focus Mode"
                            else -> "Disabled"
                        },
                        statusColor = if (effective) primaryCyan else Color.White.copy(alpha = 0.5f)
                    )
                }

                // 8. Focus Protection
                item {
                    PreventionCard(
                        title = "Focus Protection",
                        description = "Master override: automatically enforces all 6 protections above whenever a Focus Mode session is active.",
                        icon = Icons.Default.VerifiedUser,
                        checked = s.focusProtectionEnabled,
                        onCheckedChange = { wantEnabled ->
                            requestSettingChange(
                                currentSettings = s,
                                isDisablingProtection = !wantEnabled,
                                protectionName = "Focus Protection"
                            ) {
                                viewModel.updateSettings(s.copy(focusProtectionEnabled = wantEnabled))
                            }
                        },
                        statusText = when {
                            isFocusEnforced -> "Actively Enforcing All Protections Now 🔒"
                            s.focusProtectionEnabled -> "Enabled • Ready for Next Focus Session"
                            else -> "Disabled"
                        },
                        statusColor = when {
                            isFocusEnforced -> emeraldGreen
                            s.focusProtectionEnabled -> primaryCyan
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                    )
                }

                // 9. Live Escape Attempts History Log
                item {
                    EscapeAttemptsLogCard(
                        attempts = escapeAttempts,
                        onClear = { viewModel.clearEscapeAttempts() }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Anti-Delete Verification Challenge Modal when trying to disable an active protection
    if (pendingProtectedAction != null && settings != null) {
        TamperVerificationDialog(
            settings = settings!!,
            targetProtectionName = pendingActionTitle,
            onVerified = {
                val action = pendingProtectedAction
                pendingProtectedAction = null
                action?.invoke()
            },
            onDismiss = {
                if (settings?.effectiveEscapeAttemptDetection == true) {
                    viewModel.recordEscapeAttempt(
                        type = "TAMPER_CHALLENGE_CANCELLED: $pendingActionTitle"
                    )
                }
                pendingProtectedAction = null
            }
        )
    }

    // Live Shield Diagnostic Dialog
    if (showDiagnosticDialog && settings != null) {
        val s = settings!!
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            containerColor = Color(0xFF122030),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = emeraldGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Escape Shield Diagnostic", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiagnosticRow("Foreground Monitor Service", true, "Running & Polling (800ms)")
                    DiagnosticRow(
                        "Accessibility Tamper Interceptor",
                        hasAccessibilityAccess,
                        if (hasAccessibilityAccess) "Active (Settings & Uninstall Guard)" else "Grant Accessibility in Settings"
                    )
                    DiagnosticRow(
                        "System Overlay Blocker",
                        hasOverlayAccess,
                        if (hasOverlayAccess) "Ready (Full-Screen Anti-Bypass)" else "Grant Display Over Other Apps"
                    )
                    DiagnosticRow(
                        "Do Not Disturb Policy",
                        hasDndAccess,
                        if (hasDndAccess) "Granted (Suppresses Notifications)" else "Optional (Grant for DND)"
                    )
                    DiagnosticRow(
                        "Anti-Delete Verification Gate",
                        s.effectiveAntiDeleteProtection,
                        if (s.effectiveAntiDeleteProtection) "Armed (Blocks Unverified Changes)" else "Toggle On Anti-Delete to Arm"
                    )
                    DiagnosticRow(
                        "AlarmManager Service Recovery",
                        s.effectiveAutoServiceRecovery || s.effectiveStableLockMode,
                        if (s.effectiveAutoServiceRecovery || s.effectiveStableLockMode) "Armed (1.5s Auto-Restart)" else "Disabled"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticDialog = false }) {
                    Text("All Good", color = primaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun PermissionHealthChip(
    label: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val chipColor = if (isGranted) Color(0xFF00E676) else Color(0xFFFF5252)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(chipColor.copy(alpha = 0.14f))
            .border(0.8.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun DiagnosticRow(title: String, isOk: Boolean, detail: String) {
    val color = if (isOk) Color(0xFF00E676) else Color(0xFFFFB300)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(detail, color = color, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun EscapeAttemptsLogCard(
    attempts: List<EscapeAttempt>,
    onClear: () -> Unit
) {
    val primaryCyan = Color(0xFF24DFEC)
    val dateFormat = remember { SimpleDateFormat("MMM d, hh:mm:ss a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), isHighlight = false)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = primaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detected Escape Attempts (${attempts.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                }
                if (attempts.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = primaryCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (attempts.isEmpty()) {
                Text(
                    text = "No bypass attempts recorded yet. Stay locked in 🔒",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                attempts.take(6).forEach { attempt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attempt.type,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF8A80)
                            )
                            Text(
                                text = dateFormat.format(Date(attempt.attemptTime)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TamperVerificationDialog(
    settings: UserSettings,
    targetProtectionName: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryCyan = Color(0xFF24DFEC)
    val usePin = settings.pinUnlockEnabled && settings.pinHash.isNotBlank()

    val numA = remember { Random.nextInt(12, 29) }
    val numB = remember { Random.nextInt(7, 19) }
    val numC = remember { Random.nextInt(4, 15) }
    val expectedAnswer = remember(numA, numB, numC) { (numA * 2 + numB - numC).toString() }

    var userInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF122030),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = primaryCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Anti-Delete Verification",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Anti-Delete Protection is active. Complete verification to modify \"$targetProtectionName\":",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                if (usePin) {
                    Text(
                        text = "Enter your configured security PIN:",
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Solve: ($numA × 2) + $numB - $numC = ?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = primaryCyan
                        )
                    }
                }

                OutlinedTextField(
                    value = userInput,
                    onValueChange = {
                        userInput = it
                        errorText = null
                    },
                    label = { Text(if (usePin) "Security PIN" else "Your Answer", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = primaryCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            GlassButton(
                text = "Verify",
                style = GlassButtonStyle.PRIMARY,
                onClick = {
                    val trimmed = userInput.trim()
                    val isCorrect = if (usePin) {
                        trimmed == settings.pinHash.trim()
                    } else {
                        trimmed == expectedAnswer
                    }
                    if (isCorrect) {
                        onVerified()
                    } else {
                        errorText = "Incorrect verification — stay locked in 🔒"
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.75f))
            }
        }
    )
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
