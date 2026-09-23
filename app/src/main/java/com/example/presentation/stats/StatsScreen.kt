@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.presentation.stats

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.database.isPremiumActive
import com.example.ui.theme.liquidGlass
import com.example.util.AppUsageInfo
import com.example.util.DailyStat
import com.example.util.ScreenTimeSummary
import com.example.util.UsageTimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    val isUsageAccessGranted by viewModel.isUsageAccessGranted.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val summary by viewModel.screenTimeSummary.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val totalFocusSessions by viewModel.totalFocusSessions.collectAsStateWithLifecycle()
    val totalFocusTime by viewModel.totalFocusTime.collectAsStateWithLifecycle()
    val totalEscapeAttempts by viewModel.totalEscapeAttempts.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var appToSetLimitFor by remember { mutableStateOf<AppUsageInfo?>(null) }

    // Auto-refresh when returning from Settings (e.g. after granting usage access)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndSync()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val primaryCyan = Color(0xFF00E5FF)
    val accentPurple = Color(0xFF9D4EDD)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Screen Time & Analytics",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Identify distracting apps & usage trends",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshUsageData() },
                        modifier = Modifier.testTag("refresh_stats_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Stats",
                            tint = primaryCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
            // 1. Permission Warning Banner if not granted
            if (!isUsageAccessGranted) {
                item {
                    UsagePermissionRequiredCard(
                        onGrantClick = {
                            try {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to main settings
                                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(fallbackIntent)
                            }
                        }
                    )
                }
            }

            // 2. Time Range Selector Filter
            item {
                TimeRangeSelectorRow(
                    selectedRange = selectedTimeRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )
            }

            // 3. Hero Total Screen Time Card
            item {
                HeroScreenTimeCard(
                    summary = summary,
                    selectedRange = selectedTimeRange,
                    primaryCyan = primaryCyan
                )
            }

            // 4. Focus & Protection Stat Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Focus Time Glass Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .liquidGlass(shape = RoundedCornerShape(20.dp), isElevated = false)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(primaryCyan.copy(alpha = 0.2f))
                                        .padding(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = primaryCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    "Focus Time",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${totalFocusTime / 60}h ${totalFocusTime % 60}m",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = primaryCyan
                            )
                        }
                    }

                    // Escape Attempts Deflected Card
                    val errorColor = Color(0xFFFF5252)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .liquidGlass(shape = RoundedCornerShape(20.dp), isHighlight = totalEscapeAttempts > 0)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(errorColor.copy(alpha = 0.2f))
                                        .padding(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = errorColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    "Blocked Stops",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "$totalEscapeAttempts",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = errorColor
                            )
                        }
                    }
                }
            }

            // 5. Most Distracting App Spotlight Card
            if (summary.topApp != null && summary.topApp!!.usedMinutes > 0) {
                item {
                    DistractingSpotlightCard(
                        topApp = summary.topApp!!,
                        onSetLimitClick = { appToSetLimitFor = summary.topApp }
                    )
                }
            }

            // 6. 7-Day Screen Time Trend Chart
            if (summary.dailyStats.isNotEmpty()) {
                item {
                    WeeklyTrendChartCard(
                        dailyStats = summary.dailyStats,
                        primaryCyan = primaryCyan
                    )
                }
            }

            // 7. Category Breakdown
            if (summary.categoryDistribution.isNotEmpty()) {
                item {
                    CategoryBreakdownCard(
                        distribution = summary.categoryDistribution,
                        totalMinutes = summary.totalScreenTimeMinutes
                    )
                }
            }

            // 8. App Usage List Header & Search Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Usage Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${summary.appUsageList.size} apps tracked",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryCyan
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search app usage...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = primaryCyan.copy(alpha = 0.8f)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color(0xFF131824).copy(alpha = 0.6f),
                            unfocusedContainerColor = Color(0xFF131824).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_usage_search_field")
                    )
                }
            }

            // 9. Ranked App Usage Items
            val filteredApps = summary.appUsageList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }

            if (filteredApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUsageAccessGranted) "No app activity recorded for this period." else "Grant Usage Access above to see your app usage breakdown.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    AppUsageItemRow(
                        appInfo = appInfo,
                        onSetLimitClick = { appToSetLimitFor = appInfo }
                    )
                }
            }

            // 10. Native Ad Card
            item {
                LiquidGlassNativeAdCard(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // 11. Bottom Banner
            item {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }
        }
    }

    // Quick Limit Dialog
    if (appToSetLimitFor != null) {
        SetAppLimitDialog(
            appInfo = appToSetLimitFor!!,
            onDismiss = { appToSetLimitFor = null },
            onSaveLimit = { minutes ->
                viewModel.setDailyLimit(
                    packageName = appToSetLimitFor!!.packageName,
                    appName = appToSetLimitFor!!.appName,
                    dailyMinutes = minutes
                )
                appToSetLimitFor = null
            },
            onRemoveLimit = {
                viewModel.removeDailyLimit(appToSetLimitFor!!.packageName)
                appToSetLimitFor = null
            }
        )
    }
}

