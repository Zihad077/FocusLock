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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager
    
    private fun getTodayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val _currentDateFlow = MutableStateFlow(getTodayDateString())
    private val _realtimeTotalMinutes = MutableStateFlow<Int?>(null)

    init {
        syncUsageData()
    }

    fun syncUsageData() {
        _currentDateFlow.value = getTodayDateString()
        viewModelScope.launch {
            try {
                // 1. Sync historical data to database
                com.example.util.UsageStatsHelper.syncHistoricalUsageToDatabase(getApplication(), repository, 7)
                // 2. Query today's live screen time summary directly so Home and Stats are in exact alignment
                val limits = repository.allLimits.first()
                val summary = com.example.util.UsageStatsHelper.getScreenTimeSummary(getApplication(), com.example.util.UsageTimeRange.TODAY, limits)
                _realtimeTotalMinutes.value = summary.totalScreenTimeMinutes
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val userSettings = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val limitsWithUsage = _currentDateFlow.flatMapLatest { today ->
        combine(
            repository.allLimits,
            repository.getUsageForDate(today)
        ) { limits, usageList ->
            limits.map { limit ->
                val usage = usageList.find { it.packageName == limit.packageName }?.usedMinutes ?: 0
                
                // Always retrieve real original device app name directly from PackageManager
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(limit.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    if (limit.appName.isNotEmpty()) limit.appName else limit.packageName
                }
                
                AppLimitUIModel(
                    packageName = limit.packageName,
                    appName = appName,
                    dailyLimitMinutes = limit.dailyLimitMinutes,
                    usedMinutes = usage,
                    isEnabled = limit.isEnabled
                )
            }.sortedByDescending { it.usedMinutes }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val statsSummary = _currentDateFlow.flatMapLatest { today ->
        combine(
            limitsWithUsage,
            repository.getUsageForDate(today),
            _realtimeTotalMinutes
        ) { limits, allTodayUsage, liveTotalMinutes ->
            val dbTotalToday = allTodayUsage.sumOf { it.usedMinutes }
            val totalTodayScreenTime = liveTotalMinutes ?: dbTotalToday
            val totalLimit = limits.sumOf { it.dailyLimitMinutes }
            val totalUsedOnLimitedApps = limits.sumOf { it.usedMinutes }
            val timeSaved = if (totalLimit > 0) maxOf(0, totalLimit - totalUsedOnLimitedApps) else 0
            StatsSummary(if (totalTodayScreenTime > 0) totalTodayScreenTime else totalUsedOnLimitedApps, timeSaved)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsSummary(0, 0)
    )

    fun updateLimit(packageName: String, appName: String, dailyMinutes: Int) {
        viewModelScope.launch {
            val existing = repository.getLimit(packageName)
            if (existing != null) {
                repository.insertLimit(existing.copy(dailyLimitMinutes = dailyMinutes, isEnabled = true))
            } else {
                repository.insertLimit(
                    AppLimit(
                        packageName = packageName,
                        appName = appName,
                        isEnabled = true,
                        dailyLimitMinutes = dailyMinutes
                    )
                )
            }
        }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteLimit(packageName)
        }
    }

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
