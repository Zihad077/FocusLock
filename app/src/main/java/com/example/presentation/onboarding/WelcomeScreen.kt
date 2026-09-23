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
import androidx.compose.ui.draw.scale
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
    val context = LocalContext.current
    val primaryColor = com.example.ui.theme.SleekPrimaryDark
    
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
                        isDark = true,
                        primaryColor = primaryColor,
                        onGetStarted = { currentStep = 1 }
                    )
                } else {
                    TermsPrivacyView(
                        isDark = true,
                        primaryColor = primaryColor,
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
                TermsDialog(onDismiss = { showFullTermsDialog = false })
            }
            if (showFullPrivacyDialog) {
                PrivacyDialog(onDismiss = { showFullPrivacyDialog = false })
            }
        }
    }
}

@Composable
private fun WelcomeHookView(
    isDark: Boolean = true,
    primaryColor: Color,
    onGetStarted: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Breathing ambient glow animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo_ambient")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

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

        // Ambient Mindfulness Tag Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x2200E5FF))
                .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_badge),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFF00E5FF)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Background-Removed Clean Floating 3D Logo with Multi-Layer Glow Aura
        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(glowScale),
            contentAlignment = Alignment.Center
        ) {
            // Outermost soft glowing cyan pulse halo
            Box(
                modifier = Modifier
                    .size(145.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.45f),
                                Color(0xFF2979FF).copy(alpha = glowAlpha * 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner glass rim and logo container
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFF00E5FF),
                        ambientColor = Color(0xFF2979FF)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x2800E5FF),
                                Color(0x600B101D)
                            )
                        )
                    )
                    .border(
                        width = 1.8.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF00E5FF),
                                Color(0x40FFFFFF),
                                Color(0xFF2979FF),
                                Color(0xFF00E5FF)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(8.dp),
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hooking Title
        Text(
            text = stringResource(R.string.welcome_hero_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                lineHeight = 34.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Hooking Subtitle Description
        Text(
            text = stringResource(R.string.welcome_hero_desc),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
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
                accentColor = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            )
            MetricPillarCard(
                headline = stringResource(R.string.stat_reduction),
                subtitle = stringResource(R.string.stat_reduction_desc),
                accentColor = Color(0xFFFFAB00),
                modifier = Modifier.weight(1f)
            )
            MetricPillarCard(
                headline = stringResource(R.string.stat_privacy),
                subtitle = stringResource(R.string.stat_privacy_desc),
                accentColor = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 4 Key Value Pillars in Liquid Glass
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductivityFeatureRow(
                icon = Icons.Default.Shield,
                title = stringResource(R.string.feature_friction_title),
                subtitle = stringResource(R.string.feature_friction_desc),
                accentColor = Color(0xFF00E5FF)
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Psychology,
                title = stringResource(R.string.feature_challenges_title),
                subtitle = stringResource(R.string.feature_challenges_desc),
                accentColor = Color(0xFF2979FF)
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.feature_deepwork_title),
                subtitle = stringResource(R.string.feature_deepwork_desc),
                accentColor = Color(0xFFFFAB00)
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.feature_privacy_title),
                subtitle = stringResource(R.string.feature_privacy_desc),
                accentColor = Color(0xFF00E676)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // High-Energy Primary Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF00E5FF))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF2979FF)
                        )
                    )
                )
                .border(1.2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.get_started_btn),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        ),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun TermsPrivacyView(
    isDark: Boolean,
    primaryColor: Color,
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
            .padding(horizontal = 24.dp)
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
                    .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Legal & Privacy",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Shield / Trust Icon in Glass Bubble
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(12.dp, CircleShape, spotColor = primaryColor)
                .clip(CircleShape)
                .liquidGlass(shape = CircleShape, isHighlight = true),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Privacy & Trust Matter",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Before we configure your device permissions, please review and acknowledge our Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary Cards
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(22.dp), isHighlight = false)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    TextButton(onClick = onViewTerms) {
                        Text("Read", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    TextButton(onClick = onViewPrivacy) {
                        Text("Read", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Highlights
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = if (isDark) 0.05f else 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BulletPoint(text = "FocusLock operates locally and offline on your device.")
                BulletPoint(text = "No personal browsing history, keystrokes, or screen data is collected or sold.")
                BulletPoint(text = "App blocking services require specific system permissions to function.")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Checkbox Agreement
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onTermsAgreedChange(!termsAgreed) }
                .background(
                    if (termsAgreed) primaryColor.copy(alpha = if (isDark) 0.15f else 0.1f)
                    else Color.Transparent
                )
                .border(
                    1.dp,
                    if (termsAgreed) primaryColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAgreed,
                    onCheckedChange = onTermsAgreedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = primaryColor,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "I have read and agree to the Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Continue Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (termsAgreed) 12.dp else 0.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = primaryColor.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (termsAgreed) {
                        Brush.horizontalGradient(
                            listOf(primaryColor, primaryColor.copy(alpha = 0.8f))
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color.Gray.copy(alpha = 0.35f), Color.Gray.copy(alpha = 0.45f))
                        )
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onContinue,
                enabled = termsAgreed,
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Agree & Continue to Permissions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (termsAgreed) Color.White else Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (termsAgreed) Color.White else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
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
            .liquidGlass(shape = RoundedCornerShape(18.dp), isElevated = false)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
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
            .liquidGlass(shape = RoundedCornerShape(20.dp), isElevated = false)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) com.example.ui.theme.SleekPrimaryDark else com.example.ui.theme.SleekPrimaryLight

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(primaryColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
        )
    }
}

@Composable
private fun TermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Terms of Service", fontWeight = FontWeight.Bold)
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
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "1. App Purpose: FocusLock provides digital wellbeing tools including app usage limiting, screen-time statistics, and mindful focus sessions.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "2. Self-Discipline Responsibility: You control which apps to limit and which challenge verifications to enable. FocusLock does not permanently restrict your device.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "3. System Privileges: FocusLock utilizes standard Android Accessibility and Overlay APIs solely for enforcing your selected distraction boundaries.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "4. Updates & Changes: Features may be updated to maintain compatibility with modern Android versions.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Privacy Policy", fontWeight = FontWeight.Bold)
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
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• 100% Offline Database: Your app limits, focus history, gamification XP, and daily statistics are stored in a local SQLite database on your device.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• Zero Telemetry: We do not log, upload, or sell your keystrokes, personal messages, or visited URLs.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• Accessibility Data: Used exclusively in real-time on-device to inspect foreground app package names to display blocking screens when limits expire.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• Advertisements & Consent: Ads (if enabled) comply strictly with Google AdMob & UMP user consent standards.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
