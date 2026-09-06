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
            repository.updateSettings(settings.copy(isFocusModeActive = true))
        }
        
        timerJob = viewModelScope.launch {
            while (_remainingTimeSeconds.value > 0) {
                delay(1000)
                _remainingTimeSeconds.value -= 1
            }
            endFocusSession()
        }
    }
    
    fun endFocusSession() {
        timerJob?.cancel()
        _isFocusActive.value = false
        _remainingTimeSeconds.value = 0
        
        viewModelScope.launch {
            val settings = repository.userSettings.first()
            repository.updateSettings(settings.copy(isFocusModeActive = false))
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
