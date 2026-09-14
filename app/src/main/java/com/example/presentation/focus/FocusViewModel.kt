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
    
    // Mandatory 2:30 cooling down state
    private val _cooldownRemainingSeconds = MutableStateFlow<Int?>(null)
    val cooldownRemainingSeconds = _cooldownRemainingSeconds.asStateFlow()

    private val _showExitConfirmation = MutableStateFlow(false)
    val showExitConfirmation = _showExitConfirmation.asStateFlow()

    val userSettings = repository.userSettings

    private var timerJob: Job? = null

    init {
        // Recover and synchronize state based on real wall-clock timestamps
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            syncWithStoredState(settings)
        }
    }

    /**
     * Synchronizes state with persistent storage based on real elapsed time.
     */
    fun syncWithStoredState(settings: com.example.database.UserSettings) {
        val now = System.currentTimeMillis()
        if (settings.isFocusModeActive && settings.activeFocusEndTime > now) {
            val startTime = if (settings.focusSessionStartTime > 0L) settings.focusSessionStartTime else now
            val remainingSec = ((settings.activeFocusEndTime - now + 999L) / 1000L).coerceAtLeast(0L).toInt()
            _remainingTimeSeconds.value = remainingSec
            _isFocusActive.value = true

            val elapsedMillis = (now - startTime).coerceAtLeast(0L)
            val lockRemainingSec = if (elapsedMillis >= 150_000L) {
                0
            } else {
                ((150_000L - elapsedMillis + 999L) / 1000L).coerceAtLeast(0L).toInt()
            }
            _cooldownRemainingSeconds.value = lockRemainingSec

            startTimer(startTime = startTime, endTime = settings.activeFocusEndTime)
        } else if (settings.isFocusModeActive && settings.activeFocusEndTime <= now) {
            // Expired while dead/backgrounded
            endFocusSession(completed = true)
        } else {
            _isFocusActive.value = false
            _remainingTimeSeconds.value = 0
            _cooldownRemainingSeconds.value = null
        }
    }

    fun setDuration(minutes: Int) {
        if (!_isFocusActive.value) {
            _selectedDurationMinutes.value = minutes
        }
    }

    fun startFocusSession() {
        val now = System.currentTimeMillis()
        if (_isFocusActive.value) return
        
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            if (settings.isFocusModeActive && settings.activeFocusEndTime > now) {
                syncWithStoredState(settings)
                return@launch
            }

            val durationMinutes = _selectedDurationMinutes.value
            val durationMillis = durationMinutes * 60 * 1000L
            val endTime = now + durationMillis
            val cooldownEndTime = now + 150_000L // Strict 2 min 30 sec (150 seconds) cooldown
            
            _isFocusActive.value = true
            _remainingTimeSeconds.value = durationMinutes * 60
            _cooldownRemainingSeconds.value = 150
            
            repository.updateSettings(
                settings.copy(
                    isFocusModeActive = true,
                    activeFocusEndTime = endTime,
                    focusSessionStartTime = now,
                    focusExitCooldownEndTime = cooldownEndTime
                )
            )
            
            try {
                com.example.service.NotificationHelper(getApplication())
                    .showFocusModeNotification(isActive = true, remainingMinutes = durationMinutes)
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

            startTimer(startTime = now, endTime = endTime)
        }
    }
    
    private fun startTimer(startTime: Long, endTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                if (now >= endTime) {
                    _remainingTimeSeconds.value = 0
                    _cooldownRemainingSeconds.value = 0
                    endFocusSession(completed = true)
                    break
                }

                val remainingSec = ((endTime - now + 999L) / 1000L).coerceAtLeast(0L).toInt()
                _remainingTimeSeconds.value = remainingSec

                val elapsedMillis = (now - startTime).coerceAtLeast(0L)
                val cooldownSec = if (elapsedMillis >= 150_000L) {
                    0
                } else {
                    ((150_000L - elapsedMillis + 999L) / 1000L).coerceAtLeast(0L).toInt()
                }
                _cooldownRemainingSeconds.value = cooldownSec

                delay(500)
            }
        }
    }

    fun requestExitFocusSession() {
        // Kept for backward compatibility
    }

    fun resumeFocusSession() {
        _showExitConfirmation.value = false
    }

    fun promptExitConfirmation() {
        if ((_cooldownRemainingSeconds.value ?: 0) == 0) {
            _showExitConfirmation.value = true
        }
    }

    fun dismissExitConfirmation() {
        _showExitConfirmation.value = false
    }

    fun confirmEarlyExit() {
        if ((_cooldownRemainingSeconds.value ?: 0) != 0) return
        _showExitConfirmation.value = false
        endFocusSession(completed = false)
    }

    /**
     * Ends the focus session.
     * When completed == false (manual user turn-off):
     * Strictly verifies that at least 150 seconds (2m 30s) of REAL wall-clock time have elapsed.
     */
    fun endFocusSession(completed: Boolean = false) {
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            val now = System.currentTimeMillis()

            // 2.5-Minute Minimum Enforcement:
            if (!completed && settings.isFocusModeActive) {
                val startTime = settings.focusSessionStartTime
                val elapsedMillis = now - startTime
                if (startTime > 0L && elapsedMillis < 150_000L) {
                    // Under 150 seconds: rejection of early exit
                    val lockRemaining = ((150_000L - elapsedMillis + 999L) / 1000L).toInt()
                    _cooldownRemainingSeconds.value = lockRemaining
                    return@launch
                }
            }

            timerJob?.cancel()
            _cooldownRemainingSeconds.value = null
            _showExitConfirmation.value = false
            _isFocusActive.value = false
            _remainingTimeSeconds.value = 0
            
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
                        startTime = if (settings.focusSessionStartTime > 0L) settings.focusSessionStartTime else now,
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
                focusExitCooldownEndTime = 0L,
                focusSessionStartTime = 0L,
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

            try {
                com.example.service.NotificationHelper(getApplication())
                    .showFocusModeNotification(isActive = false)
            } catch (e: Exception) {
                // Ignore
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
