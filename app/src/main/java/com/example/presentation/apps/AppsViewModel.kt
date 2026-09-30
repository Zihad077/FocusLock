package com.example.presentation.apps

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.database.AppLimit
import com.example.database.AppSchedule
import com.example.database.UserSettings
import com.example.service.AppMonitorService
import com.example.util.PermissionHelper
import com.example.util.UsageStatsHelper
import com.example.util.UsageTimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppItem(
    val packageName: String,
    val appName: String,
    val category: String = "Other",
    val isLimited: Boolean = false,
    val dailyLimitMinutes: Int = 0,
    val sessionLimitMinutes: Int? = null,
    val usedTodayMinutes: Int = 0,
    val isHighImpact: Boolean = false
)

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FocusLockApplication).repository

    private val _installedBaseApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _usageMap = MutableStateFlow<Map<String, Int>>(emptyMap())

    // Live permission states validated by ViewModel
    private val _areRequiredPermissionsGranted = MutableStateFlow(
        PermissionHelper.areAllRequiredPermissionsGranted(application)
    )
    val areRequiredPermissionsGranted: StateFlow<Boolean> = _areRequiredPermissionsGranted.asStateFlow()

    private val _missingPermissionNames = MutableStateFlow(
        PermissionHelper.getMissingRequiredPermissionNames(application)
    )
    val missingPermissionNames: StateFlow<List<String>> = _missingPermissionNames.asStateFlow()

    // Emitted whenever a limit creation or activation attempt is rejected due to missing permissions
    private val _permissionGateRequired = MutableSharedFlow<String?>(extraBufferCapacity = 1)
    val permissionGateRequired: SharedFlow<String?> = _permissionGateRequired.asSharedFlow()

    val userSettings: StateFlow<UserSettings> = repository.userSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserSettings()
    )

    val appsList: StateFlow<List<AppItem>> = combine(
        _installedBaseApps,
        repository.allLimits,
        _usageMap
    ) { installed, limits, usage ->
        val limitMap = limits.associateBy { it.packageName }
        installed.map { base ->
            val limit = limitMap[base.packageName]
            base.copy(
                isLimited = limit?.isEnabled == true,
                dailyLimitMinutes = limit?.dailyLimitMinutes ?: 0,
                sessionLimitMinutes = limit?.sessionLimitMinutes,
                usedTodayMinutes = usage[base.packageName] ?: 0
            )
        }.sortedWith(
            compareByDescending<AppItem> { it.isLimited }
                .thenByDescending { it.usedTodayMinutes }
                .thenByDescending { it.isHighImpact }
                .thenBy { it.appName.lowercase() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshPermissions()
        loadInstalledApps()
        syncAndLoad()
    }

    /**
     * Rechecks all required permissions from Android system services.
     * Restores AppMonitorService automatically when permissions are granted.
     */
    fun refreshPermissions(): Boolean {
        val ctx = getApplication<Application>()
        val granted = PermissionHelper.areAllRequiredPermissionsGranted(ctx)
        _areRequiredPermissionsGranted.value = granted
        _missingPermissionNames.value = PermissionHelper.getMissingRequiredPermissionNames(ctx)
        if (granted) {
            AppMonitorService.startService(ctx)
        }
        return granted
    }

    fun syncAndLoad() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshPermissions()
            try {
                UsageStatsHelper.syncHistoricalUsageToDatabase(getApplication(), repository, 3)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayDbList = repository.getUsageForDate(todayStr).first()
                val dbMap = todayDbList
                    .groupBy { it.packageName }
                    .mapValues { (_, list) -> list.maxOfOrNull { it.usedMinutes } ?: 0 }

                val limits = repository.allLimits.first()
                val liveSummary = UsageStatsHelper.getScreenTimeSummary(
                    getApplication(),
                    UsageTimeRange.TODAY,
                    limits
                )
                val merged = dbMap.toMutableMap()
                liveSummary.appUsageList.forEach { breakdown ->
                    val current = merged[breakdown.packageName] ?: 0
                    if (breakdown.usedMinutes > current) {
                        merged[breakdown.packageName] = breakdown.usedMinutes
                    }
                }
                _usageMap.value = merged
            } catch (e: Exception) {
                // Ignore if usage permission not granted yet
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val myPackageName = getApplication<Application>().packageName

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

            val list = resolveInfos
                .mapNotNull { it.activityInfo?.applicationInfo }
                .distinctBy { it.packageName }
                .filter { it.packageName != myPackageName }
                .map { appInfo ->
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val category = determineCategory(appInfo, name)
                    val isHighImpact = category == "Social" || category == "Games" || category == "Entertainment"
                    AppItem(
                        packageName = appInfo.packageName,
                        appName = name,
                        category = category,
                        isHighImpact = isHighImpact
                    )
                }
            _installedBaseApps.value = list
        }
    }

    private fun determineCategory(appInfo: ApplicationInfo, name: String): String {
        val pkg = appInfo.packageName.lowercase()
        val lowerName = name.lowercase()
        return when {
            pkg.contains("instagram") || pkg.contains("facebook") || pkg.contains("tiktok") ||
            pkg.contains("twitter") || pkg.contains("snapchat") || pkg.contains("whatsapp") ||
            pkg.contains("reddit") || pkg.contains("threads") || pkg.contains("discord") ||
            pkg.contains("telegram") -> "Social"
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("spotify") ||
            pkg.contains("twitch") || pkg.contains("disney") || pkg.contains("primevideo") -> "Entertainment"
            appInfo.category == ApplicationInfo.CATEGORY_GAME || pkg.contains("game") ||
            lowerName.contains("game") || pkg.contains("supercell") || pkg.contains("roblox") -> "Games"
            appInfo.category == ApplicationInfo.CATEGORY_PRODUCTIVITY || pkg.contains("docs") ||
            pkg.contains("notion") || pkg.contains("slack") || pkg.contains("calendar") ||
            pkg.contains("gmail") -> "Productivity"
            else -> "Other"
        }
    }

    /**
     * Validates required permissions before creating or updating an active app limit.
     * Prevents new app limits from being activated when required permissions are missing.
     */
    fun setCustomLimit(
        app: AppItem,
        dailyMinutes: Int,
        sessionMinutes: Int?,
        isEnabled: Boolean = true
    ): Boolean {
        if (isEnabled && !refreshPermissions()) {
            _permissionGateRequired.tryEmit(app.appName)
            return false
        }
        viewModelScope.launch {
            val existing = repository.getLimit(app.packageName)
            val limit = existing?.copy(
                appName = app.appName,
                dailyLimitMinutes = dailyMinutes,
                sessionLimitMinutes = sessionMinutes,
                isEnabled = isEnabled
            ) ?: AppLimit(
                packageName = app.packageName,
                appName = app.appName,
                dailyLimitMinutes = dailyMinutes,
                sessionLimitMinutes = sessionMinutes,
                isEnabled = isEnabled
            )
            repository.insertLimit(limit)
            if (isEnabled) {
                AppMonitorService.startService(getApplication())
            }
        }
        return true
    }

    /**
     * Validates required permissions before enabling an app limit.
     * Existing configured limits are never deleted if permissions are revoked.
     */
    fun toggleLimit(app: AppItem, isEnabled: Boolean): Boolean {
        if (isEnabled && !refreshPermissions()) {
            _permissionGateRequired.tryEmit(app.appName)
            return false
        }
        viewModelScope.launch {
            val existing = repository.getLimit(app.packageName)
            if (existing != null) {
                repository.insertLimit(existing.copy(isEnabled = isEnabled))
            } else if (isEnabled) {
                repository.insertLimit(
                    AppLimit(
                        packageName = app.packageName,
                        appName = app.appName,
                        dailyLimitMinutes = if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes else 30,
                        sessionLimitMinutes = app.sessionLimitMinutes,
                        isEnabled = true
                    )
                )
            }
            if (isEnabled) {
                AppMonitorService.startService(getApplication())
            }
        }
        return true
    }

    /**
     * Applies a quick restriction template only if required permissions are granted.
     */
    fun applyTemplate(templateTitle: String): Boolean {
        if (!refreshPermissions()) {
            _permissionGateRequired.tryEmit(templateTitle)
            return false
        }
        viewModelScope.launch {
            val currentApps = _installedBaseApps.value
            val (targetCategory, limitMinutes) = when (templateTitle) {
                "Social Media" -> "Social" to 30
                "Gaming" -> "Games" to 45
                "Entertainment" -> "Entertainment" to 60
                else -> "Social" to 30
            }
            val matchingApps = currentApps.filter { it.category == targetCategory }
            matchingApps.forEach { app ->
                val existing = repository.getLimit(app.packageName)
                val updated = existing?.copy(
                    dailyLimitMinutes = limitMinutes,
                    isEnabled = true
                ) ?: AppLimit(
                    packageName = app.packageName,
                    appName = app.appName,
                    dailyLimitMinutes = limitMinutes,
                    isEnabled = true
                )
                repository.insertLimit(updated)
            }
            AppMonitorService.startService(getApplication())
        }
        return true
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteLimit(packageName)
        }
    }

    fun getSchedules(packageName: String): Flow<List<AppSchedule>> {
        return repository.getSchedulesForApp(packageName)
    }

    fun addSchedule(packageName: String, startMinute: Int, endMinute: Int, days: String): Boolean {
        if (!refreshPermissions()) {
            _permissionGateRequired.tryEmit(null)
            return false
        }
        viewModelScope.launch {
            repository.insertSchedule(
                AppSchedule(
                    packageName = packageName,
                    startTimeMinuteOfDay = startMinute,
                    endTimeMinuteOfDay = endMinute,
                    daysOfWeek = days
                )
            )
            AppMonitorService.startService(getApplication())
        }
        return true
    }

    fun deleteSchedule(scheduleId: Int) {
        viewModelScope.launch {
            repository.deleteSchedule(scheduleId)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
