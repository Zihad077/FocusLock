package com.example.presentation.apps

import android.app.Application
import android.content.pm.ApplicationInfo
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        repository.allLimits
    ) { installed, limits ->
        val limitMap = limits.associateBy { it.packageName }
        installed.map { app ->
            val limit = limitMap[app.packageName]
            app.copy(
                isLimited = limit != null && limit.isEnabled,
                dailyLimitMinutes = limit?.dailyLimitMinutes ?: 0
            )
        }.sortedByDescending { it.isLimited }
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
                    AppItem(
                        packageName = resolveInfo.activityInfo.packageName,
                        appName = resolveInfo.loadLabel(packageManager).toString()
                    )
                }.distinctBy { it.packageName }
                 .filter { it.packageName != getApplication<Application>().packageName }
            }
            _installedApps.value = apps
        }
    }
    
    fun toggleLimit(app: AppItem, isEnabled: Boolean) {
        viewModelScope.launch {
            if (isEnabled) {
                // Set default limit of 30 minutes if none exists
                val limit = repository.getLimit(app.packageName)
                if (limit != null) {
                    repository.insertLimit(limit.copy(isEnabled = true))
                } else {
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
    val dailyLimitMinutes: Int = 0
)
