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
 * Foreground Service to continuously monitor and restrict access to distracting applications
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

    private val enforcementEngine by lazy {
        EnforcementEngine(applicationContext, appRepository, usageTracker)
    }

    private var currentForegroundPackage: String? = null
    private var sessionStartTime: Long = 0L
    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L
    private val warnedPackages = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startMonitoringLoop()
        Log.d(TAG, "AppMonitorService active and monitoring")
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
            .setContentTitle("FocusLock Protection Active")
            .setContentText("Actively monitoring and enforcing your blocklist limits")
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
                    val foregroundPackage = detectForegroundPackage(usageStatsManager)
                    if (foregroundPackage != null && 
                        foregroundPackage != packageName && 
                        foregroundPackage != "com.android.systemui" &&
                        !foregroundPackage.contains("inputmethod")) {
                        
                        if (currentForegroundPackage != foregroundPackage) {
                            currentForegroundPackage = foregroundPackage
                            sessionStartTime = System.currentTimeMillis()
                        }
                        
                        val sessionElapsed = System.currentTimeMillis() - sessionStartTime
                        checkAppRestriction(foregroundPackage, sessionElapsed)
                    } else if (foregroundPackage == packageName) {
                        currentForegroundPackage = packageName
                    } else if (foregroundPackage == null || foregroundPackage.contains("launcher") || foregroundPackage.contains("home")) {
                        currentForegroundPackage = null
                        BlockOverlayManager.getInstance(applicationContext).hideOverlay()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during app monitor polling", e)
                }
                delay(800) // Poll every 800ms for snappy response
            }
        }
    }

    private fun detectForegroundPackage(usageStatsManager: UsageStatsManager?): String? {
        if (usageStatsManager == null) return null
        val now = System.currentTimeMillis()
        
        // Check UsageEvents for recent activity lifecycle transitions
        val events = usageStatsManager.queryEvents(now - 30000, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if ((event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                 event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) &&
                 event.timeStamp >= latestTimestamp) {
                latestPackage = event.packageName
                latestTimestamp = event.timeStamp
            }
        }
        if (latestPackage != null) return latestPackage

        // Fallback using queryUsageStats
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 30000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private suspend fun checkAppRestriction(packageName: String, sessionElapsedMillis: Long) {
        val now = System.currentTimeMillis()

        // Debounce if blocked very recently to avoid looping
        if (lastBlockedPackage == packageName && (now - lastBlockTimestamp) < 2500) {
            return
        }

        when (val decision = enforcementEngine.evaluate(packageName, sessionElapsedMillis, now)) {
            is EnforcementDecision.Allow -> {
                // Check if approaching daily limit (>= 80% used and not yet warned today)
                val limit = appRepository.getLimit(packageName)
                if (limit != null && limit.isEnabled && limit.dailyLimitMinutes > 5) {
                    val usedMillis = usageTracker.updateUsageForPackage(packageName)
                    val usedMinutes = ((usedMillis + sessionElapsedMillis) / (1000 * 60)).toInt()
                    val threshold = (limit.dailyLimitMinutes * 0.8).toInt()
                    if (usedMinutes in threshold until limit.dailyLimitMinutes && !warnedPackages.contains(packageName)) {
                        warnedPackages.add(packageName)
                        NotificationHelper(applicationContext).showLimitWarningNotification(
                            appName = limit.appName,
                            usedMinutes = usedMinutes,
                            limitMinutes = limit.dailyLimitMinutes
                        )
                    }
                }
            }
            is EnforcementDecision.Block -> {
                Log.d(TAG, "EnforcementEngine decision: BLOCK ${decision.packageName} due to ${decision.reason}")
                triggerBlock(
                    appName = decision.appName,
                    packageName = decision.packageName,
                    usedMinutes = decision.usedMinutes,
                    limitMinutes = decision.limitMinutes
                )
            }
        }
    }

    private fun triggerBlock(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int) {
        lastBlockedPackage = packageName
        lastBlockTimestamp = System.currentTimeMillis()

        Log.d(TAG, "Triggering block for $appName ($packageName)")

        // 1. Show immediate system window overlay directly over the restricted app
        try {
            BlockOverlayManager.getInstance(applicationContext)
                .showOverlay(appName, packageName, usedMinutes, limitMinutes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display block overlay: ${e.message}")
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
            Log.e(TAG, "Failed to launch BlockActivity: ${e.message}")
        }

        // 3. Post full-screen alarm notification
        try {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.showBlockFullScreenNotification(appName, packageName, usedMinutes, limitMinutes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post block notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "AppMonitorService destroyed")
    }
}
