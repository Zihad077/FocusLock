package com.example.presentation.apps

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.AppLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    
    val appsList = combine(
        _installedApps,
        repository.allLimits,
        repository.allTemporaryUnlocks
    ) { installed, limits, tempUnlocks ->
        val limitMap = limits.associateBy { it.packageName }
        val currentMillis = System.currentTimeMillis()
        val tempUnlockMap = tempUnlocks
            .filter { it.startTime + (it.durationMinutes * 60 * 1000L) > currentMillis }
            .associateBy { it.packageName }
            
        installed.map { app ->
            val limit = limitMap[app.packageName]
            val isHigh = isHighImpactApp(app.packageName, app.appName)
            val unlock = tempUnlockMap[app.packageName]
            app.copy(
                isLimited = limit != null && limit.isEnabled,
                dailyLimitMinutes = limit?.dailyLimitMinutes ?: 0,
                sessionLimitMinutes = limit?.sessionLimitMinutes,
                isHighImpact = isHigh,
                activeUnlockMethod = unlock?.type,
                activeUnlockRemainingMinutes = if (unlock != null) {
                    ((unlock.startTime + (unlock.durationMinutes * 60 * 1000L) - currentMillis) / (60 * 1000L)).toInt().coerceAtLeast(1)
                } else null
            )
        }.sortedWith(
            compareByDescending<AppItem> { it.activeUnlockMethod != null }
                .thenByDescending { it.isLimited }
                .thenByDescending { it.isHighImpact }
                .thenBy { it.appName.lowercase() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
                intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
                
                resolveInfoList.map { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    val name = resolveInfo.loadLabel(packageManager).toString()
                    AppItem(
                        packageName = pkg,
                        appName = name,
                        isHighImpact = isHighImpactApp(pkg, name)
                    )
                }.distinctBy { it.packageName }
                 .filter { it.packageName != getApplication<Application>().packageName }
            }
            _installedApps.value = apps
        }
    }
    
    fun setCustomLimit(app: AppItem, minutes: Int, sessionMinutes: Int? = null, isEnabled: Boolean = true) {
        viewModelScope.launch {
            repository.insertLimit(
                AppLimit(
                    packageName = app.packageName,
                    appName = app.appName,
                    isEnabled = isEnabled,
                    dailyLimitMinutes = minutes,
                    sessionLimitMinutes = sessionMinutes
                )
            )
        }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteLimit(packageName)
        }
    }

    fun toggleLimit(app: AppItem, isEnabled: Boolean) {
        viewModelScope.launch {
            if (isEnabled) {
                val limit = repository.getLimit(app.packageName)
                if (limit != null) {
                    repository.insertLimit(limit.copy(isEnabled = true))
                } else {
                    // Default to 30 mins if none exists
                    repository.insertLimit(
                        AppLimit(
                            packageName = app.packageName,
                            appName = app.appName,
                            isEnabled = true,
                            dailyLimitMinutes = 30
                        )
                    )
                }
            } else {
                val limit = repository.getLimit(app.packageName)
                if (limit != null) {
                    repository.insertLimit(limit.copy(isEnabled = false))
                }
            }
        }
    }

    companion object {
        private val HIGH_IMPACT_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.facebook.orca",
            "com.twitter.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.netflix.mediaclient",
            "tv.twitch.android.app",
            "com.discord",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.android.chrome",
            "com.pinterest",
            "com.tiktok.android"
        )

        fun isHighImpactApp(packageName: String, appName: String): Boolean {
            if (HIGH_IMPACT_PACKAGES.contains(packageName)) return true
            val lowerName = appName.lowercase()
            val keywords = listOf(
                "youtube", "instagram", "tiktok", "facebook", "twitter", "reddit",
                "netflix", "snapchat", "twitch", "discord", "game", "browser"
            )
            return keywords.any { lowerName.contains(it) }
        }
    }

    fun applyTemplate(templateName: String) {
        viewModelScope.launch {
            val installedApps = _installedApps.value
            val limitsToApply = mutableListOf<com.example.database.AppLimit>()
            when (templateName) {
                "Social Media" -> {
                    installedApps.filter { it.packageName.contains("facebook") || it.packageName.contains("instagram") || it.packageName.contains("twitter") || it.packageName.contains("tiktok") }
                        .forEach { limitsToApply.add(com.example.database.AppLimit(it.packageName, it.appName, true, 30)) }
                }
                "Gaming" -> {
                    installedApps.filter { it.packageName.contains("game") || it.packageName.contains("pubg") || it.packageName.contains("minecraft") }
                        .forEach { limitsToApply.add(com.example.database.AppLimit(it.packageName, it.appName, true, 45)) }
                }
                "Entertainment" -> {
                    installedApps.filter { it.packageName.contains("youtube") || it.packageName.contains("netflix") || it.packageName.contains("spotify") }
                        .forEach { limitsToApply.add(com.example.database.AppLimit(it.packageName, it.appName, true, 60)) }
                }
            }
            limitsToApply.forEach {
                repository.insertLimit(it)
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppsViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class AppItem(
    val packageName: String,
    val appName: String,
    val isLimited: Boolean = false,
    val dailyLimitMinutes: Int = 0,
    val sessionLimitMinutes: Int? = null,
    val isHighImpact: Boolean = false,
    val activeUnlockMethod: String? = null,
    val activeUnlockRemainingMinutes: Int? = null
)
