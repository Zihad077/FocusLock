package com.example.presentation.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Smooth Hermite interpolation for choreographed timeline sub-segments.
 */
private fun smoothPhase(progress: Float, start: Float, end: Float): Float {
    if (progress <= start) return 0f
    if (progress >= end) return 1f
    val t = ((progress - start) / (end - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

private data class StarParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val baseAlpha: Float,
    val speedFactor: Float,
    val isCyanTinted: Boolean
)

private val AmbientParticles = listOf(
    StarParticle(0.12f, 0.18f, 2.2f, 0.55f, 1.1f, true),
    StarParticle(0.85f, 0.14f, 1.8f, 0.45f, 0.8f, false),
    StarParticle(0.24f, 0.32f, 1.5f, 0.40f, 1.3f, true),
    StarParticle(0.76f, 0.28f, 2.5f, 0.60f, 0.9f, true),
    StarParticle(0.08f, 0.48f, 3.2f, 0.35f, 0.7f, true),
    StarParticle(0.92f, 0.44f, 2.0f, 0.50f, 1.2f, false),
    StarParticle(0.18f, 0.68f, 2.8f, 0.45f, 1.0f, true),
    StarParticle(0.82f, 0.64f, 1.6f, 0.40f, 1.4f, false),
    StarParticle(0.35f, 0.12f, 1.4f, 0.35f, 0.9f, true),
    StarParticle(0.64f, 0.16f, 2.1f, 0.50f, 1.1f, false),
    StarParticle(0.15f, 0.84f, 3.6f, 0.30f, 0.6f, true),
    StarParticle(0.88f, 0.82f, 2.6f, 0.42f, 0.8f, false),
    StarParticle(0.48f, 0.22f, 1.3f, 0.38f, 1.2f, true),
    StarParticle(0.54f, 0.76f, 1.9f, 0.35f, 1.0f, true),
    StarParticle(0.29f, 0.54f, 1.4f, 0.30f, 1.3f, false),
    StarParticle(0.71f, 0.52f, 1.7f, 0.32f, 0.9f, true),
    StarParticle(0.06f, 0.29f, 2.4f, 0.42f, 0.8f, true),
    StarParticle(0.94f, 0.26f, 1.5f, 0.36f, 1.1f, false),
    StarParticle(0.41f, 0.89f, 2.2f, 0.40f, 0.7f, true),
    StarParticle(0.62f, 0.91f, 1.8f, 0.38f, 1.0f, false)
)

/**
 * Redesigned First-Launch Opening Animation & Welcome Screen for FocusLock.
 *
 * Implements a continuous, choreographed 60fps experience modeled after the reference video:
 * - 0.00..0.18: Deep AMOLED atmosphere with cyan/purple aurora waves; actual FocusLock logo emerges in center.
 * - 0.14..0.28: "FocusLock" wordmark materializes beneath the logo.
 * - 0.28..0.44: Logo & wordmark glide upward while 3D Liquid Glass cards rise from depth with perspective.
 * - 0.38..0.68: 3D Smartphone reveal frames the emerging interface with "Take Back Your Focus." and 4 real FocusLock pillar cards.
 * - 0.66..0.90: Feature cards converge and morph into the central Liquid Glass Focus Stopwatch illustration while the 3D phone frame expands seamlessly into the real Android viewport.
 * - 0.82..1.00: Prominent rounded cyan/turquoise "Get Started" button and understated privacy reassurance footer settle into place for immediate interaction.
 */
@Composable
fun WelcomeScreen(onNavigateToPermissions: () -> Unit) {
    val primaryCyan = Color(0xFF24DFEC)
    val brightTurquoise = Color(0xFF33E8F5)
    val deepViolet = Color(0xFF7C4DFF)
    val softPurple = Color(0xFF9575CD)

    val coroutineScope = rememberCoroutineScope()
    var showFullTermsDialog by remember { mutableStateOf(false) }
    var showFullPrivacyDialog by remember { mutableStateOf(false) }

    // Master choreographed timeline: 0f (start of 3D reveal) -> 1f (interactive Welcome Screen)
    val masterProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        masterProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 6400,
                easing = LinearEasing
            )
        )
    }

    val p = masterProgress.value
    val isAnimationSettled = p >= 0.96f

    // Subtle continuous ambient motion for background & timer hand once visible
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_ambient")
    val ambientDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_drift"
    )
    val timerBreathing by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timer_breathing"
    )

    // Choreographed phase weights (0f..1f)
    val logoEnter = smoothPhase(p, 0.02f, 0.17f)
    val brandTextEnter = smoothPhase(p, 0.13f, 0.26f)
    val logoMoveToTop = smoothPhase(p, 0.27f, 0.45f)
    val brandTextCompact = smoothPhase(p, 0.68f, 0.84f)

    // 3D Glass cards & smartphone frame emergence (00:04 - 00:07 in reference video)
    val glassCardsEnter = smoothPhase(p, 0.32f, 0.48f)
    val cardContentEnter = smoothPhase(p, 0.45f, 0.58f)
    val glassCardsMorphOut = smoothPhase(p, 0.67f, 0.81f)

    // 3D Smartphone bezel reveal & expansion into full screen (00:05 - 00:09 in reference video)
    val phoneFrameEnter = smoothPhase(p, 0.36f, 0.52f)
    val phoneFrameExpandOut = smoothPhase(p, 0.78f, 0.95f)
    val phoneFrameAlpha = (phoneFrameEnter * (1f - phoneFrameExpandOut)).coerceIn(0f, 1f)

    // Headline & Supporting text ("Take Back Your Focus.")
    val headlineEnter = smoothPhase(p, 0.40f, 0.56f)
    val subtitleEnter = smoothPhase(p, 0.47f, 0.62f)

    // Central Focus Stopwatch illustration reveal (00:08 - 00:09 in reference video)
    val centralTimerEnter = smoothPhase(p, 0.69f, 0.87f)

    // Bottom "Get Started" CTA button & privacy reassurance entrance
    val ctaEnter = smoothPhase(p, 0.80f, 0.96f)

    val density = LocalDensity.current.density

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030710))
    ) {
        val screenHeightDp = maxHeight
        val isCompactHeight = screenHeightDp < 700.dp

        // 1. Deep AMOLED Background with Cyan/Purple Aurora Waves & Starlight Bokeh
        WelcomeAuroraBackdrop(
            timelineProgress = p,
            ambientDrift = ambientDrift,
            primaryCyan = primaryCyan,
            deepViolet = deepViolet
        )

        // 2. 3D Smartphone Reveal Stage
        // Tilts in 3D perspective during mid-animation and settles flat at 0 deg as it becomes the real screen
        val stageTiltX = lerpFloat(
            start = lerpFloat(0f, 11f, glassCardsEnter),
            stop = 0f,
            fraction = smoothPhase(p, 0.54f, 0.82f)
        )
        val stageTiltY = lerpFloat(
            start = lerpFloat(0f, -6f, phoneFrameEnter),
            stop = 0f,
            fraction = smoothPhase(p, 0.54f, 0.82f)
        )
        val stageScale = lerpFloat(
            start = lerpFloat(1f, 0.90f, phoneFrameEnter),
            stop = 1f,
            fraction = phoneFrameExpandOut
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    cameraDistance = 18f * density
                    rotationX = stageTiltX
                    rotationY = stageTiltY
                    scaleX = stageScale
                    scaleY = stageScale
                },
            contentAlignment = Alignment.Center
        ) {
            // 3D Smartphone Bezel Overlay (visible during 00:05..00:08 reveal, expands seamlessly into user's real screen)
            if (phoneFrameAlpha > 0.01f) {
                SmartphoneRevealFrame(
                    alpha = phoneFrameAlpha,
                    expandProgress = phoneFrameExpandOut,
                    primaryCyan = primaryCyan,
                    deepViolet = deepViolet
                )
            }

            // Main Coordinated Content Hierarchy
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 14.dp else 24.dp))

                // --- HEADER: Actual FocusLock Logo + Brand Wordmark ---
                // Starts in the vertical center of the screen (00:01-00:02), then glides smoothly to the top (00:03+)
                val startCenterOffsetPx = with(LocalDensity.current) {
                    (screenHeightDp * 0.27f).toPx()
                }
                val logoTranslationY = lerpFloat(startCenterOffsetPx, 0f, logoMoveToTop)
                val logoScale = lerpFloat(
                    start = lerpFloat(0.65f, 1.12f, logoEnter),
                    stop = lerpFloat(0.95f, 0.84f, brandTextCompact),
                    fraction = logoMoveToTop
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        translationY = logoTranslationY
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoEnter
                    }
                ) {
                    // Actual FocusLock Launcher Logo with Cyan/Purple Aura
                    FocusLockGenuineLogoBadge(
                        primaryCyan = primaryCyan,
                        deepViolet = deepViolet,
                        glowIntensity = lerpFloat(1f, 0.75f, brandTextCompact)
                    )

                    // "FocusLock" Brand Wordmark (prominent in 00:02-00:07, refines smoothly in 00:08-00:09)
                    val brandAlpha = brandTextEnter * lerpFloat(1f, 0.92f, brandTextCompact)
                    val brandScale = lerpFloat(1.0f, 0.86f, brandTextCompact)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FocusLock",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = Color.White.copy(alpha = brandAlpha),
                        modifier = Modifier.graphicsLayer {
                            scaleX = brandScale
                            scaleY = brandScale
                            translationY = lerpFloat(16f, 0f, brandTextEnter)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 18.dp))

                // --- HEADLINE & SUPPORTING TEXT ---
                // "Take Back Your Focus." + "Set limits. Stay focused. Make time for what matters."
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = headlineEnter
                            translationY = lerpFloat(36f, 0f, headlineEnter)
                            scaleX = lerpFloat(0.94f, 1f, headlineEnter)
                            scaleY = lerpFloat(0.94f, 1f, headlineEnter)
                        }
                ) {
                    Text(
                        text = "Take Back\nYour Focus.",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isCompactHeight) 31.sp else 36.sp,
                            lineHeight = if (isCompactHeight) 37.sp else 42.sp,
                            letterSpacing = (-0.6).sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Set limits. Stay focused.\nMake time for what matters.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = if (isCompactHeight) 14.5.sp else 16.sp,
                            lineHeight = 22.sp
                        ),
                        color = Color.White.copy(alpha = 0.78f * subtitleEnter),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            translationY = lerpFloat(18f, 0f, subtitleEnter)
                        }
                    )
                }

                // --- CENTRAL STAGE: 3D Emerging Glass Cards (00:04-00:07) -> Morph into Focus Stopwatch (00:08-00:09) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Layer A: 3D Emerging Liquid Glass Feature Grid (00:04 - 00:07)
                    val cardsAlpha = (glassCardsEnter * (1f - glassCardsMorphOut)).coerceIn(0f, 1f)
                    if (cardsAlpha > 0.01f) {
                        EmergingGlassCardsShowcase(
                            enterProgress = glassCardsEnter,
                            contentProgress = cardContentEnter,
                            morphOutProgress = glassCardsMorphOut,
                            primaryCyan = primaryCyan,
                            softPurple = softPurple,
                            density = density
                        )
                    }

                    // Layer B: Central Liquid Glass Focus Stopwatch Illustration (00:08 - 00:09 & Final Welcome Screen)
                    if (centralTimerEnter > 0.01f) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = centralTimerEnter
                                    val revealScale = lerpFloat(0.68f, 1f, centralTimerEnter) *
                                            if (isAnimationSettled) timerBreathing else 1f
                                    scaleX = revealScale
                                    scaleY = revealScale
                                    rotationX = lerpFloat(22f, 0f, centralTimerEnter)
                                    cameraDistance = 16f * density
                                }
                        ) {
                            CentralFocusStopwatchIllustration(
                                revealProgress = centralTimerEnter,
                                ambientDrift = ambientDrift,
                                primaryCyan = primaryCyan,
                                deepViolet = deepViolet,
                                sizeDp = if (isCompactHeight) 200.dp else 232.dp
                            )
                        }
                    }
                }

                // --- BOTTOM CTA: Rounded Cyan/Turquoise "Get Started" Button & Reassurance Text ---
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isCompactHeight) 8.dp else 14.dp)
                        .graphicsLayer {
                            alpha = ctaEnter
                            translationY = lerpFloat(42f, 0f, ctaEnter)
                            scaleX = lerpFloat(0.92f, 1f, ctaEnter)
                            scaleY = lerpFloat(0.92f, 1f, ctaEnter)
                        }
                ) {
                    Button(
                        onClick = onNavigateToPermissions,
                        modifier = Modifier
                            .testTag("get_started_button")
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = primaryCyan,
                                spotColor = primaryCyan
                            ),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF04141F)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF2CE8F5),
                                            Color(0xFF20D6E8),
                                            Color(0xFF19C3D8)
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.65f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = Color(0xFF04141F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Small, understated privacy/reassurance text near the bottom
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = primaryCyan.copy(alpha = 0.75f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "100% private & on-device • ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.58f)
                        )
                        Text(
                            text = "Terms",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = brightTurquoise.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showFullTermsDialog = true }
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "&",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = Color.White.copy(alpha = 0.50f)
                        )
                        Text(
                            text = "Privacy",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = brightTurquoise.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showFullPrivacyDialog = true }
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Subtle Skip / Replay affordance in top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 18.dp)
        ) {
            if (!isAnimationSettled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                masterProgress.snapTo(1f)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            masterProgress.snapTo(0f)
                            masterProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 6400,
                                    easing = LinearEasing
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay opening animation",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Terms & Privacy Dialogs
        if (showFullTermsDialog) {
            TermsDialog(onDismiss = { showFullTermsDialog = false }, primaryCyan = primaryCyan)
        }
        if (showFullPrivacyDialog) {
            PrivacyDialog(onDismiss = { showFullPrivacyDialog = false }, primaryCyan = primaryCyan)
        }
    }
}

