package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.FocusLockApplication
import com.example.MainActivity
import com.example.database.AppLimit
import com.example.presentation.blocking.BlockActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Service to actively monitor and restrict access to distracting applications
 * identified in the user-configurable blocklist.
 */
class AppMonitorService : Service() {

    companion object {
        private const val TAG = "AppMonitorService"
        const val CHANNEL_ID = "focus_lock_monitor_channel"
        const val NOTIFICATION_ID = 2002

        fun startService(context: Context) {
            try {
                val intent = Intent(context, AppMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AppMonitorService", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, AppMonitorService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AppMonitorService", e)
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private val appRepository by lazy {
        (applicationContext as FocusLockApplication).repository
    }
    
    private val usageTracker by lazy {
        UsageTracker(applicationContext, appRepository)
    }

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startMonitoringLoop()
        Log.d(TAG, "AppMonitorService created and monitoring started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FocusLock App Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors and restricts distracting apps in your blocklist"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("FocusLock Monitor Active")
            .setContentText("Actively monitoring and enforcing blocklist limits")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (isActive) {
                try {
                    val foregroundPackage = getForegroundPackage(usageStatsManager)
                    if (foregroundPackage != null && foregroundPackage != packageName) {
                        checkAppRestriction(foregroundPackage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during app monitor polling", e)
                }
                delay(1500) // Poll every 1.5 seconds
            }
        }
    }

    private fun getForegroundPackage(usageStatsManager: UsageStatsManager?): String? {
        if (usageStatsManager == null) return null
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 10000, now)
        val event = UsageEvents.Event()
        var topPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                topPackage = event.packageName
            }
        }
        if (topPackage != null) return topPackage

        // Fallback using queryUsageStats
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private suspend fun checkAppRestriction(packageName: String) {
        val limit = appRepository.getLimit(packageName) ?: return
        if (!limit.isEnabled) return

        val now = System.currentTimeMillis()

        // Debounce if already blocked in the last 3 seconds
        if (lastBlockedPackage == packageName && (now - lastBlockTimestamp) < 3000) {
            return
        }

        // Check Temporary Unlocks
        val tempUnlocks = appRepository.getTemporaryUnlocks(packageName)
        val activeUnlock = tempUnlocks.find {
            it.startTime + (it.durationMinutes * 60 * 1000L) > now
        }
        if (activeUnlock != null) {
            return
        }

        // Clean up expired unlocks
        tempUnlocks.filter {
            it.startTime + (it.durationMinutes * 60 * 1000L) <= now
        }.forEach {
            appRepository.deleteTemporaryUnlock(it.id)
        }

        val settings = appRepository.userSettings.first()

        // 1. Focus Mode restriction
        if (settings.isFocusModeActive) {
            triggerBlock(limit.appName, packageName, 0, limit.dailyLimitMinutes)
            return
        }

        // 2. Schedule restriction
        val calendar = Calendar.getInstance()
        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toString()

        val schedules = appRepository.getSchedulesForApp(packageName).first()
        for (schedule in schedules) {
            if (schedule.daysOfWeek.contains(currentDayOfWeek)) {
                val isWithinSchedule = if (schedule.startTimeMinuteOfDay <= schedule.endTimeMinuteOfDay) {
                    currentMinuteOfDay in schedule.startTimeMinuteOfDay..schedule.endTimeMinuteOfDay
                } else {
                    currentMinuteOfDay >= schedule.startTimeMinuteOfDay || currentMinuteOfDay <= schedule.endTimeMinuteOfDay
                }
                if (isWithinSchedule) {
                    triggerBlock(limit.appName, packageName, 0, limit.dailyLimitMinutes)
                    return
                }
            }
        }

        // 3. Daily Limit restriction
        val usedMillis = usageTracker.updateUsageForPackage(packageName)
        val usedMinutes = (usedMillis / (1000 * 60)).toInt()

        if (usedMinutes >= limit.dailyLimitMinutes) {
            Log.d(TAG, "Distracting app $packageName exceeded daily limit ($usedMinutes / ${limit.dailyLimitMinutes} min). Restricting.")
            triggerBlock(limit.appName, packageName, usedMinutes, limit.dailyLimitMinutes)
        }
    }

    private fun triggerBlock(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int) {
        lastBlockedPackage = packageName
        lastBlockTimestamp = System.currentTimeMillis()

        val intent = Intent(this, BlockActivity::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("PACKAGE_NAME", packageName)
            putExtra("USED_MINUTES", usedMinutes)
            putExtra("LIMIT_MINUTES", limitMinutes)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "AppMonitorService destroyed")
    }
}
