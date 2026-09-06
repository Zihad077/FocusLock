package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.FocusLockApplication
import com.example.presentation.blocking.BlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusLockAccessibilityService : AccessibilityService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var activeAppTimerJob: Job? = null
    private var currentForegroundPackage: String? = null
    
    private val appRepository by lazy {
        (applicationContext as FocusLockApplication).repository
    }
    
    private val usageTracker by lazy {
        UsageTracker(applicationContext, appRepository)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        this.serviceInfo = info
        Log.d("FocusLock", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Skip checking our own app to prevent block loops
            if (packageName == applicationContext.packageName) return
            
            currentForegroundPackage = packageName
            activeAppTimerJob?.cancel()
            
            activeAppTimerJob = serviceScope.launch {
                val limit = appRepository.getLimit(packageName)
                if (limit != null && limit.isEnabled) {
                    checkAndBlock(packageName, limit)
                    
                    // Periodically check if we're still in the foreground and the limit was reached
                    while (currentForegroundPackage == packageName) {
                        kotlinx.coroutines.delay(30000) // check every 30 seconds
                        if (currentForegroundPackage == packageName) {
                            checkAndBlock(packageName, limit)
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkAndBlock(packageName: String, limit: com.example.database.AppLimit) {
        val currentTimeMillis = System.currentTimeMillis()
        
        // Check Temporary Unlocks
        val tempUnlocks = appRepository.getTemporaryUnlocks(packageName)
        val activeUnlock = tempUnlocks.find { 
            it.startTime + (it.durationMinutes * 60 * 1000L) > currentTimeMillis 
        }
        if (activeUnlock != null) {
            Log.d("FocusLock", "App $packageName has active temporary unlock. Allowing.")
            return
        }

        // Clean up expired unlocks
        tempUnlocks.filter { 
            it.startTime + (it.durationMinutes * 60 * 1000L) <= currentTimeMillis 
        }.forEach {
            appRepository.deleteTemporaryUnlock(it.id)
        }
        
        val settings = appRepository.userSettings.first()
        
        // Check Focus Mode
        if (settings.isFocusModeActive) {
            blockApp(limit.appName, packageName, 0, limit.dailyLimitMinutes)
            return
        }

        // Check Schedules
        val calendar = java.util.Calendar.getInstance()
        val currentMinuteOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK).toString()
        
        val schedules = appRepository.getSchedulesForApp(packageName).first()
        for (schedule in schedules) {
            if (schedule.daysOfWeek.contains(currentDayOfWeek)) {
                val isWithinSchedule = if (schedule.startTimeMinuteOfDay <= schedule.endTimeMinuteOfDay) {
                    currentMinuteOfDay in schedule.startTimeMinuteOfDay..schedule.endTimeMinuteOfDay
                } else {
                    // Crosses midnight
                    currentMinuteOfDay >= schedule.startTimeMinuteOfDay || currentMinuteOfDay <= schedule.endTimeMinuteOfDay
                }
                if (isWithinSchedule) {
                    blockApp(limit.appName, packageName, 0, limit.dailyLimitMinutes)
                    return
                }
            }
        }

        // Check Daily Limit
        val usedMillis = usageTracker.updateUsageForPackage(packageName)
        val usedMinutes = (usedMillis / (1000 * 60)).toInt()
        
        if (usedMinutes >= limit.dailyLimitMinutes) {
            Log.d("FocusLock", "App $packageName exceeded limit ($usedMinutes / ${limit.dailyLimitMinutes}). Blocking.")
            blockApp(limit.appName, packageName, usedMinutes, limit.dailyLimitMinutes)
        }
    }

    private fun blockApp(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int) {
        val intent = Intent(this, BlockActivity::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("PACKAGE_NAME", packageName)
            putExtra("USED_MINUTES", usedMinutes)
            putExtra("LIMIT_MINUTES", limitMinutes)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d("FocusLock", "Accessibility Service Interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
