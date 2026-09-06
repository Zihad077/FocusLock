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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    val weeklyUsage = repository.getAllUsage().map { usages ->
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        val last7Days = (6 downTo 0).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = format.format(cal.time)
            val dayName = dayFormat.format(cal.time)
            
            val totalMinutes = usages.filter { it.dateString == dateStr }.sumOf { it.usedMinutes }
            DailyStat(dayName, totalMinutes)
        }
        last7Days
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val mostDistracting = repository.getAllUsage().map { usages ->
        val appUsageMap = usages.groupBy { it.packageName }
            .mapValues { entry -> entry.value.sumOf { it.usedMinutes } }
        
        val maxEntry = appUsageMap.maxByOrNull { it.value }
        if (maxEntry != null && maxEntry.value > 0) {
            val appInfo = try {
                val packageManager = application.packageManager
                val info = packageManager.getApplicationInfo(maxEntry.key, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                maxEntry.key
            }
            Pair(appInfo, maxEntry.value)
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

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
