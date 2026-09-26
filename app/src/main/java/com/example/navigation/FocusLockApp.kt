package com.example.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import com.example.FocusLockApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_v2_completed"

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
    // First launch -> Welcome. Subsequent launches -> MainTab (or Permissions if not yet granted).
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
                    // Mark first-launch welcome experience as completed so it doesn't replay on every launch
                    sharedPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                    isOnboardingCompleted = true

                    if (PermissionHelper.areAllRequiredPermissionsGranted(context)) {
                        navController.navigate(Route.MainTab) {
                            popUpTo(Route.Welcome) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Route.Permissions) {
                            popUpTo(Route.Welcome) { inclusive = true }
                        }
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
    val context = LocalContext.current
    val app = context.applicationContext as FocusLockApplication
    val userSettings by app.repository.userSettings.collectAsState(initial = com.example.database.UserSettings())
    val now = System.currentTimeMillis()
    val isFocusActive = userSettings.isFocusModeActive && userSettings.activeFocusEndTime > now

    val navController = rememberNavController()
    val isDark = isSystemInDarkTheme()

    // Back navigation disabled during active Focus Mode
    BackHandler(enabled = isFocusActive) {
        // Intentionally consumed: Focus Mode screen must remain the only accessible screen
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Strictly redirect and lock to Route.Focus when focus session is running
    LaunchedEffect(isFocusActive) {
        if (isFocusActive && currentDestination?.hasRoute(Route.Focus::class) != true) {
            navController.navigate(Route.Focus) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(currentDestination, isFocusActive) {
        if (isFocusActive && currentDestination?.hasRoute(Route.Focus::class) != true) {
            navController.navigate(Route.Focus) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
            }
        }
    }

    // 5 primary tabs with localized labels (Goals & Insights integrated into Home/Stats)
    val items = listOf(
        BottomNavItem(androidx.compose.ui.res.stringResource(com.example.R.string.nav_home), Route.Home, Icons.Default.Home),
        BottomNavItem(androidx.compose.ui.res.stringResource(com.example.R.string.nav_apps), Route.Apps, Icons.Default.Lock),
        BottomNavItem(androidx.compose.ui.res.stringResource(com.example.R.string.nav_focus), Route.Focus, Icons.Default.Timer),
        BottomNavItem(androidx.compose.ui.res.stringResource(com.example.R.string.nav_stats), Route.Stats, Icons.Default.BarChart),
        BottomNavItem(androidx.compose.ui.res.stringResource(com.example.R.string.nav_settings), Route.Settings, Icons.Default.Settings)
    )

    val isTopLevelDestination = items.any { currentDestination?.hasRoute(it.route::class) == true }
    val showBottomBar = !isFocusActive && isTopLevelDestination

    LiquidBackground(theme = userSettings.theme) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(animationSpec = tween(320, easing = EaseOutCubic)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(220, easing = EaseInCubic)) +
                            scaleOut(targetScale = 0.98f, animationSpec = tween(220))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(320, easing = EaseOutCubic)) +
                            scaleIn(
                                initialScale = 0.98f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(220, easing = EaseInCubic)) +
                            scaleOut(targetScale = 0.96f, animationSpec = tween(220))
                }
            ) {
                composable<Route.Home> {
                    HomeScreen(
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
                        onNavigateToStats = {
                            navController.navigate(Route.Stats) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToPermissions = {
                            navController.navigate(Route.Permissions)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Route.Settings) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable<Route.Apps> { AppsScreen() }
                composable<Route.Focus> { 
                    FocusScreen(
                        onNavigateToHome = {
                            navController.navigate(Route.Home) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    ) 
                }
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

            // Floating Frosted Liquid Glass Bottom Navigation Bar with Scrim
            if (showBottomBar) {
                // Soft bottom gradient scrim to smoothly dim content scrolling underneath
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x9008121C),
                                    Color(0xEE060E16)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xF618283B),
                                    Color(0xF2101F2F),
                                    Color(0xF90A1522)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF24DFEC).copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color(0xFF24DFEC).copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.15f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(600f, 600f)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any { 
                                it.hasRoute(item.route::class) 
                            } == true

                            val itemInteractionSource = remember { MutableInteractionSource() }
                            val isItemPressed by itemInteractionSource.collectIsPressedAsState()

                            val itemScale by animateFloatAsState(
                                targetValue = if (isItemPressed) 0.92f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "nav_item_scale"
                            )

                            val iconTint by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFF24DFEC) else Color.White.copy(alpha = 0.65f),
                                animationSpec = spring(),
                                label = "nav_icon_tint"
                            )

                            val pillBg by animateColorAsState(
                                targetValue = if (isSelected) Color(0x3524DFEC) else Color.Transparent,
                                animationSpec = spring(),
                                label = "nav_pill_bg"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .testTag("nav_tab_${item.name.lowercase()}")
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(
                                        interactionSource = itemInteractionSource,
                                        indication = null
                                    ) {
                                        if (item.route == Route.Home) {
                                            navController.navigate(Route.Home) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    inclusive = false
                                                    saveState = false
                                                }
                                                launchSingleTop = true
                                            }
                                        } else if (!isSelected) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            ) {
                                // Pill indicator around active icon
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(pillBg)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    1.2.dp,
                                                    Color(0xFF24DFEC).copy(alpha = 0.70f),
                                                    RoundedCornerShape(14.dp)
                                                )
                                            } else Modifier
                                        )
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.name,
                                        tint = iconTint,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    ),
                                    color = iconTint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BottomNavItem(val name: String, val route: Route, val icon: ImageVector)
