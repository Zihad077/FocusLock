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
import androidx.compose.ui.platform.testTag
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
    // First launch -> Welcome. Completed -> MainTab.
    val initialDestination: Route = remember {
        if (!isOnboardingCompleted) Route.Welcome else Route.MainTab
    }

    val navController = rememberNavController()

    // Keep permissions state synced on resume without forcing navigation away from current screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allRequiredPermissionsGranted = PermissionHelper.areAllRequiredPermissionsGranted(context)
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
                    // Mark onboarding complete in persistent storage synchronously
                    sharedPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).commit()
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
                                elevation = 20.dp,
                                shape = RoundedCornerShape(32.dp),
                                ambientColor = Color(0x70001025),
                                spotColor = Color(0x3500E5FF)
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xE8142338),
                                        Color(0xF00A1320)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.50f),
                                        Color(0xFF00E5FF).copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.08f),
                                        Color(0xFF0077D6).copy(alpha = 0.35f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 6.dp)
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
                                    targetValue = if (isSelected) Color(0xFF00E5FF) else Color(0xFF88A0BA),
                                    animationSpec = spring(),
                                    label = "nav_icon_tint"
                                )

                                val pillBg by animateColorAsState(
                                    targetValue = if (isSelected) Color(0x3300E5FF) else Color.Transparent,
                                    animationSpec = spring(),
                                    label = "nav_pill_bg"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .testTag("nav_tab_${item.name.lowercase()}")
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(pillBg)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    1.dp,
                                                    Color(0x5500E5FF),
                                                    RoundedCornerShape(18.dp)
                                                )
                                            } else Modifier
                                        )
                                        .clickable {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
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
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
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
                        },
                        onNavigateToDataBackup = {
                            navController.navigate(Route.DataBackup)
                        },
                        onNavigateToPremium = {
                            navController.navigate(Route.Premium)
                        }
                    ) 
                }
                composable<Route.DataBackup> {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val viewModel: com.example.presentation.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.example.presentation.settings.SettingsViewModel.Factory(ctx.applicationContext as android.app.Application)
                    )
                    com.example.presentation.settings.DataBackupScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Route.Premium> {
                    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.FocusLockApplication)
                    val settings by app.repository.userSettings.collectAsState(initial = com.example.database.UserSettings())
                    val isDark = when (settings.theme) {
                        "DARK" -> true
                        "LIGHT" -> false
                        else -> isSystemInDarkTheme()
                    }
                    com.example.presentation.premium.PremiumScreen(
                        onBack = { navController.popBackStack() },
                        isDark = isDark
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
