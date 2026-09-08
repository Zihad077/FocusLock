package com.example.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    val repository: AppRepository
) : AndroidViewModel(application) {

    val userSettings = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            repository.updateSettings(userSettings.value.copy(theme = theme))
        }
    }
    
    fun updateLanguage(language: String) {
        viewModelScope.launch {
            repository.updateSettings(userSettings.value.copy(language = language))
        }
    }

    fun updateMaxEmergencyUnlocks(max: Int) {
        viewModelScope.launch {
            val current = userSettings.value
            val remaining = current.emergencyUnlocksRemaining.coerceAtMost(max)
            repository.updateSettings(current.copy(maxEmergencyUnlocks = max, emergencyUnlocksRemaining = remaining))
        }
    }

    fun resetEmergencyUnlocksToday() {
        viewModelScope.launch {
            val current = userSettings.value
            repository.updateSettings(current.copy(emergencyUnlocksRemaining = current.maxEmergencyUnlocks))
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            val current = userSettings.value
            repository.updateSettings(
                current.copy(
                    xp = 0,
                    level = 1,
                    focusScore = 0,
                    currentStreak = 0,
                    emergencyUnlocksRemaining = current.maxEmergencyUnlocks
                )
            )
        }
    }

    fun sendTestNotification() {
        val helper = com.example.service.NotificationHelper(getApplication())
        helper.showNotification(
            "FocusLock Active",
            "Notifications are working! Your focus sessions and limits are guarded."
        )
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