@Composable
fun UsagePermissionRequiredCard(onGrantClick: () -> Unit) {
    val primaryCyan = Color(0xFF00E5FF)
    val warningOrange = Color(0xFFFF9100)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isHighlight = true,
                borderWidth = 1.5.dp
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(warningOrange.copy(alpha = 0.2f))
                        .border(1.dp, warningOrange.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = warningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usage Access Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Enable access to import historical screen time and see distracting apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grant_usage_permission_button")
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Usage Access Permission", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TimeRangeSelectorRow(
    selectedRange: UsageTimeRange,
    onRangeSelected: (UsageTimeRange) -> Unit
) {
    val primaryCyan = Color(0xFF00E5FF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(16.dp), isElevated = false)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        UsageTimeRange.values().forEach { range ->
            val isSelected = range == selectedRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) primaryCyan.copy(alpha = 0.25f) else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) primaryCyan.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onRangeSelected(range) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) primaryCyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun HeroScreenTimeCard(
    summary: ScreenTimeSummary,
    selectedRange: UsageTimeRange,
    primaryCyan: Color
) {
    val hours = summary.totalScreenTimeMinutes / 60
    val minutes = summary.totalScreenTimeMinutes % 60

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(26.dp), isHighlight = true)
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedRange) {
                        UsageTimeRange.TODAY -> "Total Screen Time Today"
                        UsageTimeRange.YESTERDAY -> "Screen Time Yesterday"
                        UsageTimeRange.LAST_7_DAYS -> "Screen Time (Last 7 Days)"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryCyan.copy(alpha = 0.15f))
                        .border(1.dp, primaryCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${summary.totalAppCount} Apps",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = primaryCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${hours}h ${minutes}m",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (summary.comparisonText.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = primaryCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = summary.comparisonText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun DistractingSpotlightCard(
    topApp: AppUsageInfo,
    onSetLimitClick: () -> Unit
) {
    val fieryRed = Color(0xFFFF5252)
    val fieryOrange = Color(0xFFFF7A00)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                borderWidth = 1.5.dp,
                isHighlight = true
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(fieryRed, fieryOrange)))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#1 Distracting App",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = fieryOrange
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(fieryRed.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${(topApp.percentageOfTotal * 100).toInt()}% of time",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = fieryRed
                            )
                        }
                    }

                    Text(
                        text = topApp.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${topApp.usedMinutes / 60}h ${topApp.usedMinutes % 60}m spent • ${topApp.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Button(
                onClick = onSetLimitClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (topApp.isLimitActive) Color(0xFF00E5FF).copy(alpha = 0.2f) else fieryOrange,
                    contentColor = if (topApp.isLimitActive) Color(0xFF00E5FF) else Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (topApp.isLimitActive) Icons.Default.CheckCircle else Icons.Default.LockClock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (topApp.isLimitActive) "Limit Active (${topApp.dailyLimitMinutes}m) • Tap to Edit" else "Set Daily Limit on ${topApp.appName}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendChartCard(
    dailyStats: List<DailyStat>,
    primaryCyan: Color
) {
    val maxMinutes = (dailyStats.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(30)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Screen Time Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Past 7 Days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyStats.forEachIndexed { index, stat ->
                    val isLatest = index == dailyStats.lastIndex
                    val fraction = (stat.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.05f, 1f)
                    val animatedHeight by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(600),
                        label = "chart_bar"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Value label above bar
                        if (stat.minutes > 0) {
                            Text(
                                text = if (stat.minutes >= 60) "${stat.minutes / 60}h" else "${stat.minutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isLatest) primaryCyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        } else {
                            Text("0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Transparent)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bar
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight(0.75f * animatedHeight)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (isLatest) {
                                        Brush.verticalGradient(
                                            listOf(primaryCyan, Color(0xFF0077B6))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.1f))
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isLatest) primaryCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stat.day,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isLatest) primaryCyan else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    distribution: Map<String, Int>,
    totalMinutes: Int
) {
    if (totalMinutes <= 0) return

    val categoryColors = mapOf(
        "Social" to Color(0xFF00E5FF),
        "Entertainment" to Color(0xFF9D4EDD),
        "Gaming" to Color(0xFFFF7A00),
        "Browsing" to Color(0xFF00B4D8),
        "Productivity" to Color(0xFF00F5D4),
        "Communication" to Color(0xFFFF007F),
        "General" to Color(0xFF90E0EF)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Category Distribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                distribution.entries.sortedByDescending { it.value }.forEach { (cat, mins) ->
                    val weight = (mins.toFloat() / totalMinutes.toFloat()).coerceAtLeast(0.01f)
                    val color = categoryColors[cat] ?: Color(0xFF90E0EF)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }

            // Legend pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                distribution.entries.sortedByDescending { it.value }.take(5).forEach { (cat, mins) ->
                    val color = categoryColors[cat] ?: Color(0xFF90E0EF)
                    val percent = (mins.toFloat() / totalMinutes.toFloat() * 100).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = "$cat: ${mins / 60}h ${mins % 60}m ($percent%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageItemRow(
    appInfo: AppUsageInfo,
    onSetLimitClick: () -> Unit
) {
    val context = LocalContext.current
    val primaryCyan = Color(0xFF00E5FF)

    val appIcon: Drawable? = remember(appInfo.packageName) {
        try {
            context.packageManager.getApplicationIcon(appInfo.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                isHighlight = appInfo.isDistracting,
                isElevated = false
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap(96, 96).asImageBitmap(),
                        contentDescription = appInfo.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            tint = primaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = appInfo.appName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (appInfo.isDistracting) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF5252).copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "Distracting",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }
                    }

                    Text(
                        text = appInfo.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }

                // Time spent
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (appInfo.usedMinutes >= 60) {
                            "${appInfo.usedMinutes / 60}h ${appInfo.usedMinutes % 60}m"
                        } else {
                            "${appInfo.usedMinutes}m"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (appInfo.isDistracting) Color(0xFFFF7A00) else primaryCyan
                    )

                    Text(
                        text = "${(appInfo.percentageOfTotal * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            // Proportion bar
            LinearProgressIndicator(
                progress = { appInfo.percentageOfTotal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (appInfo.isDistracting) Color(0xFFFF7A00) else primaryCyan,
                trackColor = Color.White.copy(alpha = 0.08f),
            )

            // Limit Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appInfo.isLimitActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00F5D4),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Daily Limit: ${appInfo.dailyLimitMinutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF00F5D4)
                        )
                    }
                } else {
                    Text(
                        "No limit set",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }

                TextButton(
                    onClick = onSetLimitClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        if (appInfo.isLimitActive) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = primaryCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (appInfo.isLimitActive) "Edit Limit" else "Set Limit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = primaryCyan
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetAppLimitDialog(
    appInfo: AppUsageInfo,
    onDismiss: () -> Unit,
    onSaveLimit: (Int) -> Unit,
    onRemoveLimit: () -> Unit
) {
    var selectedMinutes by remember {
        mutableIntStateOf(if (appInfo.dailyLimitMinutes > 0) appInfo.dailyLimitMinutes else 30)
    }

    val primaryCyan = Color(0xFF00E5FF)
    val presets = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.liquidGlass(shape = RoundedCornerShape(28.dp), isElevated = true),
        containerColor = Color.Transparent,
        title = {
            Column {
                Text(
                    text = "Daily Limit: ${appInfo.appName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "App will be blocked once daily usage reaches this limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Large minute readout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(primaryCyan.copy(alpha = 0.15f))
                        .border(1.dp, primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedMinutes >= 60) "${selectedMinutes / 60}h ${selectedMinutes % 60}m per day" else "$selectedMinutes minutes per day",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = primaryCyan
                    )
                }

                // Slider
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = (it / 5).toInt() * 5 },
                    valueRange = 5f..240f,
                    steps = 46,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryCyan,
                        activeTrackColor = primaryCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Quick preset pills
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { mins ->
                        val isSelected = selectedMinutes == mins
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMinutes = mins },
                            label = { Text(if (mins >= 60) "${mins / 60}h" else "${mins}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveLimit(selectedMinutes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryCyan,
                    contentColor = Color.Black
                )
            ) {
                Text("Save Limit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (appInfo.isLimitActive) {
                    TextButton(
                        onClick = onRemoveLimit,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text("Remove Limit")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
