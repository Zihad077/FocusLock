package com.example.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.liquidGlass
import com.example.util.DataBackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<DataBackupManager.ValidationResult?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            scope.launch {
                val result = DataBackupManager.validateBackupFromUri(context, uri)
                validationResult = result
                isImporting = false
                if (result.isValid) {
                    showRestoreDialog = true
                } else {
                    restoreMessage = result.errorMessage ?: "Failed to validate backup file."
                }
            }
        }
    }

    val primaryCyan = Color(0xFF24DFEC)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Data & Backup",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro text
            Text(
                "Secure your FocusLock data. Export a complete backup or restore from a previously saved file.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = Color.White.copy(alpha = 0.7f)
            )

            // Export Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(26.dp))
                    .clickable(enabled = !isExporting) {
                        isExporting = true
                        scope.launch {
                            try {
                                val file = DataBackupManager.exportBackupFile(context, viewModel.repository)
                                DataBackupManager.shareBackupFile(context, file)
                            } catch (e: Exception) {
                                restoreMessage = "Failed to export data: ${e.localizedMessage}"
                            } finally {
                                isExporting = false
                            }
                        }
                    }
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x283E4C5E))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = primaryCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Export Data",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "Save a complete backup of all your FocusLock data, settings, and progress.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Import Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(26.dp))
                    .clickable(enabled = !isImporting) {
                        importLauncher.launch("application/json")
                    }
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x283E4C5E))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = primaryCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Restore Data",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "Restore from a previously exported FocusLock JSON backup file.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Messages
            AnimatedVisibility(visible = restoreMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (restoreMessage?.contains("Failed", ignoreCase = true) == true) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (restoreMessage?.contains("Failed", ignoreCase = true) == true) Color(0xFFFF5252) else primaryCyan
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = restoreMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showRestoreDialog && validationResult?.isValid == true && validationResult?.parsedBackup != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Backup") },
            text = {
                val summary = validationResult?.summary
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to restore this data? Your current data will be permanently overwritten.", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Backup Summary:", fontWeight = FontWeight.Bold)
                    Text("Created: ${summary?.createdAt}")
                    Text("Level ${summary?.level} • ${summary?.streak} Day Streak")
                    Text("Focus Sessions: ${summary?.focusSessionsCount}")
                    Text("Goals: ${summary?.goalsCount}")
                    Text("Limits: ${summary?.limitsCount}")
                    Text("Profiles: ${summary?.profilesCount}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        isImporting = true
                        scope.launch {
                            val success = DataBackupManager.restoreBackup(viewModel.repository, validationResult!!.parsedBackup!!)
                            isImporting = false
                            if (success) {
                                restoreMessage = "Data restored successfully! Please restart the app if some screens don't update immediately."
                            } else {
                                restoreMessage = "Failed to restore data due to an internal error."
                            }
                        }
                    }
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        )
    }
}
