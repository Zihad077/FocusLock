@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.presentation.stats

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ads.AdsterraSocialBar
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.database.Achievement
import com.example.database.Goal
import com.example.database.isPremiumActive
import com.example.presentation.goals.GoalsViewModel
import com.example.presentation.insights.InsightsViewModel
import com.example.ui.theme.liquidGlass
import com.example.util.AppUsageInfo
import com.example.util.DailyStat
import com.example.util.ScreenTimeSummary
import com.example.util.UsageTimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(LocalContext.current.applicationContext as Application)
    ),
    insightsViewModel: InsightsViewModel = viewModel(
        factory = InsightsViewModel.Factory(LocalContext.current.applicationContext as Application)
    ),
    goalsViewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_analytics),
        stringResource(R.string.tab_insights),
        stringResource(R.string.tab_goals)
    )

    val selectedTimeRange by statsViewModel.selectedTimeRange.collectAsStateWithLifecycle()
    val isUsageAccessGranted by statsViewModel.isUsageAccessGranted.collectAsStateWithLifecycle()
    val summary by statsViewModel.screenTimeSummary.collectAsStateWithLifecycle()
    val totalFocusSessions by statsViewModel.totalFocusSessions.collectAsStateWithLifecycle()
    val totalFocusTime by statsViewModel.totalFocusTime.collectAsStateWithLifecycle()
    val totalEscapeAttempts by statsViewModel.totalEscapeAttempts.collectAsStateWithLifecycle()

    val insightsState by insightsViewModel.state.collectAsStateWithLifecycle()
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()
    val achievements by goalsViewModel.achievements.collectAsStateWithLifecycle()
    val userSettings by statsViewModel.userSettings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var appToSetLimitFor by remember { mutableStateOf<AppUsageInfo?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    // Auto-sync when resuming
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                statsViewModel.checkPermissionAndSync()
                insightsViewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val primaryCyan = Color(0xFF24DFEC)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.screen_time_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                    IconButton(
                        onClick = {
                            statsViewModel.refreshUsageData()
                            insightsViewModel.refresh()
                        },
                        modifier = Modifier
                            .testTag("refresh_stats_button")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = primaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unified 3-Segment Tab Bar (Analytics, Insights, Goals)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x283E4C5E))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) primaryCyan.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                ),
                                color = if (isSelected) primaryCyan else Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top 320x50 Banner Ad (Visible immediately from the start)
            item {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: ANALYTICS & SCREEN TIME
                    if (!isUsageAccessGranted) {
                        item {
                            UsagePermissionRequiredCard(
                                onGrantClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                                        context.startActivity(fallbackIntent)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        TimeRangeSelectorRow(
                            selectedRange = selectedTimeRange,
                            onRangeSelected = { statsViewModel.setTimeRange(it) }
                        )
                    }

                    item {
                        HeroScreenTimeCard(
                            summary = summary,
                            selectedRange = selectedTimeRange,
                            primaryCyan = primaryCyan
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Focus Time
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(primaryCyan.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = primaryCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            stringResource(R.string.total_focus),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "${totalFocusTime / 60}h ${totalFocusTime % 60}m",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp
                                        ),
                                        color = primaryCyan
                                    )
                                }
                            }

                            // Deflected Stops
                            val errorColor = Color(0xFFFF5252)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(errorColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = errorColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            stringResource(R.string.blocked_apps),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "$totalEscapeAttempts",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp
                                        ),
                                        color = errorColor
                                    )
                                }
                            }
                        }
                    }

                    if (summary.dailyStats.isNotEmpty()) {
                        item {
                            WeeklyTrendChartCard(
                                dailyStats = summary.dailyStats,
                                primaryCyan = primaryCyan
                            )
                        }
                    }

                    // App Usage List Header & Search Bar
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.all_monitored_apps),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Color.White
                            )
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.search_apps),
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = primaryCyan
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                                    focusedContainerColor = Color(0x283E4C5E),
                                    unfocusedContainerColor = Color(0x283E4C5E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val filteredApps = if (searchQuery.isBlank()) {
                        summary.appUsageList
                    } else {
                        summary.appUsageList.filter {
                            it.appName.contains(searchQuery, ignoreCase = true) ||
                                    it.packageName.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredApps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.insufficient_data),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        items(filteredApps, key = { it.packageName }) { appInfo ->
                            AppUsageRowItem(
                                appInfo = appInfo,
                                maxUsageMinutes = summary.appUsageList.maxOfOrNull { it.usedMinutes } ?: 1,
                                primaryCyan = primaryCyan,
                                onSetLimitClick = { appToSetLimitFor = appInfo }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: INSIGHTS & SMART RECOMMENDATIONS
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(shape = RoundedCornerShape(26.dp))
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x283E4C5E))
                                        .border(2.dp, primaryCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${insightsState.todayScore}",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 32.sp
                                            ),
                                            color = primaryCyan
                                        )
                                        Text(
                                            stringResource(R.string.focus_score),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (insightsState.todayScore >= 80) "Optimal Focus Habits" else "Distraction Warning",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.smart_recommendations),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                    }

                    if (insightsState.recommendations.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_insights_desc),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = Color.White.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(insightsState.recommendations) { rec ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(shape = RoundedCornerShape(22.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(primaryCyan.copy(alpha = 0.2f))
                                            .border(1.dp, primaryCyan.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = primaryCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: GOALS & ACHIEVEMENTS
                    userSettings?.let { settings ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(shape = RoundedCornerShape(26.dp))
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            stringResource(R.string.current_streak),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "${settings.currentStreak} ${stringResource(R.string.days_suffix)}",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 28.sp
                                            ),
                                            color = Color(0xFFFF9100)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FF9100))
                                            .border(1.dp, Color(0xFFFF9100).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9100),
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.active_commitments),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Color.White
                            )
                            FilledTonalButton(
                                onClick = { showAddGoalDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = primaryCyan.copy(alpha = 0.2f),
                                    contentColor = primaryCyan
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.add_goal), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (goals.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(shape = RoundedCornerShape(20.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.no_goals_set),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.no_goals_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(goals, key = { it.id }) { goal ->
                            GoalGlassCard(
                                goal = goal,
                                onDelete = { goalsViewModel.deleteGoal(goal.id) }
                            )
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.achievements_badges),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(achievements, key = { it.id }) { ach ->
                        AchievementGlassCard(achievement = ach)
                    }
                }
            }

            // Native Ad Card
            item {
                LiquidGlassNativeAdCard(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // Bottom 320x50 Banner & Social Bar
            item {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            item {
                AdsterraSocialBar(
                    isPremium = userSettings?.isPremiumActive ?: false,
                    isFocusActive = userSettings?.isFocusModeActive ?: false
                )
            }
        }
    }

    // Daily Limit Adjustment Dialog
    appToSetLimitFor?.let { appInfo ->
        DailyLimitEditDialog(
            appInfo = appInfo,
            onDismiss = { appToSetLimitFor = null },
            onSave = { minutes ->
                if (minutes <= 0) {
                    statsViewModel.removeDailyLimit(appInfo.packageName)
                } else {
                    statsViewModel.setDailyLimit(appInfo.packageName, appInfo.appName, minutes)
                }
                appToSetLimitFor = null
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var goalTarget by remember { mutableStateOf("60") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            containerColor = Color(0xFF162534),
            title = {
                Text(
                    stringResource(R.string.add_goal),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Title (e.g., Daily Deep Focus)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = goalTarget,
                        onValueChange = { goalTarget = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Minutes") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = goalTarget.toIntOrNull() ?: 60
                        if (goalTitle.isNotBlank()) {
                            goalsViewModel.addGoal(
                                title = goalTitle.trim(),
                                target = target,
                                current = 0,
                                type = "FOCUS_MINUTES"
                            )
                            showAddGoalDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryCyan,
                        contentColor = Color(0xFF0C1929)
                    )
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun GoalGlassCard(
    goal: Goal,
    onDelete: () -> Unit
) {
    val progress = (goal.currentValue.toFloat() / goal.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF24DFEC),
                trackColor = Color.White.copy(alpha = 0.12f)
            )

            Text(
                text = "${goal.currentValue} / ${goal.targetValue} min (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AchievementGlassCard(achievement: Achievement) {
    val isUnlocked = achievement.isUnlocked
    val primaryCyan = Color(0xFF24DFEC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) primaryCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        if (isUnlocked) primaryCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) primaryCyan else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun UsagePermissionRequiredCard(onGrantClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFB300)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.permission_required),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFFFFB300)
                )
            }

            Text(
                text = stringResource(R.string.grant_permission_desc),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = Color.White.copy(alpha = 0.8f)
            )

            Button(
                onClick = onGrantClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF24DFEC),
                    contentColor = Color(0xFF0C1929)
                )
            ) {
                Text(
                    stringResource(R.string.grant_permission_btn),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun TimeRangeSelectorRow(
    selectedRange: UsageTimeRange,
    onRangeSelected: (UsageTimeRange) -> Unit
) {
    val primaryCyan = Color(0xFF24DFEC)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x283E4C5E))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        UsageTimeRange.entries.forEach { range ->
            val isSelected = selectedRange == range
            val label = when (range) {
                UsageTimeRange.TODAY -> stringResource(R.string.today_label)
                UsageTimeRange.YESTERDAY -> stringResource(R.string.yesterday_label)
                UsageTimeRange.LAST_7_DAYS -> stringResource(R.string.this_week_label)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) primaryCyan.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onRangeSelected(range) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) primaryCyan else Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun HeroScreenTimeCard(
    summary: ScreenTimeSummary,
    selectedRange: UsageTimeRange,
    primaryCyan: Color
) {
    val totalMinutes = summary.totalScreenTimeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(26.dp))
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (selectedRange) {
                    UsageTimeRange.TODAY -> stringResource(R.string.todays_usage)
                    UsageTimeRange.YESTERDAY -> "YESTERDAY'S USAGE"
                    UsageTimeRange.LAST_7_DAYS -> "PAST 7 DAYS USAGE"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${hours}h ${minutes}m",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp
                ),
                color = primaryCyan
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${summary.totalAppCount} active apps monitored",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WeeklyTrendChartCard(
    dailyStats: List<DailyStat>,
    primaryCyan: Color
) {
    val maxMinutes = (dailyStats.maxOfOrNull { it.minutes } ?: 60).coerceAtLeast(60)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Daily Screen Time Trend",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyStats.forEach { stat ->
                    val barHeightFraction = (stat.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.08f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${stat.minutes / 60}h",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(barHeightFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(primaryCyan, primaryCyan.copy(alpha = 0.35f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stat.day,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageRowItem(
    appInfo: AppUsageInfo,
    maxUsageMinutes: Int,
    primaryCyan: Color,
    onSetLimitClick: () -> Unit
) {
    val progress = (appInfo.usedMinutes.toFloat() / maxUsageMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val context = LocalContext.current
    val appIcon: Drawable? = remember(appInfo.packageName) {
        try {
            context.packageManager.getApplicationIcon(appInfo.packageName)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.toBitmap().asImageBitmap(),
                            contentDescription = appInfo.appName,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryCyan.copy(alpha = 0.2f))
                                .border(1.dp, primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appInfo.appName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = primaryCyan
                            )
                        }
                    }

                    Column {
                        Text(
                            text = appInfo.appName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${appInfo.usedMinutes / 60}h ${appInfo.usedMinutes % 60}m (${appInfo.launchCount} opens)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onSetLimitClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = primaryCyan.copy(alpha = 0.18f),
                        contentColor = primaryCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (appInfo.dailyLimitMinutes > 0) "${appInfo.dailyLimitMinutes}m limit" else stringResource(R.string.set_limit),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (appInfo.dailyLimitMinutes > 0 && appInfo.usedMinutes >= appInfo.dailyLimitMinutes) Color(0xFFFF5252) else primaryCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun DailyLimitEditDialog(
    appInfo: AppUsageInfo,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val primaryCyan = Color(0xFF24DFEC)
    var limitInput by remember { mutableStateOf(if (appInfo.dailyLimitMinutes > 0) appInfo.dailyLimitMinutes.toString() else "30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF162534),
        title = {
            Text(
                "Set Daily Limit: ${appInfo.appName}",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Set how many minutes you are allowed to use this app per day before FocusLock restricts it.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = Color.White.copy(alpha = 0.75f)
                )
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Daily Minutes (0 to remove)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = limitInput.toIntOrNull() ?: 0
                    onSave(minutes)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryCyan,
                    contentColor = Color(0xFF0C1929)
                )
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
