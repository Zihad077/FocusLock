package com.example.presentation.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import com.example.database.UsageEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class InsightsState(
    val todayScore: Int = 0,
    val recommendations: List<String> = emptyList(),
    val usageEvents: List<UsageEvent> = emptyList()
)

class InsightsViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(InsightsState())
    val state: StateFlow<InsightsState> = _state.asStateFlow()
    val userSettings = repository.userSettings

    init {
        loadInsights()
    }

    private fun loadInsights() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        viewModelScope.launch {
            combine(
                repository.getAllUsage(),
                repository.allLimits,
                repository.allFocusSessions,
                repository.getUsageEventsForDate(todayStr),
                repository.userSettings
            ) { usages, limits, sessions, events, settings ->
                val todayUsages = usages.filter { it.dateString == todayStr }
                val totalUsedMinutes = todayUsages.sumOf { it.usedMinutes }
                
                // Calculate Score
                var score = 100
                
                val exceededLimits = limits.filter { limit ->
                    val usage = todayUsages.find { it.packageName == limit.packageName }?.usedMinutes ?: 0
                    limit.dailyLimitMinutes > 0 && usage > limit.dailyLimitMinutes
                }
                score -= exceededLimits.size * 15
                
                val todaySessions = sessions.filter { 
                    val sessionDate = dateFormat.format(Date(it.startTime))
                    sessionDate == todayStr && it.isCompleted 
                }
                score += todaySessions.size * 10
                score += (settings.currentStreak * 2).coerceAtMost(20)
                
                score = score.coerceIn(0, 100)
                
                val recs = mutableListOf<String>()
                if (exceededLimits.isNotEmpty()) {
                    recs.add("You exceeded ${exceededLimits.size} limits today. Consider increasing them or starting a Focus Session.")
                }
                if (todaySessions.isEmpty()) {
                    recs.add("You haven't done any Focus Sessions today. Try a short 15-minute session.")
                }
                
                val highestUsage = todayUsages.maxByOrNull { it.usedMinutes }
                if (highestUsage != null && highestUsage.usedMinutes > 60) {
                    val appName = limits.find { it.packageName == highestUsage.packageName }?.appName ?: "An app"
                    recs.add("You spent over an hour on $appName. Maybe set a stricter daily limit?")
                }

                if (recs.isEmpty()) {
                    recs.add("Great job today! You are maintaining a balanced digital diet.")
                }
                
                InsightsState(
                    todayScore = score,
                    recommendations = recs,
                    usageEvents = events
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InsightsViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
