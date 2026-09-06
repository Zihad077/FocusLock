package com.example.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable data object Welcome : Route()
    @Serializable data object Permissions : Route()
    @Serializable data object MainTab : Route() // Wrapper for bottom tabs
    
    // Bottom Tabs
    @Serializable data object Home : Route()
    @Serializable data object Apps : Route()
    @Serializable data object Focus : Route()
    @Serializable data object Stats : Route()
    @Serializable data object Settings : Route()
    
    // Deep pages
    @Serializable data class AppLimitDetail(val packageName: String) : Route()
}
