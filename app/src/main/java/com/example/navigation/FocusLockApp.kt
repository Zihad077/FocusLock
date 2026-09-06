package com.example.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.onboarding.PermissionsScreen
import com.example.presentation.onboarding.WelcomeScreen
import com.example.presentation.home.HomeScreen
import com.example.presentation.apps.AppsScreen
import com.example.presentation.focus.FocusScreen
import com.example.presentation.stats.StatsScreen
import com.example.presentation.settings.SettingsScreen

@Composable
fun FocusLockApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Welcome) {
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
                onPermissionsGranted = {
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
    
    val items = listOf(
        BottomNavItem("Home", Route.Home, Icons.Default.Home),
        BottomNavItem("Apps", Route.Apps, Icons.Default.Lock),
        BottomNavItem("Focus", Route.Focus, Icons.Default.Timer),
        BottomNavItem("Stats", Route.Stats, Icons.Default.BarChart),
        BottomNavItem("Settings", Route.Settings, Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.name) },
                        label = { Text(item.name) },
                        selected = currentDestination?.hierarchy?.any { 
                            it.hasRoute(item.route::class) 
                        } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
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
                    }
                ) 
            }
        }
    }
}

data class BottomNavItem(val name: String, val route: Route, val icon: ImageVector)



