package com.example.presentation.focus

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.liquidGlass

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = viewModel(
        factory = FocusViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val isFocusActive by viewModel.isFocusActive.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingTimeSeconds.collectAsStateWithLifecycle()
    val selectedDuration by viewModel.selectedDurationMinutes.collectAsStateWithLifecycle()
    val showJournalDialog by viewModel.showJournalDialog.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)
    val accentError = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Focus Session",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isFocusActive) "Deep work in progress. Distractions filtered." else "Immerse in uninterrupted flow state.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Liquid Glass Circular Timer Display with ambient glow ring
        Box(
            modifier = Modifier
                .size(280.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x60001025),
                    spotColor = if (isFocusActive) primaryCyan else Color(0x3500E5FF)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                (if (isFocusActive) Color(0x4000E5FF) else Color(0x301E3555)),
                                Color(0x200C1625),
                                Color(0x40050B12)
                            )
                        } else {
                            listOf(
                                (if (isFocusActive) Color(0x50E0F7FA) else Color(0x60FFFFFF)),
                                Color(0x40E8F1F8),
                                Color(0x80D5E5F2)
                            )
                        },
                        center = Offset(400f, 400f),
                        radius = 500f
                    )
                )
                .border(
                    width = 4.dp,
                    brush = Brush.sweepGradient(
                        colors = if (isFocusActive) {
                            listOf(
                                primaryCyan,
                                Color(0xFF2979FF),
                                Color.White.copy(alpha = 0.8f),
                                primaryCyan
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color(0x3080D8FF),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.5f)
                            )
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val displayTime = if (isFocusActive) {
                val m = remainingSeconds / 60
                val s = remainingSeconds % 60
                String.format("%02d:%02d", m, s)
            } else {
                "$selectedDuration:00"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = if (isFocusActive) primaryCyan else MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isFocusActive) "MINUTES REMAINING" else "TARGET DURATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(visible = !isFocusActive) {
            val durations = listOf(15, 25, 45, 60, 90)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                durations.forEach { duration ->
                    DurationGlassChip(
                        minutes = duration,
                        isSelected = selectedDuration == duration,
                        onClick = { viewModel.setDuration(duration) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Main Action Liquid Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isFocusActive) accentError else primaryCyan
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isFocusActive) {
                            listOf(Color(0xFFFF5252), Color(0xFFD50000))
                        } else {
                            if (isDark) listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
                            else listOf(Color(0xFF0077D6), Color(0xFF0288D1))
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable {
                    if (isFocusActive) viewModel.endFocusSession() else viewModel.startFocusSession()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isFocusActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = if (isFocusActive) "End Focus Session" else "Start Focus Session",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        if (showJournalDialog) {
            var journalEntry by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.dismissJournalDialog() },
                modifier = Modifier.liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true),
                title = {
                    Text(
                        "Focus Journal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = {
                    Column {
                        Text(
                            "What milestones did you accomplish during this session?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = journalEntry,
                            onValueChange = { journalEntry = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            placeholder = { Text("Notes, insights, accomplishments...") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.saveJournalEntry(journalEntry) },
                        enabled = journalEntry.isNotBlank()
                    ) {
                        Text(
                            "Save Entry",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = primaryCyan
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissJournalDialog() }) {
                        Text(
                            "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun DurationGlassChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    primaryColor.copy(alpha = if (isDark) 0.35f else 0.22f)
                } else {
                    if (isDark) Color(0x2022354E) else Color(0x40FFFFFF)
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) primaryColor else Color.White.copy(alpha = if (isDark) 0.15f else 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onBackground
        )
    }
}
