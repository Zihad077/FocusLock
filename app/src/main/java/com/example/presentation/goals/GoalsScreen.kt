package com.example.presentation.goals
import com.example.database.isPremiumActive

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdsterraSocialBar
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.database.Achievement
import com.example.database.Goal
import com.example.database.UserSettings
import com.example.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Focus Goals & Milestones",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            userSettings?.let { settings ->
                item {
                    StreakGlassSection(settings = settings)
                }
            }

            item {
                Text(
                    text = "Active Commitments",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color.White
                )
            }

            if (goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active focus targets set. Consistency fuels progress.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            } else {
                items(goals) { goal ->
                    GoalGlassCard(
                        goal = goal,
                        onDelete = { viewModel.deleteGoal(goal.id) }
                    )
                }
            }

            // Sponsored Glass Card (Zero ads for premium)
            item {
                LiquidGlassNativeAdCard(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            item {
                Text(
                    text = "Achievements & Badges",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color.White
                )
            }

            if (achievements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Complete your daily focus routines to earn trophies.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            } else {
                items(achievements) { achievement ->
                    AchievementGlassCard(achievement = achievement)
                }
            }

            // 320x50 Banner near the bottom (Zero ads for premium)
            item {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // Social Bar format (cooldown protected, zero ads for premium)
            item {
                AdsterraSocialBar(
                    isPremium = userSettings?.isPremiumActive ?: false,
                    isFocusActive = userSettings?.isFocusModeActive ?: false
                )
            }
        }
    }
}

@Composable
fun StreakGlassSection(settings: UserSettings) {
    val fireColor = Color(0xFFFF9100)
    val cyanColor = Color(0xFF24DFEC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(fireColor.copy(alpha = 0.2f))
                        .border(1.dp, fireColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = fireColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${settings.currentStreak}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = fireColor
                )
                Text(
                    "CURRENT STREAK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(70.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(cyanColor.copy(alpha = 0.2f))
                        .border(1.dp, cyanColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = cyanColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${settings.bestStreak}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = cyanColor
                )
                Text(
                    "BEST STREAK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
fun GoalGlassCard(
    goal: Goal,
    onDelete: (() -> Unit)? = null
) {
    val primaryCyan = Color(0xFF24DFEC)

    val progress = if (goal.targetValue > 0) {
        (goal.currentValue.toFloat() / goal.targetValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "goal_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x283E4C5E))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = primaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "${goal.currentValue} / ${goal.targetValue}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = primaryCyan
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .testTag("delete_goal_${goal.id}")
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Goal",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(primaryCyan)
                )
            }
        }
    }
}

@Composable
fun AchievementGlassCard(achievement: Achievement) {
    val isUnlocked = achievement.isUnlocked
    val cyanColor = Color(0xFF24DFEC)
    val inactiveColor = Color.White.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                isHighlight = isUnlocked
            )
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
                    .background(
                        if (isUnlocked) cyanColor.copy(alpha = 0.2f)
                        else Color(0x283E4C5E)
                    )
                    .border(
                        1.dp,
                        if (isUnlocked) cyanColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) cyanColor else inactiveColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.65f)
                )
                if (isUnlocked) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "UNLOCKED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = cyanColor
                    )
                }
            }
        }
    }
}
