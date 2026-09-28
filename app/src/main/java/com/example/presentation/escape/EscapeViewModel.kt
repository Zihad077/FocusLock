package com.example.presentation.escape

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.EscapeAttempt
import com.example.database.UserSettings
import com.example.service.AppMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EscapeViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    val userSettings = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val escapeAttempts = repository.allEscapeAttempts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            if (settings.autoServiceRecoveryEnabled || settings.stableLockModeEnabled) {
                AppMonitorService.startService(getApplication())
            }
        }
    }

    fun recordEscapeAttempt(type: String, packageName: String = "com.example.focuslock") {
        viewModelScope.launch {
            repository.insertEscapeAttempt(
                EscapeAttempt(
                    packageName = packageName,
                    type = type
                )
            )
        }
    }

    fun clearEscapeAttempts() {
        viewModelScope.launch {
            repository.clearEscapeAttempts()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EscapeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EscapeViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
