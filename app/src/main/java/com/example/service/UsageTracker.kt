package com.example.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.data.AppRepository
import com.example.database.DailyUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageTracker(
    private val context: Context,
    private val repository: AppRepository
) {
    suspend fun updateUsageForPackage(packageName: String): Long = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        
        val appStats = stats?.find { it.packageName == packageName }
        val usedTimeMillis = appStats?.totalTimeInForeground ?: 0L
        val usedMinutes = (usedTimeMillis / (1000 * 60)).toInt()

        val currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val settings = repository.userSettings.first()
        if (settings.lastResetDateString != currentDateString) {
            // It's a new day! Reset daily things
            repository.updateSettings(
                settings.copy(
                    lastResetDateString = currentDateString,
                    emergencyUnlocksRemaining = settings.maxEmergencyUnlocks
                    // Also handle streak logic here if needed
                )
            )
        }
        
        val existingUsage = repository.getUsage(packageName, currentDateString)
        if (existingUsage != null) {
            repository.insertUsage(existingUsage.copy(usedMinutes = usedMinutes))
        } else {
            repository.insertUsage(
                DailyUsage(
                    packageName = packageName,
                    dateString = currentDateString,
                    usedMinutes = usedMinutes
                )
            )
        }
        
        usedTimeMillis
    }
}
