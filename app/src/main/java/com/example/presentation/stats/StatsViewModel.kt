package com.example.presentation.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // Mock data for the chart
    private val _weeklyUsage = MutableStateFlow(
        listOf(
            DailyStat("Mon", 120),
            DailyStat("Tue", 100),
            DailyStat("Wed", 130),
            DailyStat("Thu", 80),
            DailyStat("Fri", 110),
            DailyStat("Sat", 120),
            DailyStat("Sun", 90)
        )
    )
    val weeklyUsage = _weeklyUsage.asStateFlow()

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StatsViewModel(
                    application,
                    (application as FocusLockApplication).repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class DailyStat(
    val dayName: String,
    val minutes: Int
)
