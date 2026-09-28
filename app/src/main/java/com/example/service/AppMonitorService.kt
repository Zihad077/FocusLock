package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.FocusLockApplication
import com.example.MainActivity
import com.example.R
import com.example.database.EscapeAttempt
import com.example.database.UsageEvent
import com.example.database.effectiveAutoServiceRecovery
import com.example.database.effectiveEscapeAttemptDetection
import com.example.database.effectiveNotificationProtection
import com.example.database.effectivePermissionProtection
import com.example.database.effectiveStableLockMode
import com.example.database.isFocusActiveNow
import com.example.presentation.blocking.BlockActivity
import com.example.util.PermissionHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service to continuously monitor and restrict access to distracting applications
 * identified in the user-configurable blocklist, as well as enforce Escape Preventions.
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

    // Tracks previously granted permissions to reliably detect revocations
    private var previousGrantedPermissions: Set<String>? = null
    private var lastPermissionAlertTimestamp: Long = 0L

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

        val largeIcon = try {
            BitmapFactory.decodeResource(resources, R.drawable.ic_custom_logo)
        } catch (e: Exception) {
            null
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .apply { largeIcon?.let { setLargeIcon(it) } }
            .setSubText("Active Shield")
            .setColor(0xFF24DFEC.toInt())
            .setContentTitle("FocusLock • Guarding Your Peace 🛡️")
            .setContentText("Distraction shield is locked in and protecting your daily focus limits.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            var loopCount = 0
            while (isActive) {
                try {
                    loopCount++
                    if (loopCount % 8 == 1) { // Every ~6.4 seconds
                        checkPermissionsAndEnforceProtections()
                    }

                    val foregroundPackage = detectForegroundPackage(usageStatsManager)
                    if (foregroundPackage != null &&
                        foregroundPackage != packageName &&
                        foregroundPackage != "com.android.systemui" &&
                        !foregroundPackage.contains("inputmethod")
                    ) {

                        if (currentForegroundPackage != foregroundPackage) {
                            if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
                                val endTime = System.currentTimeMillis()
                                val elapsedMillis = endTime - sessionStartTime
                                if (elapsedMillis >= 15000) {
                                    val durationMinutes = maxOf(1, (elapsedMillis / 60000).toInt())
                                    val event = UsageEvent(
                                        packageName = currentForegroundPackage!!,
                                        startTime = sessionStartTime,
                                        endTime = endTime,
                                        durationMinutes = durationMinutes,
                                        dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    )
                                    appRepository.insertUsageEvent(event)
                                }
                            }
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

    private suspend fun checkPermissionsAndEnforceProtections() {
        val settings = appRepository.userSettings.first()
        val context = applicationContext

        // 1. Enforce Notification Protection (Do Not Disturb) while Focus Mode is active
        if (settings.isFocusActiveNow && settings.effectiveNotificationProtection) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm?.isNotificationPolicyAccessGranted == true &&
                    nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
                ) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enforcing DND during focus session", e)
            }
        }

        // 2. Permission Protection Check
        val currentGranted = mutableSetOf<String>()
        val missingPermissions = mutableListOf<String>()

        if (PermissionHelper.hasUsageAccess(context)) {
            currentGranted.add("Usage Access")
        } else {
            missingPermissions.add("Usage Access")
        }

        if (PermissionHelper.hasOverlayPermission(context)) {
            currentGranted.add("Display Over Other Apps")
        } else {
            missingPermissions.add("Display Over Other Apps")
        }

        if (PermissionHelper.hasAccessibilityPermission(context)) {
            currentGranted.add("Accessibility Service")
        } else {
            missingPermissions.add("Accessibility Service")
        }

        if (settings.effectiveNotificationProtection) {
            if (PermissionHelper.hasNotificationPolicyAccess(context)) {
                currentGranted.add("Do Not Disturb Access")
            } else {
                missingPermissions.add("Do Not Disturb Access")
            }
        }

        val prevGranted = previousGrantedPermissions
        previousGrantedPermissions = currentGranted

        if (!settings.effectivePermissionProtection) return

        val newlyRevoked = if (prevGranted != null) {
            prevGranted - currentGranted
        } else {
            emptySet()
        }

        val now = System.currentTimeMillis()
        val shouldAlertForActiveFocus = settings.isFocusActiveNow &&
                missingPermissions.isNotEmpty() &&
                (now - lastPermissionAlertTimestamp > 5 * 60 * 1000L)

        if (newlyRevoked.isNotEmpty() || shouldAlertForActiveFocus) {
            lastPermissionAlertTimestamp = now
            val revokedNames = if (newlyRevoked.isNotEmpty()) {
                newlyRevoked.joinToString()
            } else {
                missingPermissions.joinToString()
            }

            if (settings.effectiveEscapeAttemptDetection) {
                appRepository.insertEscapeAttempt(
                    EscapeAttempt(
                        packageName = "com.android.settings",
                        type = "PERMISSION_REVOKED: $revokedNames"
                    )
                )
            }

            // Show high-priority notification to restore permissions
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                val largeIcon = try {
                    BitmapFactory.decodeResource(context.resources, R.drawable.ic_custom_logo)
                } catch (e: Exception) {
                    null
                }

                val alertTitle = "Heads up! Your Focus Shield needs you 🛡️"
                val alertBody = "System permission ($revokedNames) was turned off. Tap to lock your shield back in 🔒"
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .apply { largeIcon?.let { setLargeIcon(it) } }
                    .setSubText("FocusLock • Tamper Guard")
                    .setColor(0xFFFF5252.toInt())
                    .setContentTitle(alertTitle)
                    .setContentText(alertBody)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle(alertTitle)
                            .bigText(alertBody)
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(2003, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show permission warning notification", e)
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
            @Suppress("DEPRECATION")
            val isForeground = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            if (isForeground && event.timeStamp >= latestTimestamp) {
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
        val settings = appRepository.userSettings.first()
        val debounceMs = if (settings.effectiveStableLockMode) 800L else 2500L

        // Debounce if blocked very recently to avoid looping
        if (lastBlockedPackage == packageName && (now - lastBlockTimestamp) < debounceMs) {
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
                    limitMinutes = decision.limitMinutes,
                    blockReason = decision.reason.name
                )
            }
        }
    }

    private fun triggerBlock(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int, blockReason: String = "") {
        lastBlockedPackage = packageName
        lastBlockTimestamp = System.currentTimeMillis()

        Log.d(TAG, "Triggering block for $appName ($packageName) reason: $blockReason")

        // 1. Show immediate system window overlay directly over the restricted app
        try {
            BlockOverlayManager.getInstance(applicationContext)
                .showOverlay(appName, packageName, usedMinutes, limitMinutes, blockReason)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display block overlay: ${e.message}")
        }

        // 2. Launch BlockActivity directly over the distracting app
        val intent = Intent(this, BlockActivity::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("PACKAGE_NAME", packageName)
            putExtra("USED_MINUTES", usedMinutes)
            putExtra("LIMIT_MINUTES", limitMinutes)
            putExtra("BLOCK_REASON", blockReason)
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "AppMonitorService onTaskRemoved (swiped from recents)")
        runBlocking {
            try {
                handleServiceInterrupt("TASK_REMOVED_FROM_RECENTS")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling task removal", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "AppMonitorService destroyed")
        scheduleServiceRecoveryBroadcast()
    }

    private suspend fun handleServiceInterrupt(reason: String) {
        val settings = appRepository.userSettings.first()
        if (settings.effectiveEscapeAttemptDetection) {
            appRepository.insertEscapeAttempt(
                EscapeAttempt(
                    packageName = "com.android.systemui",
                    type = reason
                )
            )
        }
        if (settings.effectiveAutoServiceRecovery || settings.effectiveStableLockMode) {
            scheduleServiceRecoveryBroadcast()
        }
    }

    private fun scheduleServiceRecoveryBroadcast() {
        try {
            val restartIntent = Intent(applicationContext, BootReceiver::class.java).apply {
                action = "com.example.service.RESTART_MONITOR"
            }
            sendBroadcast(restartIntent)

            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                9901,
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1500L,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule service recovery broadcast", e)
        }
    }
}
