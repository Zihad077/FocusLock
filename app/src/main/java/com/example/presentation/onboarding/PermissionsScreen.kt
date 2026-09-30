package com.example.presentation.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.AppMonitorService
import com.example.ui.theme.FocusLockSemanticColors
import com.example.ui.theme.GlassStatusBadge
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.SemanticTone
import com.example.ui.theme.liquidGlass
import com.example.util.PermissionHelper
import kotlinx.coroutines.delay

enum class PermissionType {
    REQUIRED
}

data class PermissionItemData(
    val id: String,
    val title: String,
    val category: PermissionType = PermissionType.REQUIRED,
    val icon: ImageVector,
    val shortPurpose: String,
    val detailedPurpose: String,
    val whyRequiredBullet: String,
    val isGranted: Boolean,
    val actionText: String = "Grant",
    val restrictedSettingsGuide: String? = null,
    val onGrantClick: (() -> Unit)? = null
)

/**
 * Dedicated Permission Screen with Context-Based Color Psychology & Compact Permission Center Cards:
 * - Green: Successfully granted permissions & ready confirmation
 * - Amber: Permissions that still need attention / pending setup
 * - Red: Critical permission errors / missing protection alerts when attempting to configure or enforce limits
 * - Blue/Teal: Informational context and guidance
 *
 * Layout:
 * 1. Compact Required Permission Cards at the top (with an "i" info button beside each permission name to view full details)
 * 2. Contextual explanation cards ("Required to Limit..." and "Why FocusLock Needs These 3 Permissions") below the permission cards
 * 3. Bottom "Continue" button (disabled until all 3 required permissions are granted)
 */
