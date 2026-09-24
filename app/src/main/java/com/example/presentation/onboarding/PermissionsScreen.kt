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
    val primaryCyan = Color(0xFF00E5FF)

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
                            .background(Color.White.copy(alpha = 0.1f))
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (allRequiredGranted) "All required system guards active"
                        else "$requiredCount of $totalRequired required permissions enabled",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (allRequiredGranted) Color(0xFF00E5FF) else Color(0xFFE55353),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }

                // Status Badge in Liquid Glass (as shown in 1790213352222.png)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (allRequiredGranted) Color(0x3000E5FF)
                            else Color(0x40551822)
                        )
                        .border(
                            1.dp,
                            if (allRequiredGranted) Color(0x8000E5FF)
                            else Color(0x80EF4444),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (allRequiredGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (allRequiredGranted) Color(0xFF00E5FF) else Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (allRequiredGranted) "PROTECTED" else "SETUP NEEDED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (allRequiredGranted) Color(0xFF00E5FF) else Color(0xFFFF5252)
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
                            icon = Icons.Default.ShowChart,
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

            // Bottom Primary Action Button with Specular Diamond Star Sparkle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(
                        elevation = if (allRequiredGranted) 14.dp else 6.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = if (allRequiredGranted) primaryCyan else Color(0x20000000)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (allRequiredGranted) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x4239495B),
                                    Color(0x2E2A3644),
                                    Color(0x201E2632)
                                )
                            )
                        }
                    )
                    .border(
                        1.5.dp,
                        if (allRequiredGranted) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.55f),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (allRequiredGranted) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isFromSettings) {
                                if (allRequiredGranted) "All Required Active • Return" else "Save & Return"
                            } else {
                                if (allRequiredGranted) "Complete Setup & Enter FocusLock" else "Enter FocusLock (Preview Mode)"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Decorative Diamond Star Sparkle Flare on Top-Right Edge (matches 1790213352222.png)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-40).dp, y = (-8).dp)
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft halo glow
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                    // 4-point diamond star
                    Text(
                        text = "✦",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
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
    val primaryCyan = Color(0xFF00E5FF)

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isCritical) primaryCyan else Color(0xFFE2E8F0))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = if (isCritical) primaryCyan else Color(0xFFE2E8F0)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.5.sp
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
    val primaryCyan = Color(0xFF24DFEC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(26.dp), isHighlight = data.isGranted)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Frosted Circular Glass Icon Background
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (data.isGranted) Color(0x3500E5FF)
                            else Color(0x303E4F63)
                        )
                        .border(
                            1.dp,
                            if (data.isGranted) Color(0x8000E5FF)
                            else Color.White.copy(alpha = 0.35f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        tint = if (data.isGranted) Color(0xFF00E5FF) else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { onHelpClick(data) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Explain ${data.title}",
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = data.shortPurpose,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Action Button / Active Badge
                if (data.isGranted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x3000E5FF))
                            .border(1.dp, Color(0x8000E5FF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ON",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                } else if (data.category == PermissionType.REQUIRED) {
                    // Required Solid Turquoise Cyan Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF24DFEC))
                            .clickable(onClick = onEnable)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.actionText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF061820)
                        )
                    }
                } else {
                    // Optional Frosted Glass Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x403E4C5E))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.40f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(onClick = onEnable)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.actionText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // Android 13+ Restricted Settings warning if present
            if (data.restrictedSettingsGuide != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x455A371B))
                        .border(1.dp, Color(0x80FFA726), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFA726),
                            modifier = Modifier
                                .size(17.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = data.restrictedSettingsGuide,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFDE68A),
                                fontSize = 12.5.sp,
                                lineHeight = 16.5.sp
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
