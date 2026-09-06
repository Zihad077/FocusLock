package com.example.presentation.focus

import android.app.Application
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
            }
            
            repository.updateSettings(settings.copy(
                isFocusModeActive = false, 
                activeFocusEndTime = 0L,
                xp = newXp,
                level = newLevel
            ))
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
