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
    private val cache = mutableMapOf<String, Pair<Long, Long>>()

    suspend fun updateUsageForPackage(packageName: String): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = cache[packageName]
        // Cache for 60 seconds to prevent battery drain from constant polling
        if (cached != null && (now - cached.first) < 60000) {
            return@withContext cached.second
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        
        // Use queryAndAggregateUsageStats for better accuracy over queryUsageStats
        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, now)
        val appStats = stats[packageName]
        val usedTimeMillis = appStats?.totalTimeInForeground ?: 0L
        val usedMinutes = (usedTimeMillis / (1000 * 60)).toInt()
        
        cache[packageName] = Pair(now, usedTimeMillis)

        val currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val settings = repository.userSettings.first()
        if (settings.lastResetDateString != currentDateString) {
            val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdayCalendar.time)
            
            var newStreak = settings.currentStreak
            if (settings.lastResetDateString == yesterdayDateString) {
                newStreak += 1
            } else if (settings.lastResetDateString.isNotEmpty()) {
                newStreak = 1
            } else {
                newStreak = 1
            }
            val newBestStreak = maxOf(settings.bestStreak, newStreak)

            repository.updateSettings(
                settings.copy(
                    lastResetDateString = currentDateString,
                    emergencyUnlocksRemaining = settings.maxEmergencyUnlocks,
                    currentStreak = newStreak,
                    bestStreak = newBestStreak
                )
            )
            cache.clear() // Invalidate cache on new day
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
