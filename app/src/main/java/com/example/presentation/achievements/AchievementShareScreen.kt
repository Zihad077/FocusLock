package com.example.presentation.achievements

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.FocusLockApplication
import com.example.database.UserSettings
import com.example.ui.theme.liquidGlass
import com.example.util.StreakAndAchievementManager
import com.example.util.UsageStatsHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementShareViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FocusLockApplication).repository

    init {
        viewModelScope.launch {
            StreakAndAchievementManager.evaluateAndSync(repository)
        }
    }

    val shareableData: StateFlow<ShareableAchievementData?> = combine(
        repository.userSettings,
        repository.allAchievements,
        repository.allFocusSessions,
        repository.allLimits,
        repository.allGoals
    ) { settings, achievements, sessions, limits, goals ->
        val milestoneInfo = StreakAndAchievementManager.buildStreakMilestoneInfo(
            settings = settings,
            sessions = sessions,
            goals = goals
        )
        val progressList = StreakAndAchievementManager.buildAchievementProgressList(
            achievements = achievements,
            settings = settings,
            sessions = sessions,
            limits = limits,
            goals = goals
        )
        val completedSessions = sessions.count { it.isCompleted }
        val activeLimitsCount = limits.count { it.isEnabled }
        val focusScore = if (settings.focusScore > 0) {
            settings.focusScore.coerceIn(0, 100)
        } else {
            (75 + completedSessions * 4 + activeLimitsCount * 3).coerceIn(0, 100)
        }

        ShareableAchievementData(
            settings = settings,
            milestoneInfo = milestoneInfo,
            achievements = progressList,
            focusScore = focusScore,
            activeLimitsCount = activeLimitsCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}

@Composable
fun AchievementShareScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementShareViewModel = viewModel()
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val shareData by viewModel.shareableData.collectAsStateWithLifecycle()
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccessBannerVisible by remember { mutableStateOf(false) }

    // Re-render high-resolution bitmap whenever real user achievement/streak data updates
    LaunchedEffect(shareData) {
        val currentData = shareData ?: return@LaunchedEffect
        isRendering = true
        val bmp = AchievementCardRenderer.renderShareableBitmap(currentData)
        generatedBitmap = bmp
        isRendering = false
    }

    val performSaveToGallery: () -> Unit = {
        val bmp = generatedBitmap
        if (bmp != null && !isSaving) {
            isSaving = true
            coroutineScope.launch {
                val uri = AchievementCardRenderer.saveBitmapToGallery(context, bmp)
                isSaving = false
                if (uri != null) {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    saveSuccessBannerVisible = true
                    Toast.makeText(
                        context,
                        "Saved to Gallery ✨",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Could not save image. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            performSaveToGallery()
        } else {
            Toast.makeText(
                context,
                "Storage permission is required on this Android version to save to gallery.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val cyanColor = Color(0xFF00E5FF)
    val goldColor = Color(0xFFFFCA28)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .clickable {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (_: Exception) {}
                            onNavigateBack()
                        }
                        .testTag("achievement_share_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Share Achievements",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Flex your real focus streak & unlocked badges ✨",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFFB0C0D4)
                    )
                }
            }

            // Live Unlocked Pill
            shareData?.let { data ->
                val unlockedCount = data.achievements.count { it.achievement.isUnlocked }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(cyanColor.copy(alpha = 0.14f))
                        .border(1.dp, cyanColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = goldColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$unlockedCount/${data.achievements.size}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Scrollable Image Preview Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val bmp = generatedBitmap
            if (bmp == null || isRendering) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidGlass(shape = RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = cyanColor,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "Crafting your high-res achievement card...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFFB0C0D4)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Saved confirmation pill
                    AnimatedVisibility(
                        visible = saveSuccessBannerVisible,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF062E22))
                                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "High-res card saved to your Gallery! ✨",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Rendered High-Res Achievement Card Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        cyanColor.copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.2f),
                                        goldColor.copy(alpha = 0.5f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .testTag("achievement_share_image_preview")
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Shareable FocusLock Achievements Card",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Bottom Action Bar: Download Image & Share Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .liquidGlass(shape = RoundedCornerShape(26.dp), isHighlight = true)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download Image Button
                val downloadInteraction = remember { MutableInteractionSource() }
                val downloadPressed by downloadInteraction.collectIsPressedAsState()
                val downloadScale by animateFloatAsState(
                    targetValue = if (downloadPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "download_btn_scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("download_achievement_image_button")
                        .graphicsLayer {
                            scaleX = downloadScale
                            scaleY = downloadScale
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF132238))
                        .border(1.2.dp, cyanColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = downloadInteraction,
                            indication = null,
                            enabled = generatedBitmap != null && !isSaving
                        ) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (_: Exception) {}
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    performSaveToGallery()
                                } else {
                                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            } else {
                                performSaveToGallery()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Image",
                            tint = cyanColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isSaving) "Saving..." else "Download Image",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Share Image Button
                val shareInteraction = remember { MutableInteractionSource() }
                val sharePressed by shareInteraction.collectIsPressedAsState()
                val shareScale by animateFloatAsState(
                    targetValue = if (sharePressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "share_btn_scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("share_achievement_image_button")
                        .graphicsLayer {
                            scaleX = shareScale
                            scaleY = shareScale
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF24DFEC))
                            )
                        )
                        .clickable(
                            interactionSource = shareInteraction,
                            indication = null,
                            enabled = generatedBitmap != null
                        ) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (_: Exception) {}
                            generatedBitmap?.let { bmp ->
                                coroutineScope.launch {
                                    AchievementCardRenderer.shareBitmapImage(context, bmp)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Image",
                            tint = Color(0xFF04151F),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Share Image",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF04151F)
                        )
                    }
                }
            }
        }
    }
}
