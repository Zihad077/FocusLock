package com.example.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.PermissionHelper

@Composable
fun PermissionsScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasUsageAccess by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }
    var hasOverlayPermission by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionHelper.hasAccessibilityPermission(context)) }
    
    // Core protection works when at least Usage Access + Overlay are enabled; Accessibility gives instant interception
    val isReadyToBlock = hasUsageAccess && hasOverlayPermission
    val allGranted = isReadyToBlock && hasAccessibility
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = PermissionHelper.hasUsageAccess(context)
                hasOverlayPermission = PermissionHelper.hasOverlayPermission(context)
                hasAccessibility = PermissionHelper.hasAccessibilityPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = "Setup App Blocker",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "FocusLock requires permissions to detect when distracting apps are launched and show the mindful block screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PermissionCard(
                    title = "1. Accessibility Service (Recommended)",
                    description = "Enables instant, real-time app interception and returns to Home when a blocked app is opened.",
                    isGranted = hasAccessibility,
                    onEnableClick = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to general settings
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    },
                    additionalNote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasAccessibility) {
                        "Android 13+ Note: If grayed out, open Settings > Apps > FocusLock > tap 3 dots (⋮) top-right > tap 'Allow restricted settings'."
                    } else null
                )
            }

            item {
                PermissionCard(
                    title = "2. Display Over Other Apps",
                    description = "Allows FocusLock to show the blocking screen and mindful breathing challenge over restricted apps.",
                    isGranted = hasOverlayPermission,
                    onEnableClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            item {
                PermissionCard(
                    title = "3. Usage Access",
                    description = "Allows FocusLock to track foreground minutes against your custom daily limits.",
                    isGranted = hasUsageAccess,
                    onEnableClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
        }
        
        Button(
            onClick = onPermissionsGranted,
            enabled = isReadyToBlock || allGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (allGranted) "All Set — Start FocusLock" 
                       else if (isReadyToBlock) "Continue with Background Monitor" 
                       else "Enable Required Permissions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onEnableClick: () -> Unit,
    additionalNote: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Active",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Button(
                        onClick = onEnableClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Enable", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (additionalNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = additionalNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
