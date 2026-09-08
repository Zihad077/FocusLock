package com.example.presentation.challenge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.UserSettings
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.delay

enum class ChallengeType { MIND, FOCUS, TYPING, PIN }

@Composable
fun ChallengeScreen(
    settings: UserSettings,
    onChallengeComplete: (ChallengeType) -> Unit,
    onCancel: () -> Unit
) {
    var selectedChallenge by remember { mutableStateOf<ChallengeType?>(null) }

    LiquidBackground {
        if (selectedChallenge == null) {
            ChallengeSelectionScreen(
                settings = settings,
                onSelect = { selectedChallenge = it },
                onCancel = onCancel
            )
        } else {
            when (selectedChallenge) {
                ChallengeType.MIND -> MindChallenge(
                    difficulty = settings.difficulty,
                    onComplete = { onChallengeComplete(ChallengeType.MIND) },
                    onCancel = { selectedChallenge = null }
                )
                ChallengeType.FOCUS -> FocusChallenge(
                    durationSecs = if (settings.difficulty == "HARD") 60 else if (settings.difficulty == "NORMAL") 30 else 15,
                    onComplete = { onChallengeComplete(ChallengeType.FOCUS) },
                    onCancel = { selectedChallenge = null }
                )
                ChallengeType.TYPING -> TypingChallenge(
                    difficulty = settings.difficulty,
                    onComplete = { onChallengeComplete(ChallengeType.TYPING) },
                    onCancel = { selectedChallenge = null }
                )
                ChallengeType.PIN -> PinChallenge(
                    settings = settings,
                    onComplete = { onChallengeComplete(ChallengeType.PIN) },
                    onCancel = { selectedChallenge = null }
                )
                null -> {}
            }
        }
    }
}

@Composable
fun ChallengeSelectionScreen(
    settings: UserSettings,
    onSelect: (ChallengeType) -> Unit,
    onCancel: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "VERIFICATION GATEWAY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            ),
            color = primaryCyan
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Complete a challenge to unlock a temporary session.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(36.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            if (!settings.mindChallengeEnabled && !settings.focusChallengeEnabled && !settings.typingChallengeEnabled && !settings.pinUnlockEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No verification methods configured. Enable challenges in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (settings.mindChallengeEnabled) {
                    ChallengeGlassOptionCard(
                        title = "Mental Math Challenge",
                        subtitle = "Solve an arithmetic problem",
                        icon = Icons.Default.Psychology,
                        onClick = { onSelect(ChallengeType.MIND) }
                    )
                }
                if (settings.focusChallengeEnabled) {
                    ChallengeGlassOptionCard(
                        title = "Patience & Stillness",
                        subtitle = "Mindful pause countdown",
                        icon = Icons.Default.Timer,
                        onClick = { onSelect(ChallengeType.FOCUS) }
                    )
                }
                if (settings.typingChallengeEnabled) {
                    ChallengeGlassOptionCard(
                        title = "Mindful Intention Typing",
                        subtitle = "Type an affirmation prompt",
                        icon = Icons.Default.Keyboard,
                        onClick = { onSelect(ChallengeType.TYPING) }
                    )
                }
                if (settings.pinUnlockEnabled) {
                    ChallengeGlassOptionCard(
                        title = "Secure PIN Authorization",
                        subtitle = "Enter your preset passcode",
                        icon = Icons.Default.Dialpad,
                        onClick = { onSelect(ChallengeType.PIN) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(onClick = onCancel) {
            Text(
                "Return to Block Screen",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ChallengeGlassOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp), isElevated = true)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryCyan.copy(alpha = 0.18f))
                    .border(1.dp, primaryCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = primaryCyan, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
fun MindChallenge(difficulty: String, onComplete: () -> Unit, onCancel: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var expected by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    LaunchedEffect(Unit) {
        val a = (12..48).random()
        val b = (12..48).random()
        question = "What is $a + $b?"
        expected = a + b
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true, isHighlight = true)
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Mental Math Challenge",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    question,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = primaryCyan
                )
                Spacer(modifier = Modifier.height(28.dp))
                
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; error = false },
                    label = { Text("Your Answer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (answer.toIntOrNull() == expected) {
                            onComplete()
                        } else {
                            error = true
                            answer = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Verify Answer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
        }
    }
}

@Composable
fun FocusChallenge(durationSecs: Int, onComplete: () -> Unit, onCancel: () -> Unit) {
    var countdown by remember { mutableIntStateOf(durationSecs) }
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)
    
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        } else {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true, isHighlight = true)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Patience & Stillness",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Keep your awareness steady without closing this view.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(primaryCyan.copy(alpha = 0.15f))
                        .border(2.dp, primaryCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$countdown",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = primaryCyan
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onCancel) {
            Text("Give Up", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
        }
    }
}

@Composable
fun TypingChallenge(difficulty: String, onComplete: () -> Unit, onCancel: () -> Unit) {
    val phrase = "I will stay focused on my goals"
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true)
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Mindful Intention Prompt",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Type the affirmation below exactly:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "\"$phrase\"",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = false },
                    label = { Text("Type affirmation") },
                    isError = error,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (input.trim().equals(phrase, ignoreCase = true)) onComplete() else error = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Confirm Intention", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
        }
    }
}

@Composable
fun PinChallenge(settings: UserSettings, onComplete: () -> Unit, onCancel: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true)
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Security Passcode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = false },
                    label = { Text("Enter PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (settings.pinHash.isNotEmpty() && input == settings.pinHash) onComplete() else error = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Authorize Unlock", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
        }
    }
}
