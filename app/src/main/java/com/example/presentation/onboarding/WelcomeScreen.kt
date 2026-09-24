package com.example.presentation.onboarding

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LiquidBackground
import com.example.ui.theme.liquidGlass

@Composable
fun WelcomeScreen(onNavigateToPermissions: () -> Unit) {
    val primaryCyan = Color(0xFF24DFEC)
    
    // Step 0: Welcome Productivity Hook, Step 1: Terms & Privacy Agreement
    var currentStep by remember { mutableIntStateOf(0) }
    var termsAgreed by remember { mutableStateOf(false) }
    var showFullTermsDialog by remember { mutableStateOf(false) }
    var showFullPrivacyDialog by remember { mutableStateOf(false) }

    LiquidBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { -it } + fadeOut(tween(350)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(350)) { -it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { it } + fadeOut(tween(350)))
                    }
                },
                label = "welcome_flow_step"
            ) { step ->
                if (step == 0) {
                    WelcomeHookView(
                        primaryCyan = primaryCyan,
                        onGetStarted = { currentStep = 1 }
                    )
                } else {
                    TermsPrivacyView(
                        primaryCyan = primaryCyan,
                        termsAgreed = termsAgreed,
                        onTermsAgreedChange = { termsAgreed = it },
                        onViewTerms = { showFullTermsDialog = true },
                        onViewPrivacy = { showFullPrivacyDialog = true },
                        onContinue = onNavigateToPermissions,
                        onBack = { currentStep = 0 }
                    )
                }
            }

            // Dialogs for full Terms and Privacy reading
            if (showFullTermsDialog) {
                TermsDialog(onDismiss = { showFullTermsDialog = false }, primaryCyan = primaryCyan)
            }
            if (showFullPrivacyDialog) {
                PrivacyDialog(onDismiss = { showFullPrivacyDialog = false }, primaryCyan = primaryCyan)
            }
        }
    }
}

@Composable
private fun WelcomeHookView(
    primaryCyan: Color,
    onGetStarted: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Ambient Mindfulness Tag Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(primaryCyan.copy(alpha = 0.18f))
                .border(1.dp, primaryCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_badge),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = primaryCyan
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Soft Clean Translucent Glass Logo Container
        Box(
            modifier = Modifier
                .size(116.dp)
                .liquidGlass(shape = CircleShape)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_focuslock_clean_logo),
                contentDescription = "FocusLock Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hooking Title
        Text(
            text = stringResource(R.string.welcome_hero_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                fontSize = 28.sp,
                lineHeight = 34.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Hooking Subtitle Description
        Text(
            text = stringResource(R.string.welcome_hero_desc),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        // High-Impact 3-Column Glass Metric Pillars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricPillarCard(
                headline = stringResource(R.string.stat_hours_saved),
                subtitle = stringResource(R.string.stat_hours_saved_desc),
                accentColor = primaryCyan,
                modifier = Modifier.weight(1f)
            )
            MetricPillarCard(
                headline = stringResource(R.string.stat_reduction),
                subtitle = stringResource(R.string.stat_reduction_desc),
                accentColor = Color(0xFFFFB300),
                modifier = Modifier.weight(1f)
            )
            MetricPillarCard(
                headline = stringResource(R.string.stat_privacy),
                subtitle = stringResource(R.string.stat_privacy_desc),
                accentColor = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4 Key Value Pillars in Liquid Glass
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductivityFeatureRow(
                icon = Icons.Default.Shield,
                title = stringResource(R.string.feature_friction_title),
                subtitle = stringResource(R.string.feature_friction_desc),
                accentColor = primaryCyan
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Psychology,
                title = stringResource(R.string.feature_challenges_title),
                subtitle = stringResource(R.string.feature_challenges_desc),
                accentColor = Color(0xFF40C4FF)
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.feature_deepwork_title),
                subtitle = stringResource(R.string.feature_deepwork_desc),
                accentColor = Color(0xFFFFB300)
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.feature_privacy_title),
                subtitle = stringResource(R.string.feature_privacy_desc),
                accentColor = Color(0xFF00E676)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // High-Energy Primary Action Button (Matches Permission Center reference style)
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryCyan,
                contentColor = Color(0xFF0C1929)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.get_started_btn),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun TermsPrivacyView(
    primaryCyan: Color,
    termsAgreed: Boolean,
    onTermsAgreedChange: (Boolean) -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation / Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Legal & Privacy",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Shield / Trust Icon in Glass Bubble
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .liquidGlass(shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = primaryCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Privacy & Trust Matter",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Before configuring device permissions, please review and acknowledge our Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Summary Cards
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }
                    TextButton(onClick = onViewTerms) {
                        Text("Read", color = primaryCyan, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }
                    TextButton(onClick = onViewPrivacy) {
                        Text("Read", color = primaryCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Highlights
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BulletPoint(text = "FocusLock operates locally and offline on your device.", primaryCyan = primaryCyan)
                BulletPoint(text = "No personal browsing history, keystrokes, or screen data is collected or sold.", primaryCyan = primaryCyan)
                BulletPoint(text = "App blocking services require specific system permissions to function.", primaryCyan = primaryCyan)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Checkbox Agreement
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onTermsAgreedChange(!termsAgreed) }
                .background(
                    if (termsAgreed) primaryCyan.copy(alpha = 0.15f)
                    else Color(0x283E4C5E)
                )
                .border(
                    1.dp,
                    if (termsAgreed) primaryCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.18f),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAgreed,
                    onCheckedChange = onTermsAgreedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = primaryCyan,
                        checkmarkColor = Color(0xFF0C1929),
                        uncheckedColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "I have read and agree to the Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            enabled = termsAgreed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryCyan,
                contentColor = Color(0xFF0C1929),
                disabledContainerColor = Color(0x353E4C5E),
                disabledContentColor = Color.White.copy(alpha = 0.4f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Agree & Continue to Permissions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricPillarCard(
    headline: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                ),
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProductivityFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String, primaryCyan: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(primaryCyan)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
private fun TermsDialog(onDismiss: () -> Unit, primaryCyan: Color) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF162534),
        title = {
            Text("Terms of Service", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scroll)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Welcome to FocusLock. By using our application, you agree to the following terms:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                )
                Text(
                    "1. App Purpose: FocusLock provides digital wellbeing tools including app usage limiting, screen-time statistics, and mindful focus sessions.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "2. Self-Discipline Responsibility: You control which apps to limit and which challenge verifications to enable. FocusLock does not permanently restrict your device.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "3. System Privileges: FocusLock utilizes standard Android Accessibility and Overlay APIs solely for enforcing your selected distraction boundaries.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "4. Updates & Changes: Features may be updated to maintain compatibility with modern Android versions.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryCyan,
                    contentColor = Color(0xFF0C1929)
                )
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit, primaryCyan: Color) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF162534),
        title = {
            Text("Privacy Policy", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scroll)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "FocusLock is engineered with an offline-first, privacy-by-design philosophy:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                )
                Text(
                    "• 100% Offline Database: Your app limits, focus history, gamification XP, and daily statistics are stored in a local SQLite database on your device.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "• Zero Telemetry: We do not log, upload, or sell your keystrokes, personal messages, or visited URLs.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "• Accessibility Data: Used exclusively in real-time on-device to inspect foreground app package names to display blocking screens when limits expire.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "• Advertisements & Consent: Ads (if enabled) comply strictly with Google AdMob & UMP user consent standards.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryCyan,
                    contentColor = Color(0xFF0C1929)
                )
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
