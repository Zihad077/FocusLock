package com.example.presentation.onboarding

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) com.example.ui.theme.SleekPrimaryDark else com.example.ui.theme.SleekPrimaryLight
    
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
                        isDark = isDark,
                        primaryColor = primaryColor,
                        onGetStarted = { currentStep = 1 }
                    )
                } else {
                    TermsPrivacyView(
                        isDark = isDark,
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
    isDark: Boolean,
    primaryColor: Color,
    onGetStarted: () -> Unit
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
        Spacer(modifier = Modifier.height(20.dp))

        // Hero Visual Card with Liquid Glass Border
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1.15f)
                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = primaryColor.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(32.dp))
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f),
                            primaryColor.copy(alpha = 0.2f)
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.welcome_illustration_1789049712801),
                contentDescription = "FocusLock Flow Illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Take Back Your Focus",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "FocusLock creates a calm, secure space on your device to protect your deepest work and build healthy habits.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 3 Key Productivity Value Pillars in Glass Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductivityFeatureRow(
                icon = Icons.Default.Shield,
                title = "Friction Overlays",
                subtitle = "Mindful interventions to prevent impulsive app opens."
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Timer,
                title = "Deep Focus Sessions",
                subtitle = "Strict time blocks with limits to build digital discipline."
            )
            ProductivityFeatureRow(
                icon = Icons.Default.Lock,
                title = "Private & On-Device",
                subtitle = "Zero tracking. Your usage stats stay completely on your device."
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = primaryColor.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(primaryColor, primaryColor.copy(alpha = 0.8f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
private fun ProductivityFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) com.example.ui.theme.SleekPrimaryDark else com.example.ui.theme.SleekPrimaryLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(18.dp), isHighlight = false)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.15f))
                    .border(1.dp, primaryColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
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
                        lineHeight = 16.sp
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