/**
 * Genuine FocusLock Launcher Icon/Logo Component.
 * Uses the exact project drawable `R.drawable.ic_custom_logo` with a subtle cyan/violet halo.
 */
@Composable
private fun FocusLockGenuineLogoBadge(
    primaryCyan: Color,
    deepViolet: Color,
    glowIntensity: Float
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(88.dp)
    ) {
        // Ambient radial cyan/purple glow behind the real FocusLock logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = 0.42f * glowIntensity),
                        deepViolet.copy(alpha = 0.22f * glowIntensity),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f
            )
        }

        // Outer Liquid Glass framing around the genuine FocusLock icon
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = primaryCyan,
                    spotColor = primaryCyan
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A1626))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryCyan.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.45f),
                            deepViolet.copy(alpha = 0.70f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_custom_logo),
                contentDescription = "FocusLock Official Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(19.dp))
            )
        }
    }
}

/**
 * 3D Emerging Liquid Glass Cards Showcase (Frames 00:04 - 00:07 of reference video).
 * First rises as translucent specular glass planes (00:04-00:05), then reveals crisp FocusLock features (00:06-00:07),
 * and smoothly converges into the central Focus Stopwatch illustration (00:08).
 */
@Composable
private fun EmergingGlassCardsShowcase(
    enterProgress: Float,
    contentProgress: Float,
    morphOutProgress: Float,
    primaryCyan: Color,
    softPurple: Color,
    density: Float
) {
    val cardTiltX = lerpFloat(28f, 0f, enterProgress)
    val cardTranslateY = lerpFloat(90f, 0f, enterProgress)
    val convergeScale = lerpFloat(1f, 0.55f, morphOutProgress)
    val containerAlpha = (enterProgress * (1f - morphOutProgress)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                cameraDistance = 16f * density
                rotationX = cardTiltX
                translationY = cardTranslateY
                scaleX = convergeScale
                scaleY = convergeScale
                alpha = containerAlpha
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper 2 Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RevealGlassFeatureCard(
                icon = Icons.Default.Nightlight,
                iconTint = primaryCyan,
                title = "Bedtime Curfew",
                subtitle = "Peaceful nights without late-night scrolling",
                contentAlpha = contentProgress,
                primaryCyan = primaryCyan,
                modifier = Modifier
                    .weight(1f)
                    .height(142.dp)
                    .graphicsLayer {
                        translationX = lerpFloat(0f, 40f, morphOutProgress)
                        translationY = lerpFloat(0f, 40f, morphOutProgress)
                    }
            )
            RevealGlassFeatureCard(
                icon = Icons.Default.PhonelinkLock,
                iconTint = softPurple,
                title = "Smart App Limits",
                subtitle = "Automatic daily caps on distracting apps",
                contentAlpha = contentProgress,
                primaryCyan = primaryCyan,
                modifier = Modifier
                    .weight(1f)
                    .height(142.dp)
                    .graphicsLayer {
                        translationX = lerpFloat(0f, -40f, morphOutProgress)
                        translationY = lerpFloat(0f, 40f, morphOutProgress)
                    }
            )
        }

        // Lower 2 Cards Row (emerges from the bottom bar in 00:04 -> 2 cards in 00:06)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RevealGlassFeatureCard(
                icon = Icons.Default.Timer,
                iconTint = primaryCyan,
                title = "Deep Work Timer",
                subtitle = "Uninterrupted focus sessions on demand",
                contentAlpha = contentProgress,
                primaryCyan = primaryCyan,
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .graphicsLayer {
                        translationX = lerpFloat(0f, 40f, morphOutProgress)
                        translationY = lerpFloat(0f, -40f, morphOutProgress)
                    }
            )
            RevealGlassFeatureCard(
                icon = Icons.Default.Shield,
                iconTint = softPurple,
                title = "Strict Shield",
                subtitle = "Mindful challenges stop impulse unlocks",
                contentAlpha = contentProgress,
                primaryCyan = primaryCyan,
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .graphicsLayer {
                        translationX = lerpFloat(0f, -40f, morphOutProgress)
                        translationY = lerpFloat(0f, -40f, morphOutProgress)
                    }
            )
        }
    }
}

