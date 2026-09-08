package com.example.presentation.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.Achievement
import com.example.database.Goal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    val goals = repository.allGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val achievements = repository.allAchievements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userSettings = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun addGoal(title: String, target: Int, current: Int, type: String) {
        viewModelScope.launch {
            repository.insertGoal(
                Goal(
                    title = title,
                    targetValue = target,
                    currentValue = current,
                    type = type
                )
            )
        }
    }

    fun awardBonusXP(amount: Int = 100) {
        viewModelScope.launch {
            val settings = userSettings.value ?: return@launch
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
            if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GoalsViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
