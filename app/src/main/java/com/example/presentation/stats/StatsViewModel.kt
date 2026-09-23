package com.example.presentation.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.AppLimit
import com.example.util.AppUsageInfo
import com.example.util.DailyStat
import com.example.util.PermissionHelper
import com.example.util.ScreenTimeSummary
import com.example.util.UsageStatsHelper
import com.example.util.UsageTimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val _selectedTimeRange = MutableStateFlow(UsageTimeRange.TODAY)
    val selectedTimeRange: StateFlow<UsageTimeRange> = _selectedTimeRange.asStateFlow()

    private val _isUsageAccessGranted = MutableStateFlow(
        PermissionHelper.hasUsageAccess(application)
    )
    val isUsageAccessGranted: StateFlow<Boolean> = _isUsageAccessGranted.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _screenTimeSummary = MutableStateFlow(ScreenTimeSummary())
    val screenTimeSummary: StateFlow<ScreenTimeSummary> = _screenTimeSummary.asStateFlow()

    val userSettings = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val totalFocusSessions = repository.allFocusSessions.map { sessions ->
        sessions.count { it.isCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalFocusTime = repository.allFocusSessions.map { sessions ->
        sessions.filter { it.isCompleted }.sumOf { it.durationMinutes }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalEscapeAttempts = repository.allEscapeAttempts.map { attempts ->
        attempts.size
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        checkPermissionAndSync()
    }

    fun checkPermissionAndSync() {
        val hasAccess = PermissionHelper.hasUsageAccess(getApplication())
        _isUsageAccessGranted.value = hasAccess

        viewModelScope.launch {
            if (hasAccess) {
                _isRefreshing.value = true
                // Sync historical 7 days to Room database in background
                UsageStatsHelper.syncHistoricalUsageToDatabase(getApplication(), repository, 7)
                // Load detailed summary for currently selected time range
                loadUsageData()
                _isRefreshing.value = false
            } else {
                _screenTimeSummary.value = ScreenTimeSummary()
            }
        }
    }

    fun setTimeRange(timeRange: UsageTimeRange) {
        _selectedTimeRange.value = timeRange
        loadUsageData()
    }

    fun refreshUsageData() {
        checkPermissionAndSync()
    }

    private fun loadUsageData() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val hasAccess = PermissionHelper.hasUsageAccess(app)
            _isUsageAccessGranted.value = hasAccess
            if (!hasAccess) return@launch

            val timeRange = _selectedTimeRange.value
            val summary = withContext(Dispatchers.IO) {
                // Fetch latest limits to flag which apps have active limits
                val limits = try {
                    repository.allLimits.first()
                } catch (e: Exception) {
                    emptyList()
                }
                UsageStatsHelper.getScreenTimeSummary(app, timeRange, limits)
            }
            _screenTimeSummary.value = summary
        }
    }

    fun setDailyLimit(packageName: String, appName: String, dailyMinutes: Int) {
        viewModelScope.launch {
            val existing = repository.getLimit(packageName)
            if (existing != null) {
                repository.insertLimit(
                    existing.copy(
                        dailyLimitMinutes = dailyMinutes,
                        isEnabled = true
                    )
                )
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
            // Refresh to update active limit badge
            loadUsageData()
        }
    }

    fun removeDailyLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteLimit(packageName)
            loadUsageData()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StatsViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
