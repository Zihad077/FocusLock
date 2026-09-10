package com.example.navigation

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.apps.AppsScreen
import com.example.presentation.escape.EscapeScreen
import com.example.presentation.focus.FocusScreen
import com.example.presentation.goals.GoalsScreen
import com.example.presentation.home.HomeScreen
import com.example.presentation.insights.InsightsScreen
import com.example.presentation.onboarding.PermissionsScreen
import com.example.presentation.onboarding.WelcomeScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.stats.StatsScreen
import com.example.ui.theme.LiquidBackground
import com.example.util.PermissionHelper

private const val PREFS_NAME = "focuslock_onboarding_prefs"
private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"

@Composable
fun FocusLockApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    
    var isOnboardingCompleted by remember {
        mutableStateOf(sharedPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    }
    
    var allRequiredPermissionsGranted by remember {
        mutableStateOf(PermissionHelper.areAllRequiredPermissionsGranted(context))
    }

    // Determine initial route:
    // 1. If first launch (onboarding not completed) -> Welcome
    // 2. If onboarding completed but required permissions missing -> Permissions
    // 3. If onboarding completed and all required permissions granted -> MainTab
    val initialDestination: Route = remember {
        when {
            !isOnboardingCompleted -> Route.Welcome
            !allRequiredPermissionsGranted -> Route.Permissions
            else -> Route.MainTab
        }
    }

    val navController = rememberNavController()

    // Re-check permissions whenever app returns to the foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentGranted = PermissionHelper.areAllRequiredPermissionsGranted(context)
                allRequiredPermissionsGranted = currentGranted
                
                // If user has finished onboarding previously, but a required permission is revoked:
                val isCompleted = sharedPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
                if (isCompleted && !currentGranted) {
                    // Check current route; if not already on Permissions, navigate to Permissions
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    val isAlreadyOnPermissions = currentRoute?.contains("Permissions") == true
                    if (!isAlreadyOnPermissions) {
                        navController.navigate(Route.Permissions) {
                            popUpTo(0) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(navController = navController, startDestination = initialDestination) {
        composable<Route.Welcome> {
            WelcomeScreen(
                onNavigateToPermissions = { 
                    navController.navigate(Route.Permissions) {
                        popUpTo(Route.Welcome) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.Permissions> {
            PermissionsScreen(
                isFromSettings = false,
                onPermissionsGranted = {
                    // Mark onboarding complete in persistent storage
                    sharedPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                    isOnboardingCompleted = true
                    allRequiredPermissionsGranted = true
                    
                    navController.navigate(Route.MainTab) {
                        popUpTo(Route.Permissions) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.MainTab> {
            MainTabScreen()
        }
    }
}

@Composable
fun MainTabScreen() {
    val navController = rememberNavController()
    val isDark = isSystemInDarkTheme()

    // 5 primary tabs + Goals access via top/insights/more as shown in Liquid Glass UI mockup
    val items = listOf(
        BottomNavItem("Home", Route.Home, Icons.Default.Home),
        BottomNavItem("Apps", Route.Apps, Icons.Default.Lock),
        BottomNavItem("Focus", Route.Focus, Icons.Default.Timer),
        BottomNavItem("Goals", Route.Goals, Icons.Default.EmojiEvents),
        BottomNavItem("Stats", Route.Stats, Icons.Default.BarChart),
        BottomNavItem("Settings", Route.Settings, Icons.Default.Settings)
    )

    LiquidBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Floating Liquid Glass Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(32.dp),
                                ambientColor = Color(0x60001025),
                                spotColor = Color(0x3500E5FF)
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (isDark) {
                                        listOf(
                                            Color(0x7516243A),
                                            Color(0x600C1625)
                                        )
                                    } else {
                                        listOf(
                                            Color(0xD9FFFFFF),
                                            Color(0xBFEDF5FA)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = if (isDark) {
                                        listOf(
                                            Color(0x6080D8FF),
                                            Color(0x25FFFFFF),
                                            Color(0x1080D8FF),
                                            Color(0x4000E5FF)
                                        )
                                    } else {
                                        listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0x7080D8FF),
                                            Color(0x40FFFFFF),
                                            Color(0x80FFFFFF)
                                        )
                                    },
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEach { item ->
                                val isSelected = currentDestination?.hierarchy?.any { 
                                    it.hasRoute(item.route::class) 
                                } == true

                                val iconTint by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)
                                    } else {
                                        if (isDark) Color(0xFF88A0BA) else Color(0xFF6A8199)
                                    },
                                    animationSpec = spring(),
                                    label = "nav_icon_tint"
                                )

                                val pillBg by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        if (isDark) Color(0x3000E5FF) else Color(0x280077D6)
                                    } else {
                                        Color.Transparent
                                    },
                                    animationSpec = spring(),
                                    label = "nav_pill_bg"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .background(pillBg)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.name,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = iconTint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<Route.Home> { HomeScreen() }
                composable<Route.Apps> { AppsScreen() }
                composable<Route.Focus> { FocusScreen() }
                composable<Route.Goals> { GoalsScreen() }
                composable<Route.Insights> { InsightsScreen() }
                composable<Route.Stats> { StatsScreen() }
                composable<Route.Settings> { 
                    SettingsScreen(
                        onNavigateToApps = {
                            navController.navigate(Route.Apps) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToFocus = {
                            navController.navigate(Route.Focus) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToEscapePrevention = {
                            navController.navigate(Route.EscapePrevention)
                        },
                        onNavigateToPermissions = {
                            navController.navigate(Route.Permissions)
                        }
                    ) 
                }
                composable<Route.EscapePrevention> {
                    EscapeScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Route.Permissions> {
                    PermissionsScreen(
                        isFromSettings = true,
                        onNavigateBack = { navController.popBackStack() },
                        onPermissionsGranted = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

data class BottomNavItem(val name: String, val route: Route, val icon: ImageVector)
