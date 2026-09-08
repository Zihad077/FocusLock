package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.AppRepository
import com.example.database.AppLimit
import com.example.database.AppSchedule
import kotlinx.coroutines.flow.first
import java.util.Calendar

class EnforcementEngine(
    private val context: Context,
    private val repository: AppRepository,
    private val usageTracker: UsageTracker
) {
    companion object {
        private const val TAG = "EnforcementEngine"
    }

    suspend fun evaluate(
        packageName: String,
        sessionElapsedMillis: Long = 0L,
        currentTimestamp: Long = System.currentTimeMillis()
    ): EnforcementDecision {
        val limit = repository.getLimit(packageName) ?: return EnforcementDecision.Allow
        if (!limit.isEnabled) return EnforcementDecision.Allow

        // 1. Temporary Unlocks Check
        val tempUnlocks = repository.getTemporaryUnlocks(packageName)
        val activeUnlock = tempUnlocks.find {
            it.startTime + (it.durationMinutes * 60 * 1000L) > currentTimestamp
        }
        if (activeUnlock != null) {
            Log.d(TAG, "Active temporary unlock for $packageName. Allowing access.")
            return EnforcementDecision.Allow
        }

        // Clean up expired unlocks asynchronously
        val expired = tempUnlocks.filter {
            it.startTime + (it.durationMinutes * 60 * 1000L) <= currentTimestamp
        }
        for (item in expired) {
            repository.deleteTemporaryUnlock(item.id)
        }

        val settings = repository.userSettings.first()

        // 2. Focus Mode Check (Survives process death via activeFocusEndTime)
        if (settings.isFocusModeActive) {
            if (settings.activeFocusEndTime == 0L || settings.activeFocusEndTime > currentTimestamp) {
                Log.d(TAG, "Focus mode active. Blocking $packageName.")
                return EnforcementDecision.Block(
                    reason = BlockReason.FOCUS_MODE_ACTIVE,
                    appName = limit.appName,
                    packageName = packageName,
                    usedMinutes = 0,
                    limitMinutes = limit.dailyLimitMinutes
                )
            } else {
                // Focus session expired while app was running/dead
                repository.updateSettings(settings.copy(isFocusModeActive = false, activeFocusEndTime = 0L))
            }
        }

        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 3. Bedtime Protection Check
        if (settings.bedtimeEnabled) {
            val isBedtime = if (settings.bedtimeStartMinuteOfDay <= settings.bedtimeEndMinuteOfDay) {
                currentMinuteOfDay in settings.bedtimeStartMinuteOfDay..settings.bedtimeEndMinuteOfDay
            } else {
                currentMinuteOfDay >= settings.bedtimeStartMinuteOfDay || currentMinuteOfDay <= settings.bedtimeEndMinuteOfDay
            }
            if (isBedtime) {
                Log.d(TAG, "Bedtime protection active. Blocking $packageName.")
                return EnforcementDecision.Block(
                    reason = BlockReason.BEDTIME_ACTIVE,
                    appName = limit.appName,
                    packageName = packageName,
                    usedMinutes = 0,
                    limitMinutes = limit.dailyLimitMinutes
                )
            }
        }

        // 4. Focus Profiles Check
        try {
            val activeProfiles = repository.getActiveFocusProfiles()
            for (profile in activeProfiles) {
                val profileApps = repository.getAppsListForProfile(profile.id)
                if (profileApps.any { it.packageName == packageName }) {
                    Log.d(TAG, "Active focus profile '${profile.name}' blocks $packageName.")
                    return EnforcementDecision.Block(
                        reason = BlockReason.FOCUS_MODE_ACTIVE,
                        appName = limit.appName,
                        packageName = packageName,
                        usedMinutes = 0,
                        limitMinutes = limit.dailyLimitMinutes
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking focus profiles: ${e.message}")
        }

        // 5. App Schedules Check (Time-window & overnight support)
        val schedules = repository.getSchedulesForApp(packageName).first()

        for (schedule in schedules) {
            if (isScheduleActive(schedule, currentMinuteOfDay, calendar)) {
                Log.d(TAG, "Active schedule matched for $packageName. Blocking.")
                return EnforcementDecision.Block(
                    reason = BlockReason.SCHEDULE_ACTIVE,
                    appName = limit.appName,
                    packageName = packageName,
                    usedMinutes = 0,
                    limitMinutes = limit.dailyLimitMinutes
                )
            }
        }

        // 6. Always Blocked (dailyLimitMinutes == 0)
        if (limit.dailyLimitMinutes == 0) {
            Log.d(TAG, "App $packageName is strictly blocked (0m daily limit).")
            return EnforcementDecision.Block(
                reason = BlockReason.ALWAYS_BLOCKED,
                appName = limit.appName,
                packageName = packageName,
                usedMinutes = 0,
                limitMinutes = 0
            )
        }

        // 7. Session Limit Check (Continuous usage limit in single session)
        val sessionLimit = limit.sessionLimitMinutes
        if (sessionLimit != null && sessionLimit > 0) {
            val sessionElapsedMinutes = (sessionElapsedMillis / (1000 * 60)).toInt()
            if (sessionElapsedMinutes >= sessionLimit) {
                Log.d(TAG, "Session limit exceeded for $packageName ($sessionElapsedMinutes / $sessionLimit min).")
                return EnforcementDecision.Block(
                    reason = BlockReason.SESSION_LIMIT_EXCEEDED,
                    appName = limit.appName,
                    packageName = packageName,
                    usedMinutes = sessionElapsedMinutes,
                    limitMinutes = sessionLimit
                )
            }
        }

        // 8. Daily Usage Limit Check (with Weekly Planning day-by-day overrides)
        val daySpecificLimit = when (currentDayOfWeek) {
            Calendar.MONDAY -> limit.mondayLimitMinutes
            Calendar.TUESDAY -> limit.tuesdayLimitMinutes
            Calendar.WEDNESDAY -> limit.wednesdayLimitMinutes
            Calendar.THURSDAY -> limit.thursdayLimitMinutes
            Calendar.FRIDAY -> limit.fridayLimitMinutes
            Calendar.SATURDAY -> limit.saturdayLimitMinutes
            Calendar.SUNDAY -> limit.sundayLimitMinutes
            else -> null
        }
        val effectiveDailyLimit = daySpecificLimit ?: limit.dailyLimitMinutes

        val baseUsedMillis = usageTracker.updateUsageForPackage(packageName)
        val totalUsedMinutes = ((baseUsedMillis + sessionElapsedMillis) / (1000 * 60)).toInt()

        if (totalUsedMinutes >= effectiveDailyLimit) {
            Log.d(TAG, "Daily limit exceeded for $packageName ($totalUsedMinutes / $effectiveDailyLimit min).")
            return EnforcementDecision.Block(
                reason = BlockReason.DAILY_LIMIT_EXCEEDED,
                appName = limit.appName,
                packageName = packageName,
                usedMinutes = totalUsedMinutes,
                limitMinutes = effectiveDailyLimit
            )
        }

        return EnforcementDecision.Allow
    }

    /**
     * Determines if a schedule is active, accounting for normal and overnight windows
     * across midnight (e.g. starting Monday at 22:00 and ending Tuesday at 06:00).
     *
     * Day of week values: 1=Sunday, 2=Monday, ..., 7=Saturday (matching java.util.Calendar).
     */
    fun isScheduleActive(
        schedule: AppSchedule,
        currentMinuteOfDay: Int,
        currentCalendar: Calendar
    ): Boolean {
        val currentDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK).toString()

        return if (schedule.startTimeMinuteOfDay <= schedule.endTimeMinuteOfDay) {
            // Same-day window (e.g. 09:00 to 17:00)
            schedule.daysOfWeek.contains(currentDayOfWeek) &&
                    currentMinuteOfDay in schedule.startTimeMinuteOfDay..schedule.endTimeMinuteOfDay
        } else {
            // Overnight window (e.g. 22:00 to 06:00 next day)
            // Case A: After startTime on a scheduled day (e.g., Monday 23:00)
            val startsToday = schedule.daysOfWeek.contains(currentDayOfWeek) &&
                    currentMinuteOfDay >= schedule.startTimeMinuteOfDay

            // Case B: Before endTime on the morning following a scheduled day (e.g., Tuesday 04:00)
            val prevCal = (currentCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val prevDayOfWeek = prevCal.get(Calendar.DAY_OF_WEEK).toString()
            val startedYesterday = schedule.daysOfWeek.contains(prevDayOfWeek) &&
                    currentMinuteOfDay <= schedule.endTimeMinuteOfDay

            startsToday || startedYesterday
        }
    }
}
