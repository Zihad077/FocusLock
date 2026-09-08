package com.example.presentation.challenge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.database.UserSettings
import kotlinx.coroutines.delay

enum class ChallengeType { MIND, FOCUS, TYPING, PIN }

@Composable
fun ChallengeScreen(
    settings: UserSettings,
    onChallengeComplete: (ChallengeType) -> Unit,
    onCancel: () -> Unit
) {
    var selectedChallenge by remember { mutableStateOf<ChallengeType?>(null) }

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

@Composable
fun ChallengeSelectionScreen(
    settings: UserSettings,
    onSelect: (ChallengeType) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "VERIFY UNLOCK",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Choose a verification method to grant temporary access.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!settings.mindChallengeEnabled && !settings.focusChallengeEnabled && !settings.typingChallengeEnabled && !settings.pinUnlockEnabled) {
                Text(
                    text = "No verification methods are enabled. Enable them in settings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else {
                if (settings.mindChallengeEnabled) {
                    ChallengeOptionButton(
                        title = "Quick Mind Challenge",
                        icon = Icons.Default.Psychology,
                        onClick = { onSelect(ChallengeType.MIND) }
                    )
                }
                if (settings.focusChallengeEnabled) {
                    ChallengeOptionButton(
                        title = "Focus Challenge",
                        icon = Icons.Default.Timer,
                        onClick = { onSelect(ChallengeType.FOCUS) }
                    )
                }
                if (settings.typingChallengeEnabled) {
                    ChallengeOptionButton(
                        title = "Typing Challenge",
                        icon = Icons.Default.Keyboard,
                        onClick = { onSelect(ChallengeType.TYPING) }
                    )
                }
                if (settings.pinUnlockEnabled) {
                    ChallengeOptionButton(
                        title = "PIN Unlock",
                        icon = Icons.Default.Dialpad,
                        onClick = { onSelect(ChallengeType.PIN) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ChallengeOptionButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MindChallenge(difficulty: String, onComplete: () -> Unit, onCancel: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val a = (10..50).random()
        val b = (10..50).random()
        question = "What is $a + $b?"
        expected = a + b
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Mind Challenge", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(24.dp))
        Text(question, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; error = false },
            label = { Text("Your Answer") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (answer.toIntOrNull() == expected) {
                    onComplete()
                } else {
                    error = true
                    answer = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Submit")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
fun FocusChallenge(durationSecs: Int, onComplete: () -> Unit, onCancel: () -> Unit) {
    var countdown by remember { mutableStateOf(durationSecs) }
    
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        } else {
            onComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Focus Challenge", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Do not close this screen.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "$countdown",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        TextButton(onClick = onCancel) { Text("Give Up") }
    }
}

@Composable
fun TypingChallenge(difficulty: String, onComplete: () -> Unit, onCancel: () -> Unit) {
    val phrase = "I will stay focused on my goals"
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Typing Challenge", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Type exactly:", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(phrase, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = false },
            label = { Text("Type here") },
            isError = error,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (input.trim() == phrase) onComplete() else error = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Submit")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
fun PinChallenge(settings: UserSettings, onComplete: () -> Unit, onCancel: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PIN Unlock", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = false },
            label = { Text("Enter PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (input == settings.pinHash || settings.pinHash.isEmpty()) onComplete() else error = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Unlock")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
