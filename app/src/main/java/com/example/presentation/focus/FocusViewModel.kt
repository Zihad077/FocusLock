package com.example.presentation.focus

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class FocusViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive = _isFocusActive.asStateFlow()

    private val _remainingTimeSeconds = MutableStateFlow(0)
    val remainingTimeSeconds = _remainingTimeSeconds.asStateFlow()
    
    private val _selectedDurationMinutes = MutableStateFlow(25)
    val selectedDurationMinutes = _selectedDurationMinutes.asStateFlow()
    private val _showJournalDialog = MutableStateFlow(false)
    val showJournalDialog = _showJournalDialog.asStateFlow()
    
    val userSettings = repository.userSettings

    private var timerJob: Job? = null

    init {
        // Recover state
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            if (settings.isFocusModeActive && settings.activeFocusEndTime > System.currentTimeMillis()) {
                val remaining = ((settings.activeFocusEndTime - System.currentTimeMillis()) / 1000).toInt()
                _remainingTimeSeconds.value = remaining
                _isFocusActive.value = true
                startTimer()
            } else if (settings.isFocusModeActive) {
                // Was active but expired while app was dead
                endFocusSession(completed = true)
            }
        }
    }

    fun setDuration(minutes: Int) {
        if (!_isFocusActive.value) {
            _selectedDurationMinutes.value = minutes
        }
    }

    fun startFocusSession() {
        if (_isFocusActive.value) return
        
        _isFocusActive.value = true
        _remainingTimeSeconds.value = _selectedDurationMinutes.value * 60
        
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            val endTime = System.currentTimeMillis() + (_selectedDurationMinutes.value * 60 * 1000L)
            repository.updateSettings(settings.copy(isFocusModeActive = true, activeFocusEndTime = endTime))
            
            try {
                com.example.service.NotificationHelper(getApplication())
                    .showFocusModeNotification(isActive = true, remainingMinutes = _selectedDurationMinutes.value)
            } catch (e: Exception) {
                // Ignore if notifications restricted
            }

            if (settings.focusProtectionEnabled && settings.notificationProtectionEnabled) {
                try {
                    val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (nm?.isNotificationPolicyAccessGranted == true) {
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingTimeSeconds.value > 0) {
                delay(1000)
                _remainingTimeSeconds.value -= 1
            }
            endFocusSession(completed = true)
        }
    }
    
    fun endFocusSession(completed: Boolean = false) {
        timerJob?.cancel()
        _isFocusActive.value = false
        _remainingTimeSeconds.value = 0
        
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            var newXp = settings.xp
            var newLevel = settings.level
            
            if (completed) {
                newXp += 50
                try {
                    val achievements = repository.allAchievements.first()
                    achievements.find { it.id == "first_step" && !it.isUnlocked }?.let {
                        repository.insertAchievement(it.copy(isUnlocked = true))
                        newXp += it.xpReward
                    }
                    if (_selectedDurationMinutes.value >= 120) {
                        achievements.find { it.id == "deep_diver" && !it.isUnlocked }?.let {
                            repository.insertAchievement(it.copy(isUnlocked = true))
                            newXp += it.xpReward
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                if (newXp >= newLevel * 100) {
                    newXp -= (newLevel * 100)
                    newLevel += 1
                }
                repository.insertFocusSession(
                    com.example.database.FocusSession(
                        startTime = System.currentTimeMillis(),
                        durationMinutes = _selectedDurationMinutes.value,
                        isCompleted = true,
                        mode = "DEEP_FOCUS"
                    )
                )
                _showJournalDialog.value = true
            }
            
            repository.updateSettings(settings.copy(
                isFocusModeActive = false, 
                activeFocusEndTime = 0L,
                xp = newXp,
                level = newLevel
            ))

            if (settings.focusProtectionEnabled && settings.notificationProtectionEnabled) {
                try {
                    val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (nm?.isNotificationPolicyAccessGranted == true) {
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            if (completed) {
                try {
                    com.example.service.NotificationHelper(getApplication())
                        .showFocusModeNotification(isActive = false)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun saveJournalEntry(entry: String) {
        viewModelScope.launch {
            val sessions = repository.allFocusSessions.first()
            val latest = sessions.firstOrNull()
            if (latest != null) {
                repository.insertFocusSession(latest.copy(journalEntry = entry))
            }
            _showJournalDialog.value = false
        }
    }
    fun dismissJournalDialog() {
        _showJournalDialog.value = false
    }

    fun awardBonusXP(amount: Int = 100) {
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            var newXp = settings.xp + amount
            var newLevel = settings.level
            if (newXp >= newLevel * 100) {
                newXp -= (newLevel * 100)
                newLevel += 1
            }
            repository.updateSettings(settings.copy(xp = newXp, level = newLevel))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FocusViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
