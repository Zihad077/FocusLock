package com.example.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper

enum class PermissionType {
    REQUIRED,
    OPTIONAL
}

data class PermissionItemData(
    val id: String,
    val title: String,
    val category: PermissionType,
    val icon: ImageVector,
    val shortPurpose: String,
    val detailedPurpose: String,
    val isGranted: Boolean,
    val actionText: String = "Grant",
    val restrictedSettingsGuide: String? = null
)

@Composable
fun PermissionsScreen(
    isFromSettings: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    // Permission states
    var hasAccessibility by remember { mutableStateOf(PermissionHelper.hasAccessibilityPermission(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasUsage by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }
    var hasNotification by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var hasBatteryOpt by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var hasDndPolicy by remember { mutableStateOf(PermissionHelper.hasNotificationPolicyAccess(context)) }
    var hasLocation by remember { mutableStateOf(PermissionHelper.hasLocationPermission(context)) }

    // Dialogs / Explanations
    var selectedHelpPermission by remember { mutableStateOf<PermissionItemData?>(null) }

    // Runtime permission launcher for Notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotification = isGranted
    }

    // Runtime permission launcher for Location (Optional profiles)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Refresh state when coming back to the foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = PermissionHelper.hasAccessibilityPermission(context)
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
                hasUsage = PermissionHelper.hasUsageAccess(context)
                hasNotification = PermissionHelper.hasNotificationPermission(context)
                hasBatteryOpt = PermissionHelper.isIgnoringBatteryOptimizations(context)
                hasDndPolicy = PermissionHelper.hasNotificationPolicyAccess(context)
                hasLocation = PermissionHelper.hasLocationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allRequiredGranted = hasAccessibility && hasOverlay && hasUsage
    val requiredCount = listOf(hasAccessibility, hasOverlay, hasUsage).count { it }
    val totalRequired = 3

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFromSettings && onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permission Center",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (allRequiredGranted) "All required system guards active"
                        else "$requiredCount of $totalRequired required permissions enabled",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (allRequiredGranted) primaryCyan else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Status Badge in Liquid Glass
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (allRequiredGranted) primaryCyan.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (allRequiredGranted) primaryCyan.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (allRequiredGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (allRequiredGranted) primaryCyan else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (allRequiredGranted) "PROTECTED" else "SETUP NEEDED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (allRequiredGranted) primaryCyan else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // Section 1: REQUIRED PERMISSIONS
                item {
                    SectionHeader(
                        title = "REQUIRED PERMISSIONS",
                        subtitle = "Essential for real-time app blocking and screen time limits",
                        isCritical = true
                    )
                }

                // 1. Accessibility Service
                item {
                    PermissionGlassCard(
                        data = PermissionItemData(
                            id = "accessibility",
                            title = "Accessibility Service",
                            category = PermissionType.REQUIRED,
                            icon = Icons.Default.Accessibility,
                            shortPurpose = "Instant detection when distraction apps launch to enforce limits.",
                            detailedPurpose = "FocusLock monitors window transitions strictly on-device to intercept restricted apps immediately. No keystrokes or text contents are read or stored.",
                            isGranted = hasAccessibility,
                            actionText = "Turn On",
                            restrictedSettingsGuide = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasAccessibility) {
                                "If toggle is grayed out on Android 13+: Open Phone Settings > Apps > FocusLock > tap 3 dots (⋮) in top right > tap 'Allow restricted settings'."
                            } else null
                        ),
                        onEnable = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // 2. Display Over Other Apps (Overlay)
                item {
                    PermissionGlassCard(
                        data = PermissionItemData(
                            id = "overlay",
                            title = "Display Over Other Apps",
                            category = PermissionType.REQUIRED,
                            icon = Icons.Default.Layers,
                            shortPurpose = "Displays the Liquid Glass blocking screen over restricted apps.",
                            detailedPurpose = "Allows FocusLock to render mindful challenge screens, countdown timers, and emergency break options when an app limit is reached.",
                            isGranted = hasOverlay,
                            actionText = "Grant"
                        ),
                        onEnable = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // 3. Usage Data Access
                item {
                    PermissionGlassCard(
                        data = PermissionItemData(
                            id = "usage",
                            title = "Usage Data Access",
                            category = PermissionType.REQUIRED,
                            icon = Icons.Default.QueryStats,
                            shortPurpose = "Calculates daily screen time and tracks streaks accurately.",
                            detailedPurpose = "Reads Android OS package usage statistics locally to tally daily screen-time minutes, track goal progress, and alert you before limits are hit.",
                            isGranted = hasUsage,
                            actionText = "Allow"
                        ),
                        onEnable = {
                            try {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // Section 2: OPTIONAL PERMISSIONS
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SectionHeader(
                        title = "OPTIONAL ENHANCEMENTS",
                        subtitle = "Enable for seamless background defense and silence controls",
                        isCritical = false
                    )
                }

                // 4. Notifications (POST_NOTIFICATIONS)
                item {
                    PermissionGlassCard(
                        data = PermissionItemData(
                            id = "notification",
                            title = "Active Notifications",
                            category = PermissionType.OPTIONAL,
                            icon = Icons.Default.Notifications,
                            shortPurpose = "Sends limit alerts, streak reminders, and active focus timers.",
                            detailedPurpose = "Allows FocusLock to show the persistent foreground service notification and alert you when approaching a daily time limit.",
                            isGranted = hasNotification,
                            actionText = "Allow"
                        ),
                        onEnable = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, "Notification permission is granted by default on your Android version", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // 5. Unrestricted Battery / Background
                item {
                    PermissionGlassCard(
                        data = PermissionItemData(
                            id = "battery",
                            title = "Unrestricted Battery",
                            category = PermissionType.OPTIONAL,
                            icon = Icons.Default.BatteryChargingFull,
                            shortPurpose = "Prevents Android OS from killing FocusLock in the background.",
                            detailedPurpose = "Disables aggressive OS power saving routines so focus sessions and app limit enforcements are never unexpectedly terminated.",
                            isGranted = hasBatteryOpt,
                            actionText = "Optimize"
                        ),
                        onEnable = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (ex: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        },
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // 6. Do Not Disturb (DND Policy)
                item {
                    PermissionGlassCard(
                        data = PermissionItemData(
                            id = "dnd",
                            title = "Do Not Disturb Access",
                            category = PermissionType.OPTIONAL,
                            icon = Icons.Default.DoNotDisturbOn,
                            shortPurpose = "Silences incoming alerts automatically during deep focus sessions.",
                            detailedPurpose = "Allows FocusLock to toggle system Do Not Disturb mode when you start a timed Deep Focus or Bedtime session.",
                            isGranted = hasDndPolicy,
                            actionText = "Grant"
                        ),
                        onEnable = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // 7. Location (Location-based Profiles)
                item {
                    PermissionGlassCard(
                        data = PermissionItemData(
                            id = "location",
                            title = "Location (Profiles)",
                            category = PermissionType.OPTIONAL,
                            icon = Icons.Default.Place,
                            shortPurpose = "Triggers automatic focus profiles at specific places (Work, Study).",
                            detailedPurpose = "Used solely to detect arrival at your configured Work or Library locations to automatically engage focus profiles. Location data never leaves your device.",
                            isGranted = hasLocation,
                            actionText = "Grant"
                        ),
                        onEnable = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Primary Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = if (allRequiredGranted) 12.dp else 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = primaryCyan
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (allRequiredGranted) {
                            Brush.horizontalGradient(
                                if (isDark) listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
                                else listOf(Color(0xFF0077D6), Color(0xFF0288D1))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF42556B), Color(0xFF334354))
                            )
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onPermissionsGranted,
                    enabled = allRequiredGranted,
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (allRequiredGranted) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFromSettings) {
                                if (allRequiredGranted) "All Required Active • Return" else "Enable Remaining Required"
                            } else {
                                if (allRequiredGranted) "Complete Setup & Enter FocusLock" else "Enable All 3 Required to Continue"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Detailed Permission Explanation Dialog
        selectedHelpPermission?.let { item ->
            PermissionExplanationDialog(
                data = item,
                onDismiss = { selectedHelpPermission = null }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    isCritical: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isCritical) primaryCyan else Color(0xFF9E9E9E))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = if (isCritical) primaryCyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontSize = 11.5.sp
            )
        )
    }
}

@Composable
private fun PermissionGlassCard(
    data: PermissionItemData,
    onEnable: () -> Unit,
    onHelpClick: (PermissionItemData) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(20.dp), isHighlight = data.isGranted)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Bubble
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (data.isGranted) primaryCyan.copy(alpha = 0.18f)
                            else Color.White.copy(alpha = if (isDark) 0.08f else 0.4f)
                        )
                        .border(
                            1.dp,
                            if (data.isGranted) primaryCyan.copy(alpha = 0.4f)
                            else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        tint = if (data.isGranted) primaryCyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { onHelpClick(data) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Explain ${data.title}",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = data.shortPurpose,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            lineHeight = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Grant Button or Active Badge
                if (data.isGranted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryCyan.copy(alpha = 0.15f))
                            .border(1.dp, primaryCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ON",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                ),
                                color = primaryCyan
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onEnable,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (data.category == PermissionType.REQUIRED) primaryCyan
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = data.actionText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (data.category == PermissionType.REQUIRED) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Android 13+ Restricted Settings warning if present
            if (data.restrictedSettingsGuide != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x25FF9800))
                        .border(1.dp, Color(0x60FF9800), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = data.restrictedSettingsGuide,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                lineHeight = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionExplanationDialog(
    data: PermissionItemData,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                data.icon,
                contentDescription = null,
                tint = primaryCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = data.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Category: ",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (data.category == PermissionType.REQUIRED) "Required (App-Blocking Core)" else "Optional (System Convenience)",
                        color = if (data.category == PermissionType.REQUIRED) primaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = data.detailedPurpose,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = if (isDark) 0.06f else 0.4f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🔒 FocusLock guarantees that your data is processed 100% locally on your device and never uploaded to remote servers.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", fontWeight = FontWeight.Bold, color = primaryCyan)
            }
        }
    )
}
