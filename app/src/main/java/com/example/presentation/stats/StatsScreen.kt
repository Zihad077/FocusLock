package com.example.presentation.stats
import com.example.database.isPremiumActive

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val weeklyUsage by viewModel.weeklyUsage.collectAsStateWithLifecycle()
    val mostDistracting by viewModel.mostDistracting.collectAsStateWithLifecycle()
    val totalFocusSessions by viewModel.totalFocusSessions.collectAsStateWithLifecycle()
    val totalFocusTime by viewModel.totalFocusTime.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Analytics & Insights",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
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
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                WeeklyChartGlassCard(weeklyUsage)
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Focus Sessions Stat Glass
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .liquidGlass(shape = RoundedCornerShape(22.dp), isElevated = false)
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(primaryCyan.copy(alpha = 0.2f))
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = primaryCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    "Focus Sessions",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$totalFocusSessions",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = primaryCyan
                            )
                        }
                    }

                    // Focus Time Stat Glass
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .liquidGlass(shape = RoundedCornerShape(22.dp), isElevated = false)
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF00BFA5).copy(alpha = 0.2f))
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = Color(0xFF00BFA5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    "Focus Time",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${totalFocusTime/60}h ${totalFocusTime%60}m",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
            
            item {
                val totalEscapeAttempts by viewModel.totalEscapeAttempts.collectAsStateWithLifecycle()
                val errorColor = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(22.dp), isHighlight = totalEscapeAttempts > 0)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(errorColor.copy(alpha = 0.2f))
                                .border(1.dp, errorColor.copy(alpha = 0.4f), CircleShape)
                                .padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = errorColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Escape Attempts Deflected",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "$totalEscapeAttempts",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = errorColor
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Most Distracting App",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(22.dp))
                        .padding(22.dp)
                ) {
                    if (mostDistracting == null) {
                        Text(
                            text = "Insufficient telemetry to determine top distractor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF5252), Color(0xFFFF7A00))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = mostDistracting!!.first,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${mostDistracting!!.second / 60}h ${mostDistracting!!.second % 60}m total screen time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }

            // Sponsored Native Glass Card (Zero ads for premium)
            item {
                LiquidGlassNativeAdCard(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // Bottom Adaptive Banner (Zero ads for premium)
            item {
                LiquidGlassAdaptiveBanner(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }
        }
    }
}

@Composable
fun WeeklyChartGlassCard(data: List<DailyStat>) {
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(28.dp),
                isElevated = true,
                isHighlight = true
            )
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THIS WEEK'S TELEMETRY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
                val totalMinutes = data.sumOf { it.minutes }
                Text(
                    text = "${totalMinutes / 60}h ${totalMinutes % 60}m",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryCyan
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            val maxMins = data.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { stat ->
                    BarChartGlassItem(
                        day = stat.dayName,
                        value = stat.minutes,
                        maxValue = maxMins
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartGlassItem(day: String, value: Int, maxValue: Int) {
    var isVisible by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val animatedHeight by animateFloatAsState(
        targetValue = if (isVisible) (value.toFloat() / maxValue) else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "BarHeight"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        // Value Text
        Text(
            text = "${value/60}h",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        // Liquid Pillar Bar
        Box(
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight(animatedHeight.coerceAtLeast(0.04f))
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryCyan,
                            primaryCyan.copy(alpha = 0.45f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                )
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Day Label
        Text(
            text = day.take(3).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
    }
}
