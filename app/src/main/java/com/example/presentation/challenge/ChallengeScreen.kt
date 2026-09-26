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
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.database.UserSettings
import com.example.database.isPremiumActive
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.liquidGlass
import com.example.util.TypingChallengePhrases
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
            when (val challenge = selectedChallenge) {
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
                    settings = settings,
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
    val primaryColor = if (isDark) com.example.ui.theme.SleekPrimaryDark else com.example.ui.theme.SleekPrimaryLight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = stringResource(R.string.verification_gateway),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            ),
            color = primaryColor
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.verification_gateway_desc),
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
                        text = stringResource(R.string.no_challenges_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (settings.mindChallengeEnabled) {
                    ChallengeGlassOptionCard(
                        title = stringResource(R.string.mental_math_challenge),
                        subtitle = stringResource(R.string.mental_math_subtitle),
                        icon = Icons.Default.Psychology,
                        onClick = { onSelect(ChallengeType.MIND) }
                    )
                }
                if (settings.focusChallengeEnabled) {
                    ChallengeGlassOptionCard(
                        title = stringResource(R.string.patience_stillness),
                        subtitle = stringResource(R.string.patience_stillness_subtitle),
                        icon = Icons.Default.Timer,
                        onClick = { onSelect(ChallengeType.FOCUS) }
                    )
                }
                if (settings.typingChallengeEnabled) {
                    ChallengeGlassOptionCard(
                        title = stringResource(R.string.typing_intention_title),
                        subtitle = stringResource(R.string.typing_intention_subtitle),
                        icon = Icons.Default.Keyboard,
                        onClick = { onSelect(ChallengeType.TYPING) }
                    )
                }
                if (settings.pinUnlockEnabled) {
                    ChallengeGlassOptionCard(
                        title = stringResource(R.string.secure_pin_title),
                        subtitle = stringResource(R.string.secure_pin_subtitle),
                        icon = Icons.Default.Dialpad,
                        onClick = { onSelect(ChallengeType.PIN) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.return_to_block_screen),
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
    val primaryCyan = Color(0xFF24DFEC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), isElevated = false)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x283E4C5E))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = primaryCyan, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MindChallenge(difficulty: String, onComplete: () -> Unit, onCancel: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var a by remember { mutableIntStateOf((12..48).random()) }
    var b by remember { mutableIntStateOf((12..48).random()) }
    val expected by remember(a, b) { derivedStateOf { a + b } }
    var error by remember { mutableStateOf(false) }
    val primaryColor = com.example.ui.theme.SleekPrimaryDark

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
                    text = stringResource(R.string.mental_math_challenge),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.what_is_math, a, b),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = primaryColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; error = false },
                    label = { Text(stringResource(R.string.your_answer)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (error) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.incorrect_try_again),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (answer.trim().toIntOrNull() == expected) {
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
                    Text(
                        text = stringResource(R.string.verify_answer),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.cancel),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun FocusChallenge(durationSecs: Int, onComplete: () -> Unit, onCancel: () -> Unit) {
    var countdown by remember { mutableIntStateOf(durationSecs) }
    val primaryColor = com.example.ui.theme.SleekPrimaryDark
    
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
                    text = stringResource(R.string.patience_stillness),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.patience_stillness_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f))
                        .border(2.dp, primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$countdown",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = primaryColor
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.give_up),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun TypingChallenge(settings: UserSettings, onComplete: () -> Unit, onCancel: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLang = remember(settings.language) {
        val saved = com.example.util.LocaleHelper.getSavedLanguage(context)
        if (saved.isNotBlank()) saved else settings.language
    }

    var phrase by remember {
        mutableStateOf(TypingChallengePhrases.getRandomPhrase(currentLang))
    }
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
                .liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true, isHighlight = true)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mindful_intention_prompt),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = {
                            var newPhrase = TypingChallengePhrases.getRandomPhrase(currentLang)
                            // Pick a different sentence if possible
                            var attempts = 0
                            while (newPhrase == phrase && attempts < 5) {
                                newPhrase = TypingChallengePhrases.getRandomPhrase(currentLang)
                                attempts++
                            }
                            phrase = newPhrase
                            input = ""
                            error = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.new_sentence),
                            tint = Color(0xFF00E5FF)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.type_affirmation_exact),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Highlighted quote bubble
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x2000E5FF))
                        .border(1.dp, Color(0x5000E5FF), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "\"$phrase\"",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        ),
                        color = Color(0xFF00E5FF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = input,
                    onValueChange = { 
                        input = it
                        error = false 
                    },
                    placeholder = { 
                        Text(
                            stringResource(R.string.type_affirmation_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    isError = error,
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (error) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.incorrect_try_again),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val cleanInput = input.trim().replace("\n", " ").replace("  ", " ")
                        val cleanPhrase = phrase.trim().replace("  ", " ")
                        if (cleanInput.equals(cleanPhrase, ignoreCase = true) || cleanInput == cleanPhrase) {
                            onComplete()
                        } else {
                            error = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(R.string.confirm_intention),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onCancel) {
            Text(
                stringResource(R.string.cancel),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
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
                    text = stringResource(R.string.security_passcode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = false },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Incorrect PIN. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (settings.pinHash.isNotEmpty() && input == settings.pinHash) {
                            onComplete()
                        } else {
                            error = true
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.authorize_unlock),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.cancel),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }
    }
}
