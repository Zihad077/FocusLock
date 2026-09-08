package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.FocusLockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ensures blocking protections, active deep-work focus sessions,
 * and monitoring persist across device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent?.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent?.action != "com.example.service.RESTART_MONITOR"
        ) {
            return
        }

        Log.d(TAG, "Device booted, replaced or restart requested. Restoring FocusLock services.")

        val appContext = context.applicationContext
        val app = appContext as? FocusLockApplication

        // Check focus state and clean up if needed
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (app != null) {
                    val repo = app.repository
                    val settings = repo.userSettings.first()
                    val now = System.currentTimeMillis()

                    if (settings.isFocusModeActive && settings.activeFocusEndTime <= now) {
                        // Focus mode was running before shutdown and has now completed
                        Log.d(TAG, "Focus mode expired during shutdown. Finalizing session.")
                        var newXp = settings.xp + 50
                        var newLevel = settings.level
                        if (newXp >= newLevel * 100) {
                            newXp -= (newLevel * 100)
                            newLevel += 1
                        }

                        repo.insertFocusSession(
                            com.example.database.FocusSession(
                                startTime = now - (25 * 60 * 1000L),
                                durationMinutes = 25,
                                isCompleted = true,
                                mode = "DEEP_FOCUS"
                            )
                        )

                        repo.updateSettings(
                            settings.copy(
                                isFocusModeActive = false,
                                activeFocusEndTime = 0L,
                                xp = newXp,
                                level = newLevel
                            )
                        )
                    }
                }

                // Start Foreground AppMonitorService
                val monitorIntent = Intent(appContext, AppMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(monitorIntent)
                } else {
                    appContext.startService(monitorIntent)
                }
                Log.d(TAG, "AppMonitorService restart dispatched after boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring state after boot", e)
            }
        }
    }
}
