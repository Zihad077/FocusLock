package com.example.presentation.insights

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdsterraSocialBar
import com.example.ads.LiquidGlassAdaptiveBanner
import com.example.ads.LiquidGlassNativeAdCard
import com.example.database.isPremiumActive
import com.example.ui.theme.liquidGlass
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = viewModel(
        factory = InsightsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle(initialValue = null)
    val primaryCyan = Color(0xFF24DFEC)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Productivity Intelligence",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
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
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Productivity Score Glass Card
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
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(primaryCyan.copy(alpha = 0.18f))
                                .border(1.dp, primaryCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "DAILY PRODUCTIVITY SCORE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${state.todayScore}",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 48.sp
                            ),
                            color = primaryCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Synthesized from goals, session limits & focus purity",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Recommendations
            item {
                Text(
                    "Smart Recommendations",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }
            items(state.recommendations) { rec ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(shape = RoundedCornerShape(22.dp))
                        .padding(16.dp)
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
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = primaryCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            rec,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = Color.White
                        )
                    }
                }
            }

            // Native ad after main insight cards (Zero ads for premium)
            item {
                LiquidGlassNativeAdCard(
                    isPremium = userSettings?.isPremiumActive ?: false
                )
            }

            // Usage Timeline
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Today's Timeline",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }
            
            if (state.usageEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No foreground usage events logged today yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            } else {
                items(state.usageEvents) { event ->
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val startStr = timeFormat.format(Date(event.startTime))
                    val endStr = timeFormat.format(Date(event.endTime))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(54.dp)
                        ) {
                            Text(
                                startStr,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = primaryCyan
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(26.dp)
                                    .background(primaryCyan.copy(alpha = 0.4f))
                            )
                            Text(
                                endStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .liquidGlass(shape = RoundedCornerShape(20.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    event.packageName.split(".").last().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${event.durationMinutes} minutes foreground duration",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
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