@Composable
private fun RevealGlassFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    contentAlpha: Float,
    primaryCyan: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x381E324A),
                        Color(0x22101D2E),
                        Color(0x3016283E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 300f)
                )
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.25f),
                        primaryCyan.copy(alpha = 0.18f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f))
                    .border(0.8.dp, iconTint.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    ),
                    color = Color.White.copy(alpha = 0.68f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Central Focus Stopwatch / Timer Illustration with Liquid Glass rings and orbital nodes (Frames 00:08 - 00:09).
 */
@Composable
private fun CentralFocusStopwatchIllustration(
    revealProgress: Float,
    ambientDrift: Float,
    primaryCyan: Color,
    deepViolet: Color,
    sizeDp: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension / 2f

            // 1. Soft Ambient Cyan/Violet Glow Halo behind the Stopwatch
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = 0.28f),
                        deepViolet.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = maxR * 1.05f
                ),
                radius = maxR * 1.05f,
                center = Offset(cx, cy)
            )

            // 2. Outer Orbital Glass Ring
            val outerOrbitR = maxR * 0.90f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.15f),
                        deepViolet.copy(alpha = 0.38f)
                    )
                ),
                radius = outerOrbitR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.3.dp.toPx())
            )

            // Secondary Tilted Elliptical Glass Orbit Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = maxR * 0.78f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Stopwatch Top Crown & Angled Side Pusher (like 00:08 - 00:09)
            val bodyR = maxR * 0.60f
            val crownStemWidth = 10.dp.toPx()
            val crownStemHeight = 12.dp.toPx()
            drawRoundRect(
                color = Color(0xCC567396),
                topLeft = Offset(cx - crownStemWidth / 2f, cy - bodyR - crownStemHeight),
                size = Size(crownStemWidth, crownStemHeight + 4.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            val crownCapWidth = 26.dp.toPx()
            val crownCapHeight = 8.dp.toPx()
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF7FA1C7),
                        Color(0xFFB3CBE6),
                        Color(0xFF6B8CB3)
                    )
                ),
                topLeft = Offset(cx - crownCapWidth / 2f, cy - bodyR - crownStemHeight - crownCapHeight + 2.dp.toPx()),
                size = Size(crownCapWidth, crownCapHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Angled Side Pusher at ~42 degrees (top-right of stopwatch)
            rotate(degrees = 40f, pivot = Offset(cx, cy)) {
                val pusherW = 8.dp.toPx()
                val pusherH = 11.dp.toPx()
                drawRoundRect(
                    color = Color(0xCC7FA1C7),
                    topLeft = Offset(cx - pusherW / 2f, cy - bodyR - pusherH + 2.dp.toPx()),
                    size = Size(pusherW, pusherH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                val pusherCapW = 14.dp.toPx()
                val pusherCapH = 5.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFA6C2E0),
                    topLeft = Offset(cx - pusherCapW / 2f, cy - bodyR - pusherH - pusherCapH + 3.dp.toPx()),
                    size = Size(pusherCapW, pusherCapH),
                    cornerRadius = CornerRadius(2.5.dp.toPx())
                )
            }

            // 4. Outer Frosted Glass Stopwatch Bezel
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x553A5F8A),
                        Color(0x351B314C),
                        Color(0x554F3B88)
                    ),
                    start = Offset(cx - bodyR, cy - bodyR),
                    end = Offset(cx + bodyR, cy + bodyR)
                ),
                radius = bodyR,
                center = Offset(cx, cy)
            )

            // Glowing Cyan-to-Purple Bezel Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryCyan,
                        Color(0xFF64B5F6),
                        deepViolet,
                        Color(0xFFBA68C8),
                        primaryCyan
                    ),
                    center = Offset(cx, cy)
                ),
                radius = bodyR * 0.92f,
                center = Offset(cx, cy),
                style = Stroke(width = 7.dp.toPx())
            )

            // 5. Inner Luminous Stopwatch Dial Face
            val dialR = bodyR * 0.80f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFDFF7FA),
                        Color(0xFFB5E8F2),
                        Color(0xFF8CD6EC)
                    ),
                    center = Offset(cx - dialR * 0.2f, cy - dialR * 0.2f),
                    radius = dialR * 1.3f
                ),
                radius = dialR,
                center = Offset(cx, cy)
            )

            // 6. 12 Precision Dial Tick Marks
            for (i in 0 until 12) {
                val angleRad = (i * 30.0 - 90.0) * (PI / 180.0)
                val isMajor = i % 3 == 0
                val innerTickR = dialR * (if (isMajor) 0.74f else 0.80f)
                val outerTickR = dialR * 0.88f
                val startX = cx + (innerTickR * cos(angleRad)).toFloat()
                val startY = cy + (innerTickR * sin(angleRad)).toFloat()
                val endX = cx + (outerTickR * cos(angleRad)).toFloat()
                val endY = cy + (outerTickR * sin(angleRad)).toFloat()
                drawLine(
                    color = Color(0xFF1A3654).copy(alpha = if (isMajor) 0.75f else 0.45f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Subtle Sweeping Progress Arc inside the Dial
            val handAngleDeg = lerpFloat(-45f, 48f, revealProgress) + (ambientDrift * 8f)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryCyan.copy(alpha = 0.35f),
                        deepViolet.copy(alpha = 0.25f)
                    ),
                    center = Offset(cx, cy)
                ),
                startAngle = -90f,
                sweepAngle = handAngleDeg + 90f,
                useCenter = true,
                topLeft = Offset(cx - dialR * 0.72f, cy - dialR * 0.72f),
                size = Size(dialR * 1.44f, dialR * 1.44f)
            )

            // 7. Stopwatch Hand & Center Pivot Cap
            rotate(degrees = handAngleDeg, pivot = Offset(cx, cy)) {
                drawLine(
                    color = Color(0xFF162B44),
                    start = Offset(cx, cy + dialR * 0.14f),
                    end = Offset(cx, cy - dialR * 0.56f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                color = Color(0xFF162B44),
                radius = 5.5.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = primaryCyan,
                radius = 2.5.dp.toPx(),
                center = Offset(cx, cy)
            )

            // 8. Orbital Floating Liquid Glass & Cyan Spheres (matching 00:08 - 00:09)
            val driftOffset = (ambientDrift - 0.5f) * 6.dp.toPx()

            // Top-right frosted glass sphere
            val trCenter = Offset(cx + outerOrbitR * 0.66f, cy - outerOrbitR * 0.42f + driftOffset)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        primaryCyan.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = trCenter,
                    radius = 22.dp.toPx()
                ),
                radius = 22.dp.toPx(),
                center = trCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.32f),
                radius = 22.dp.toPx(),
                center = trCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // Bottom-right luminous cyan orb
            val brCenter = Offset(cx + outerOrbitR * 0.58f, cy + outerOrbitR * 0.50f - driftOffset)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB2FEFA),
                        primaryCyan,
                        primaryCyan.copy(alpha = 0.2f)
                    ),
                    center = Offset(brCenter.x - 3.dp.toPx(), brCenter.y - 3.dp.toPx()),
                    radius = 12.dp.toPx()
                ),
                radius = 11.dp.toPx(),
                center = brCenter
            )

            // Bottom-left soft teal orb
            val blCenter = Offset(cx - outerOrbitR * 0.56f, cy + outerOrbitR * 0.46f + driftOffset)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = 0.85f),
                        Color(0xFF26A69A).copy(alpha = 0.55f)
                    ),
                    center = blCenter,
                    radius = 8.dp.toPx()
                ),
                radius = 7.5.dp.toPx(),
                center = blCenter
            )
        }
    }
}

