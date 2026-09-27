package com.example.presentation.focus

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdsterraSocialBar
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.database.isPremiumActive
import com.example.ui.theme.GlassButton
import com.example.ui.theme.GlassButtonStyle
import com.example.ui.theme.GlassIconBubble
import com.example.ui.theme.GlassStatusBadge
import com.example.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onNavigateToHome: () -> Unit = {},
    viewModel: FocusViewModel = viewModel(
        factory = FocusViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val isFocusActive by viewModel.isFocusActive.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingTimeSeconds.collectAsStateWithLifecycle()
    val selectedDuration by viewModel.selectedDurationMinutes.collectAsStateWithLifecycle()
    val showJournalDialog by viewModel.showJournalDialog.collectAsStateWithLifecycle()
    val cooldownRemaining by viewModel.cooldownRemainingSeconds.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle(initialValue = null)

    // Back navigation disabled strictly only during active Focus Mode to prevent escape
    BackHandler(enabled = isFocusActive) {
        // Intentionally consumed: Focus Mode screen must remain the only accessible screen during active focus
    }

    val primaryCyan = Color(0xFF24DFEC)
    val accentBlue = Color(0xFF1EA7FD)
    val accentPurple = Color(0xFF9D4EDD)
    val accentError = Color(0xFFFF5252)

    val scrollState = rememberScrollState()

    // Smooth pulsing glow when in active focus mode
    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Focus Mode",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (isFocusActive) "SESSION ACTIVE" else "DEEP WORK FLOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.5.sp
                            ),
                            color = primaryCyan
                        )
                    }
                },
                navigationIcon = {
                    if (!isFocusActive) {
                        IconButton(
                            onClick = onNavigateToHome,
                            modifier = Modifier.testTag("focus_back_to_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to Home",
                                tint = primaryCyan
                            )
                        }
                    }
                },
                actions = {
                    if (userSettings != null) {
                        GlassStatusBadge(
                            text = "Lv. ${userSettings?.level ?: 1}",
                            isHighlight = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp), // Generous bottom padding to ensure start button is always above floating bottom bar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Description Subtitle
            Text(
                text = if (isFocusActive) "Deep work in progress. Distractions & notifications blocked." else "Enter uninterrupted deep flow state. Pick your focus duration.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0C0D4),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (!isFocusActive) {
                com.example.ads.LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // ==========================================
            // CLEAN CIRCULAR GLASS TIMER
            // ==========================================
            val totalSeconds = (selectedDuration * 60).toFloat()
            val progressFraction = if (isFocusActive && totalSeconds > 0) {
                (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
            } else {
                1f
            }

            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = tween(500, easing = LinearEasing),
                label = "timer_progress"
            )

            Box(
                modifier = Modifier
                    .size(256.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isFocusActive) {
                                listOf(
                                    Color(0xF2132842),
                                    Color(0xF20D1B2E),
                                    Color(0xF5091220)
                                )
                            } else {
                                listOf(
                                    Color(0xEB122036),
                                    Color(0xEE0C1626),
                                    Color(0xF208101C)
                                )
                            }
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = if (isFocusActive) {
                                listOf(
                                    primaryCyan.copy(alpha = 0.75f * pulseGlow),
                                    accentBlue.copy(alpha = 0.45f),
                                    primaryCyan.copy(alpha = 0.35f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color(0xFF00E5FF).copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.10f)
                                )
                            }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Smooth Clean Canvas Arc Ring
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val strokePx = 10.dp.toPx()
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    val arcTopLeft = Offset(strokePx / 2f, strokePx / 2f)

                    // 1. Deep Recessed Track Ring
                    drawArc(
                        color = Color(0xFF18283E),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // 2. Active Animated Gradient Arc
                    val sweepAngle = 360f * animatedProgress
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush = Brush.linearGradient(
                                colors = if (isFocusActive) {
                                    listOf(Color(0xFF00E5FF), accentBlue, accentPurple)
                                } else {
                                    listOf(Color(0xFF00E5FF), Color(0xFF24DFEC), accentBlue)
                                },
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Display Time & Status
                val displayTime = if (isFocusActive) {
                    val m = remainingSeconds / 60
                    val s = remainingSeconds % 60
                    String.format("%02d:%02d", m, s)
                } else {
                    "$selectedDuration:00"
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFocusActive) Color(0x2900E5FF) else Color(0xFF152338)
                            )
                            .border(
                                1.dp,
                                if (isFocusActive) primaryCyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFocusActive) "FLOW STATE ACTIVE" else "TARGET DURATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = if (isFocusActive) primaryCyan else Color(0xFFB0C0D4)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = if (isFocusActive) primaryCyan else Color(0xFFF8FAFC)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isFocusActive) "STAY FOCUSED" else "$selectedDuration MIN SESSION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Duration selection card (Visible when not active)
            AnimatedVisibility(
                visible = !isFocusActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SELECT DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp
                        ),
                        color = Color(0xFFB0C0D4)
                    )

                    val durations = listOf(15, 25, 45, 60, 90, 120)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durations.forEach { duration ->
                            val isSelected = selectedDuration == duration
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        if (isSelected) Color(0xFF15324D) else Color(0xFF152236)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) primaryCyan else Color.White.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(13.dp)
                                    )
                                    .clickable { viewModel.setDuration(duration) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (duration >= 60) "${duration / 60}h" else "${duration}m",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) primaryCyan else Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // MAIN ACTION BUTTON (PROMINENT, CLEAN GLASS CTA)
            // ==========================================
            val isCooldownLocked = isFocusActive && ((cooldownRemaining ?: 0) > 0)
            val cooldownMinutes = (cooldownRemaining ?: 0) / 60
            val cooldownSeconds = (cooldownRemaining ?: 0) % 60
            val cooldownFormatted = String.format("%d:%02d", cooldownMinutes, cooldownSeconds)

            GlassButton(
                onClick = {
                    if (isFocusActive) {
                        viewModel.endFocusSession(completed = false)
                    } else {
                        viewModel.startFocusSession()
                    }
                },
                text = when {
                    !isFocusActive -> "Start Focus Session (${selectedDuration}m)"
                    isCooldownLocked -> "Cooldown Active ($cooldownFormatted)"
                    else -> "End Focus Session"
                },
                icon = when {
                    !isFocusActive -> Icons.Default.PlayArrow
                    isCooldownLocked -> Icons.Default.Lock
                    else -> Icons.Default.Stop
                },
                style = if (isFocusActive) GlassButtonStyle.DESTRUCTIVE else GlassButtonStyle.PRIMARY,
                enabled = !isCooldownLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_action_button")
            )

            // Return to Home Dashboard button (Available when not in active focus session)
            if (!isFocusActive) {
                GlassButton(
                    onClick = onNavigateToHome,
                    text = "Return to Home Dashboard",
                    icon = Icons.Default.Home,
                    style = GlassButtonStyle.SECONDARY,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("return_to_home_button")
                )
            }

            // Info & Protection Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(22.dp), isElevated = false)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GlassIconBubble(
                        icon = Icons.Default.Shield,
                        size = 44.dp,
                        iconSize = 22.dp,
                        isHighlight = isFocusActive
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFocusActive) "Distraction Shield Active" else "Strict Focus Protection",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFocusActive) "Restricted apps are blocked until your session concludes." else "Includes cooling-down protection to prevent impulsive unlocking.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = Color(0xFFB0C0D4)
                        )
                    }
                }
            }

            if (!isFocusActive) {
                LiquidGlassNativeAdCard(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
                AdsterraSocialBar(
                    isPremium = userSettings?.isPremiumActive ?: false,
                    isFocusActive = isFocusActive
                )
            }
        }
    }

    // Journal Dialog upon session completion
    if (showJournalDialog) {
        var journalEntry by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissJournalDialog()
                onNavigateToHome()
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFF101C2E),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp)),
            title = {
                Text(
                    text = "Focus Session Complete! 🎉",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Great job staying in the zone! What did you accomplish?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0C0D4)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = journalEntry,
                        onValueChange = { journalEntry = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Notes, insights, accomplishments...", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                            focusedContainerColor = Color(0xFF0B1422),
                            unfocusedContainerColor = Color(0xFF0B1422)
                        )
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    onClick = {
                        viewModel.saveJournalEntry(journalEntry)
                        onNavigateToHome()
                    },
                    text = "Save & View Dashboard",
                    style = GlassButtonStyle.PRIMARY,
                    enabled = journalEntry.isNotBlank()
                )
            },
            dismissButton = {
                GlassButton(
                    onClick = {
                        viewModel.dismissJournalDialog()
                        onNavigateToHome()
                    },
                    text = "Skip to Home",
                    style = GlassButtonStyle.SECONDARY
                )
            }
        )
    }
}
