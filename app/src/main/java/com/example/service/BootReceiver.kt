package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.FocusLockApplication
import com.example.database.FocusSession
import com.example.database.effectiveAutoServiceRecovery
import com.example.database.effectiveStableLockMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ensures blocking protections, active deep-work focus sessions,
 * and monitoring persist across device reboot and unexpected process terminations.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.example.service.RESTART_MONITOR"
        ) {
            return
        }

        Log.d(TAG, "Received $action. Evaluating Escape Prevention & restoring FocusLock services.")

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val app = appContext as? FocusLockApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var shouldStartMonitor = true
                if (app != null) {
                    val repo = app.repository
                    val settings = repo.userSettings.first()
                    val now = System.currentTimeMillis()

                    if (settings.isFocusModeActive && settings.activeFocusEndTime in 1..now) {
                        // Focus mode was running before shutdown and has now completed
                        Log.d(TAG, "Focus mode expired during shutdown. Finalizing session.")
                        var newXp = settings.xp + 50
                        var newLevel = settings.level
                        if (newXp >= newLevel * 100) {
                            newXp -= (newLevel * 100)
                            newLevel += 1
                        }

                        repo.insertFocusSession(
                            FocusSession(
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

                    // Respect Stable Lock Mode and Automatic Service Recovery flags
                    shouldStartMonitor = if (action == "com.example.service.RESTART_MONITOR") {
                        settings.effectiveAutoServiceRecovery || settings.effectiveStableLockMode
                    } else {
                        settings.effectiveStableLockMode || settings.effectiveAutoServiceRecovery
                    }
                }

                if (shouldStartMonitor) {
                    val monitorIntent = Intent(appContext, AppMonitorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appContext.startForegroundService(monitorIntent)
                    } else {
                        appContext.startService(monitorIntent)
                    }
                    Log.d(TAG, "AppMonitorService restart dispatched ($action).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring state in BootReceiver", e)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Exception) {}
            }
        }
    }
}