/**
 * 3D Smartphone Bezel Frame shown during the mid-animation reveal (00:05 - 00:08)
 * before expanding seamlessly into the user's real Android viewport.
 */
@Composable
private fun SmartphoneRevealFrame(
    alpha: Float,
    expandProgress: Float,
    primaryCyan: Color,
    deepViolet: Color
) {
    val frameScale = lerpFloat(0.92f, 1.08f, expandProgress)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = frameScale
                scaleY = frameScale
            }
            .border(
                width = 2.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.45f),
                        deepViolet.copy(alpha = 0.70f),
                        primaryCyan.copy(alpha = 0.50f)
                    )
                ),
                shape = RoundedCornerShape(42.dp)
            )
    ) {
        // Subtle top speaker / camera pill indicator on the 3D phone reveal frame
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .width(76.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
        )
    }
}

/**
 * Deep AMOLED Background with Flowing Cyan & Purple Aurora Waves and Starlight Particles.
 */
@Composable
private fun WelcomeAuroraBackdrop(
    timelineProgress: Float,
    ambientDrift: Float,
    primaryCyan: Color,
    deepViolet: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base deep AMOLED midnight gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF030710),
                    Color(0xFF060E1B),
                    Color(0xFF040914)
                )
            )
        )

        // Flowing Cyan/Teal Nebula Glow (shifts smoothly from bottom-left to mid-left)
        val cyanCenterX = w * lerpFloat(0.18f, 0.25f, timelineProgress) + (ambientDrift * 18.dp.toPx())
        val cyanCenterY = h * lerpFloat(0.68f, 0.56f, timelineProgress)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryCyan.copy(alpha = 0.26f),
                    Color(0xFF00838F).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(cyanCenterX, cyanCenterY),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(cyanCenterX, cyanCenterY)
        )

        // Flowing Deep Purple/Violet Nebula Glow (shifts smoothly from bottom-right to upper-left/mid-right)
        val purpleCenterX = w * lerpFloat(0.82f, 0.75f, timelineProgress) - (ambientDrift * 16.dp.toPx())
        val purpleCenterY = h * lerpFloat(0.75f, 0.48f, timelineProgress)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    deepViolet.copy(alpha = 0.28f),
                    Color(0xFF4A148C).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(purpleCenterX, purpleCenterY),
                radius = w * 0.82f
            ),
            radius = w * 0.82f,
            center = Offset(purpleCenterX, purpleCenterY)
        )

        // Floating Starlight / Bokeh Particles
        AmbientParticles.forEach { particle ->
            val driftY = ((timelineProgress * 28f * particle.speedFactor) + (ambientDrift * 10f * particle.speedFactor))
            val px = (particle.xFraction * w)
            val py = ((particle.yFraction * h) - driftY.dp.toPx()).let { rawY ->
                if (rawY < 0f) rawY + h else rawY
            }
            val color = if (particle.isCyanTinted) primaryCyan else Color.White
            drawCircle(
                color = color.copy(alpha = particle.baseAlpha),
                radius = particle.radiusDp.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun TermsDialog(onDismiss: () -> Unit, primaryCyan: Color) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101D2E),
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
                    "Welcome to FocusLock. By using this application, you agree to the following terms:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                )
                Text(
                    "1. Mindful Usage: FocusLock is a digital wellbeing tool designed to help you manage screen time and build healthy focus habits.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "2. User Control: You choose which apps to limit, when schedules apply, and which verification challenges to enable.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "3. System Permissions: FocusLock uses standard Android Usage Access, Accessibility, and Overlay APIs solely to enforce the focus limits you configure.",
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
                    contentColor = Color(0xFF04141F)
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
        containerColor = Color(0xFF101D2E),
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
                    "FocusLock is built with an offline-first, privacy-by-design architecture:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                )
                Text(
                    "• 100% On-Device Storage: Your app limits, focus sessions, and screen time statistics remain stored locally on your device.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "• Zero Personal Spying: We never read or transmit your personal messages, keystrokes, photos, or browsing history.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f))
                )
                Text(
                    "• Foreground Detection Only: Accessibility and Usage Stats are evaluated strictly on-device in real time to detect when a limited app is opened.",
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
                    contentColor = Color(0xFF04141F)
                )
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
