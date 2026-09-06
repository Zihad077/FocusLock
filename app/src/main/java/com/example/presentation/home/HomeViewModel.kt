package com.example.presentation.home

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.AppLimit
import com.example.database.DailyUsage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager
    
    private val currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val userSettings = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val limitsWithUsage = combine(
        repository.allLimits,
        repository.getUsageForDate(currentDateString)
    ) { limits, usageList ->
        limits.map { limit ->
            val usage = usageList.find { it.packageName == limit.packageName }?.usedMinutes ?: 0
            
            // Try to get app name if it's empty
            val appName = if (limit.appName.isEmpty()) {
                try {
                    val appInfo = packageManager.getApplicationInfo(limit.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    limit.packageName
                }
            } else {
                limit.appName
            }
            
            AppLimitUIModel(
                packageName = limit.packageName,
                appName = appName,
                dailyLimitMinutes = limit.dailyLimitMinutes,
                usedMinutes = usage,
                isEnabled = limit.isEnabled
            )
        }.sortedByDescending { it.usedMinutes }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val statsSummary = limitsWithUsage.map { limits ->
        val totalUsed = limits.sumOf { it.usedMinutes }
        val totalLimit = limits.sumOf { it.dailyLimitMinutes }
        val timeSaved = maxOf(0, totalLimit - totalUsed)
        StatsSummary(totalUsed, timeSaved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsSummary(0, 0)
    )

    // A simple factory
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class AppLimitUIModel(
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val usedMinutes: Int,
    val isEnabled: Boolean
) {
    val remainingMinutes: Int
        get() = maxOf(0, dailyLimitMinutes - usedMinutes)
        
    val progress: Float
        get() = if (dailyLimitMinutes > 0) (usedMinutes.toFloat() / dailyLimitMinutes).coerceIn(0f, 1f) else 0f
}

data class StatsSummary(
    val totalUsedMinutes: Int,
    val totalSavedMinutes: Int
)
