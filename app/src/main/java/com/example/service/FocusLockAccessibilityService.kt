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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        this.serviceInfo = info
        Log.d("FocusLock", "Accessibility Service Connected and Configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        
        // If our own app is foreground (e.g. BlockActivity or main app)
        if (packageName == applicationContext.packageName) {
            currentForegroundPackage = packageName
            activeAppTimerJob?.cancel()
            return
        }

        // Skip system overlays, status bar, and soft keyboards
        if (packageName == "com.android.systemui" || packageName.contains("inputmethod")) return

        // If launcher is active, clear tracking and cancel active session timer
        if (isLauncherPackage(packageName)) {
            currentForegroundPackage = packageName
            activeAppTimerJob?.cancel()
            BlockOverlayManager.getInstance(applicationContext).hideOverlay()
            return
        }
        
        // If already tracking this package and the timer coroutine is active, do not recreate
        if (currentForegroundPackage == packageName && activeAppTimerJob?.isActive == true) {
            return
        }

        currentForegroundPackage = packageName
        activeAppTimerJob?.cancel()
        
        activeAppTimerJob = serviceScope.launch {
            val limit = appRepository.getLimit(packageName)
            if (limit != null && limit.isEnabled) {
                // If Always Block (0m limit) or Focus Mode / Active Schedule, block immediately
                val blockedImmediately = checkAndBlock(packageName, limit, sessionElapsedMillis = 0L)
                if (blockedImmediately) return@launch

                // If daily limit configured, track real-time session seconds
                val sessionStartTime = System.currentTimeMillis()
                val baseUsedMillis = usageTracker.updateUsageForPackage(packageName)

                while (isActive && currentForegroundPackage == packageName) {
                    delay(1000) // 1-second precision timer
                    if (currentForegroundPackage == packageName) {
                        val sessionElapsed = System.currentTimeMillis() - sessionStartTime
                        val totalMinutes = ((baseUsedMillis + sessionElapsed) / (1000 * 60)).toInt()

                        if (totalMinutes >= limit.dailyLimitMinutes) {
                            checkAndBlock(packageName, limit, sessionElapsed)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun isLauncherPackage(pkg: String): Boolean {
        if (pkg.contains("launcher", ignoreCase = true) ||
            pkg.contains("home", ignoreCase = true) ||
            pkg == "com.google.android.apps.nexuslauncher" ||
            pkg == "com.sec.android.app.launcher" ||
            pkg == "com.android.systemui" ||
            pkg == "com.android.quickstep"
        ) {
            return true
        }
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.activityInfo?.packageName == pkg) return true
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }

    /**
     * Checks restrictions and triggers block if limits, focus mode, or schedules apply.
     * Returns true if the app was blocked.
     */
    private suspend fun checkAndBlock(
        packageName: String, 
        limit: com.example.database.AppLimit,
        sessionElapsedMillis: Long
    ): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        
        // 1. Check Temporary Unlocks
        val tempUnlocks = appRepository.getTemporaryUnlocks(packageName)
        val activeUnlock = tempUnlocks.find { 
            it.startTime + (it.durationMinutes * 60 * 1000L) > currentTimeMillis 
        }
        if (activeUnlock != null) {
            Log.d("FocusLock", "App $packageName has active temporary unlock. Allowing.")
            return false
        }

        // Clean up expired unlocks
        tempUnlocks.filter { 
            it.startTime + (it.durationMinutes * 60 * 1000L) <= currentTimeMillis 
        }.forEach {
            appRepository.deleteTemporaryUnlock(it.id)
        }
        
        val settings = appRepository.userSettings.first()
        
        // 2. Check Focus Mode
        if (settings.isFocusModeActive) {
            Log.d("FocusLock", "Focus Mode Active. Blocking $packageName.")
            blockApp(limit.appName, packageName, 0, limit.dailyLimitMinutes)
            return true
        }

        // 3. Check Schedules
        val calendar = java.util.Calendar.getInstance()
        val currentMinuteOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK).toString()
        
        val schedules = appRepository.getSchedulesForApp(packageName).first()
        for (schedule in schedules) {
            if (schedule.daysOfWeek.contains(currentDayOfWeek)) {
                val isWithinSchedule = if (schedule.startTimeMinuteOfDay <= schedule.endTimeMinuteOfDay) {
                    currentMinuteOfDay in schedule.startTimeMinuteOfDay..schedule.endTimeMinuteOfDay
                } else {
                    currentMinuteOfDay >= schedule.startTimeMinuteOfDay || currentMinuteOfDay <= schedule.endTimeMinuteOfDay
                }
                if (isWithinSchedule) {
                    Log.d("FocusLock", "Active schedule matched. Blocking $packageName.")
                    blockApp(limit.appName, packageName, 0, limit.dailyLimitMinutes)
                    return true
                }
            }
        }

        // 4. Check Daily Limit
        // If dailyLimitMinutes == 0, it means "Always Blocked"
        if (limit.dailyLimitMinutes == 0) {
            Log.d("FocusLock", "App $packageName is configured as Always Block (0m). Blocking immediately.")
            blockApp(limit.appName, packageName, 0, 0)
            return true
        }

        val baseUsedMillis = usageTracker.updateUsageForPackage(packageName)
        val totalUsedMinutes = ((baseUsedMillis + sessionElapsedMillis) / (1000 * 60)).toInt()
        
        if (totalUsedMinutes >= limit.dailyLimitMinutes) {
            Log.d("FocusLock", "App $packageName exceeded limit ($totalUsedMinutes / ${limit.dailyLimitMinutes}). Blocking.")
            blockApp(limit.appName, packageName, totalUsedMinutes, limit.dailyLimitMinutes)
            return true
        }

        return false
    }

    private fun blockApp(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int) {
        Log.d("FocusLock", "Enforcing block on $appName ($packageName)")

        // 1. Show immediate system window overlay directly over the restricted app
        try {
            BlockOverlayManager.getInstance(applicationContext)
                .showOverlay(appName, packageName, usedMinutes, limitMinutes)
        } catch (e: Exception) {
            Log.e("FocusLock", "Error displaying overlay: ${e.message}")
        }

        // 2. Launch BlockActivity directly over the distracting app
        val intent = Intent(this, BlockActivity::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("PACKAGE_NAME", packageName)
            putExtra("USED_MINUTES", usedMinutes)
            putExtra("LIMIT_MINUTES", limitMinutes)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FocusLock", "Error launching BlockActivity: ${e.message}")
        }

        // 3. Post full-screen alarm notification as bulletproof backup
        try {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.showBlockFullScreenNotification(appName, packageName, usedMinutes, limitMinutes)
        } catch (e: Exception) {
            Log.e("FocusLock", "Error showing block notification: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d("FocusLock", "Accessibility Service Interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