@Composable
fun PermissionsScreen(
    isFromSettings: Boolean = false,
    isForLimitSetup: Boolean = false,
    targetAppName: String? = null,
    hasExistingConfiguredLimits: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val semantic = FocusLockSemanticColors

    // Live required permission states — never assumed granted until verified by PermissionHelper
    var hasUsage by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionHelper.hasAccessibilityPermission(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }

    // Track if the user launched Android Settings from this screen so we can auto-continue once all 3 are granted
    var launchedSettingsForPermission by remember { mutableStateOf(false) }
    var selectedHelpPermission by remember { mutableStateOf<PermissionItemData?>(null) }

    // Recheck permission status when the user returns from Android Settings using ON_RESUME lifecycle callback
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsage = PermissionHelper.hasUsageAccess(context)
                hasAccessibility = PermissionHelper.hasAccessibilityPermission(context)
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allRequiredGranted = hasUsage && hasAccessibility && hasOverlay
    val requiredCount = listOf(hasUsage, hasAccessibility, hasOverlay).count { it }
    val totalRequired = 3
    val missingNames = remember(hasUsage, hasAccessibility, hasOverlay) {
        buildList {
            if (!hasUsage) add("Usage Data Access")
            if (!hasAccessibility) add("Accessibility Service")
            if (!hasOverlay) add("Display Over Other Apps")
        }
    }

    // Once all permissions are granted during an app-limit setup flow (or after returning from Settings),
    // start the monitoring service and automatically return the user to their original limit-setup flow.
    LaunchedEffect(allRequiredGranted, launchedSettingsForPermission, isForLimitSetup) {
        if (allRequiredGranted) {
            AppMonitorService.startService(context)
            if (isForLimitSetup || launchedSettingsForPermission) {
                delay(380) // Brief visual confirmation of Green "All Granted" state before returning
                onPermissionsGranted()
            }
        }
    }

    if (onNavigateBack != null) {
        BackHandler(onBack = onNavigateBack)
    }

    val headerSemanticTone = when {
        allRequiredGranted -> SemanticTone.GREEN
        isForLimitSetup || hasExistingConfiguredLimits -> SemanticTone.RED
        else -> SemanticTone.AMBER
    }

    val openUsageSettings: () -> Unit = {
        launchedSettingsForPermission = true
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (ex: Exception) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    val openAccessibilitySettings: () -> Unit = {
        launchedSettingsForPermission = true
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    val openOverlaySettings: () -> Unit = {
        launchedSettingsForPermission = true
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .testTag("permissions_back_button")
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(semantic.neutral.container)
                            .border(1.dp, semantic.neutral.border, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = semantic.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isForLimitSetup) "Permissions Required" else "Permission Center",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = semantic.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            allRequiredGranted -> "All 3 required permissions granted & active"
                            else -> "$requiredCount of $totalRequired required permissions enabled"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = when {
                                allRequiredGranted -> semantic.green.accent
                                isForLimitSetup -> semantic.amber.accent
                                else -> semantic.red.accent
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                GlassStatusBadge(
                    text = when {
                        allRequiredGranted -> "READY"
                        isForLimitSetup -> "ACTION NEEDED"
                        else -> "$requiredCount/$totalRequired ACTIVE"
                    },
                    semanticTone = headerSemanticTone,
                    icon = when {
                        allRequiredGranted -> Icons.Default.CheckCircle
                        headerSemanticTone == SemanticTone.RED -> Icons.Default.ErrorOutline
                        else -> Icons.Default.WarningAmber
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // Section Header for the 3 Required Permissions (at the very top)
                item {
                    val sectionColor = if (allRequiredGranted) semantic.green.accent else semantic.amber.accent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(sectionColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REQUIRED PERMISSIONS ($requiredCount/$totalRequired)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.5.sp
                                ),
                                color = sectionColor
                            )
                        }
                        Text(
                            text = "Tap ⓘ for details",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = semantic.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // 1. Usage Data Access (Compact Card)
                item {
                    val usageItem = PermissionItemData(
                        id = "usage",
                        title = "Usage Data Access",
                        icon = Icons.Default.QueryStats,
                        shortPurpose = "Tracks daily screen time & app usage minutes.",
                        detailedPurpose = "FocusLock queries Android's UsageStatsManager locally on your device to calculate how many minutes you have spent in each app today and trigger timely warnings when you approach your limit.",
                        whyRequiredBullet = "Required to measure how long restricted apps are used each day.",
                        isGranted = hasUsage,
                        actionText = "Grant",
                        onGrantClick = openUsageSettings
                    )
                    PermissionGlassCard(
                        data = usageItem,
                        onEnable = openUsageSettings,
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // 2. Accessibility Service (Compact Card)
                item {
                    val accessibilityItem = PermissionItemData(
                        id = "accessibility",
                        title = "Accessibility Service",
                        icon = Icons.Default.AccessibilityNew,
                        shortPurpose = "Detects restricted app launches in real time.",
                        detailedPurpose = "FocusLock uses Android's Accessibility Service strictly on-device to detect foreground app launches so limits and focus locks cannot be bypassed. FocusLock never reads or stores personal text or passwords.",
                        whyRequiredBullet = "Required to instantly detect and intercept restricted apps when opened.",
                        isGranted = hasAccessibility,
                        actionText = "Enable",
                        restrictedSettingsGuide = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasAccessibility) {
                            "Tip for Android 13+: If the toggle is grayed out, open Phone Settings > Apps > FocusLock > tap the 3 dots (⋮) in the top-right corner > tap 'Allow restricted settings'."
                        } else null,
                        onGrantClick = openAccessibilitySettings
                    )
                    PermissionGlassCard(
                        data = accessibilityItem,
                        onEnable = openAccessibilitySettings,
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                // 3. Display Over Other Apps (Compact Card)
                item {
                    val overlayItem = PermissionItemData(
                        id = "overlay",
                        title = "Display Over Other Apps",
                        icon = Icons.Default.Layers,
                        shortPurpose = "Shows the lock shield when time expires.",
                        detailedPurpose = "Allows FocusLock to display the blocking screen, countdown timer, and mindful unlock challenge directly over restricted apps the moment their time limit is reached.",
                        whyRequiredBullet = "Required to display the lock shield when an app limit is reached.",
                        isGranted = hasOverlay,
                        actionText = "Grant",
                        onGrantClick = openOverlaySettings
                    )
                    PermissionGlassCard(
                        data = overlayItem,
                        onEnable = openOverlaySettings,
                        onHelpClick = { selectedHelpPermission = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Contextual Explanation Banner (moved below the 3 permission cards as requested)
                item {
                    PermissionContextBanner(
                        allRequiredGranted = allRequiredGranted,
                        isForLimitSetup = isForLimitSetup,
                        targetAppName = targetAppName,
                        hasExistingConfiguredLimits = hasExistingConfiguredLimits,
                        missingNames = missingNames
                    )
                }

                // Why FocusLock Needs These Permissions Summary Card (moved below as requested)
                item {
                    WhyPermissionsNecessaryCard()
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Bottom "Continue" Button — Disabled until ALL required permissions are granted
            val continueButtonContainerBrush = if (allRequiredGranted) {
                Brush.horizontalGradient(
                    listOf(semantic.green.accent, Color(0xFF059669))
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color(0x42334155),
                        Color(0x2E1E293B)
                    )
                )
            }

            val continueBorderColor by animateColorAsState(
                targetValue = if (allRequiredGranted) semantic.green.border else semantic.neutral.border,
                animationSpec = tween(250),
                label = "continue_border"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!allRequiredGranted) {
                    Text(
                        text = "Enable ${totalRequired - requiredCount} more required permission${if (totalRequired - requiredCount == 1) "" else "s"} to unlock Continue",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = semantic.amber.accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Button(
                    onClick = {
                        if (allRequiredGranted) {
                            AppMonitorService.startService(context)
                            onPermissionsGranted()
                        }
                    },
                    enabled = allRequiredGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("permissions_continue_button")
                        .shadow(
                            elevation = if (allRequiredGranted) 12.dp else 0.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = if (allRequiredGranted) semantic.green.accent else Color.Transparent
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(continueButtonContainerBrush)
                        .border(
                            width = 1.4.dp,
                            color = continueBorderColor,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = Color(0xFF042F2E),
                        disabledContentColor = semantic.textMuted
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (allRequiredGranted) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (allRequiredGranted) Color(0xFF032219) else semantic.textMuted,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                allRequiredGranted && isForLimitSetup && !targetAppName.isNullOrBlank() ->
                                    "Continue to $targetAppName Limit Setup"
                                allRequiredGranted && isForLimitSetup ->
                                    "Continue to Limit Setup"
                                allRequiredGranted ->
                                    "Continue"
                                else ->
                                    "Continue ($requiredCount/$totalRequired Permissions Granted)"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp
                            ),
                            color = if (allRequiredGranted) Color(0xFF032219) else semantic.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Detailed Permission Explanation Dialog when user taps the "i" button
        selectedHelpPermission?.let { item ->
            PermissionExplanationDialog(
                data = item,
                onDismiss = { selectedHelpPermission = null }
            )
        }
    }
}

/**
 * Compact, single-row Permission Card matching the original Permission Center look:
 * - Left: 42dp Semantic Icon Bubble
 * - Middle: Permission Title + "i" Info Button right beside the name (opens full details popup) + compact 1-line status
 * - Right: Compact "Grant" / "Enable" pill button or "GRANTED" badge
 */
@Composable
private fun PermissionGlassCard(
    data: PermissionItemData,
    onEnable: () -> Unit,
    onHelpClick: (PermissionItemData) -> Unit
) {
    val semantic = FocusLockSemanticColors
    // Semantic Rule: Green for granted permissions, Amber for permissions that still need attention
    val cardTone = if (data.isGranted) SemanticTone.GREEN else SemanticTone.AMBER
    val palette = semantic.forTone(cardTone)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                semanticTone = cardTone
            )
            .clickable {
                if (!data.isGranted) {
                    onEnable()
                } else {
                    onHelpClick(data)
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Compact Circular Icon Bubble
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.container)
                    .border(1.1.dp, palette.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (data.isGranted) Icons.Default.CheckCircle else data.icon,
                    contentDescription = null,
                    tint = palette.icon,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Permission Name + "i" Info Button + 1-line compact status
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = semantic.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // "i" button right next to the permission name to view full details
                    Box(
                        modifier = Modifier
                            .testTag("info_permission_${data.id}")
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(semantic.blue.container)
                            .border(0.8.dp, semantic.blue.border, CircleShape)
                            .clickable { onHelpClick(data) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Details for ${data.title}",
                            tint = semantic.blue.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (data.isGranted) {
                        "Granted & Active"
                    } else {
                        data.shortPurpose
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.8.sp,
                        fontWeight = if (data.isGranted) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (data.isGranted) palette.accent else semantic.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Action Button or Granted Status Badge
            if (data.isGranted) {
                GlassStatusBadge(
                    text = "GRANTED",
                    semanticTone = SemanticTone.GREEN,
                    icon = Icons.Default.Check
                )
            } else {
                Box(
                    modifier = Modifier
                        .testTag("grant_permission_${data.id}")
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(semantic.amber.accent, Color(0xFFD97706))
                            )
                        )
                        .border(1.dp, semantic.amber.border, RoundedCornerShape(14.dp))
                        .clickable(onClick = onEnable)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = data.actionText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        ),
                        color = Color(0xFF1F1200)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionContextBanner(
    allRequiredGranted: Boolean,
    isForLimitSetup: Boolean,
    targetAppName: String?,
    hasExistingConfiguredLimits: Boolean,
    missingNames: List<String>
) {
    val semantic = FocusLockSemanticColors
    val tone = when {
        allRequiredGranted -> SemanticTone.GREEN
        isForLimitSetup || hasExistingConfiguredLimits -> SemanticTone.RED
        else -> SemanticTone.AMBER
    }
    val palette = semantic.forTone(tone)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                semanticTone = tone
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.container)
                    .border(1.dp, palette.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        allRequiredGranted -> Icons.Default.VerifiedUser
                        tone == SemanticTone.RED -> Icons.Default.GppMaybe
                        else -> Icons.Default.WarningAmber
                    },
                    contentDescription = null,
                    tint = palette.icon,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        allRequiredGranted -> "All Required Permissions Active"
                        isForLimitSetup && !targetAppName.isNullOrBlank() ->
                            "Required to Limit $targetAppName"
                        isForLimitSetup ->
                            "Permissions Needed to Set App Limits"
                        hasExistingConfiguredLimits ->
                            "Limit Enforcement Paused — Permission Missing"
                        else ->
                            "Complete Permission Setup for App Blocking"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.8.sp
                    ),
                    color = palette.accent
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = when {
                        allRequiredGranted ->
                            "FocusLock has full access to track usage and enforce your app limits reliably. Tap Continue below to proceed."
                        isForLimitSetup ->
                            "Missing: ${missingNames.joinToString(", ")}. FocusLock cannot monitor screen time or block restricted apps until all 3 permissions above are enabled."
                        hasExistingConfiguredLimits ->
                            "Your configured app limits are safely stored, but enforcement is currently unavailable because ${missingNames.joinToString(", ")} ${if (missingNames.size == 1) "is" else "are"} turned off."
                        else ->
                            "Currently missing: ${missingNames.joinToString(", ")}. Enable each permission above to activate real-time app limits."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.5.sp
                    ),
                    color = semantic.textPrimary.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun WhyPermissionsNecessaryCard() {
    val semantic = FocusLockSemanticColors
    val bluePalette = semantic.blue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                semanticTone = SemanticTone.BLUE
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = bluePalette.icon,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Why FocusLock Needs These 3 Permissions",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = bluePalette.accent
                )
            }
            Text(
                text = "• Usage Data Access measures how many minutes you spend in each app.\n" +
                        "• Accessibility Service detects the instant a restricted app opens.\n" +
                        "• Display Over Other Apps shows the lock screen when your time limit is up.\n" +
                        "All usage checks run 100% locally on your device.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.8.sp,
                    lineHeight = 16.sp
                ),
                color = semantic.textSecondary
            )
        }
    }
}

@Composable
private fun PermissionExplanationDialog(
    data: PermissionItemData,
    onDismiss: () -> Unit
) {
    val semantic = FocusLockSemanticColors
    val tonePalette = if (data.isGranted) semantic.green else semantic.amber

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = semantic.surfaceElevated,
        modifier = Modifier.border(1.dp, tonePalette.border, RoundedCornerShape(24.dp)),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(tonePalette.container)
                    .border(1.2.dp, tonePalette.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    data.icon,
                    contentDescription = null,
                    tint = tonePalette.icon,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = data.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                color = semantic.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassStatusBadge(
                    text = if (data.isGranted) "GRANTED & ACTIVE" else "REQUIRED FOR APP LIMITS",
                    semanticTone = if (data.isGranted) SemanticTone.GREEN else SemanticTone.AMBER,
                    icon = if (data.isGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber
                )

                Text(
                    text = data.detailedPurpose,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.5.sp
                    ),
                    color = semantic.textSecondary
                )

                if (data.restrictedSettingsGuide != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(semantic.amber.container)
                            .border(1.dp, semantic.amber.border, RoundedCornerShape(12.dp))
                            .padding(11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = semantic.amber.icon,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = data.restrictedSettingsGuide,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = semantic.amber.onContainer,
                                    fontSize = 11.8.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(semantic.blue.container)
                        .border(1.dp, semantic.blue.border, RoundedCornerShape(12.dp))
                        .padding(11.dp)
                ) {
                    Text(
                        text = "🔒 Privacy Guarantee: All usage tracking and app-blocking checks happen 100% locally on your device.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = semantic.blue.onContainer,
                            fontSize = 11.8.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            if (!data.isGranted && data.onGrantClick != null) {
                Button(
                    onClick = {
                        onDismiss()
                        data.onGrantClick.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = semantic.amber.accent,
                        contentColor = Color(0xFF1F1200)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = data.actionText,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Got It", fontWeight = FontWeight.Bold, color = semantic.blue.accent)
                }
            }
        },
        dismissButton = {
            if (!data.isGranted && data.onGrantClick != null) {
                TextButton(onClick = onDismiss) {
                    Text("Close", fontWeight = FontWeight.SemiBold, color = semantic.textSecondary)
                }
            }
        }
    )
}
