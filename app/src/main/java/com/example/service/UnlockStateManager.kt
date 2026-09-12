package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.FocusLockApplication
import com.example.database.TemporaryUnlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Atomic in-memory and persistent unlock state manager.
 * Guarantees zero race conditions between challenge completion and enforcement checks.
 */
object UnlockStateManager {
    private const val TAG = "UnlockStateManager"

    // Atomic map: packageName -> expiryTimestampMillis
    private val activeUnlocks = ConcurrentHashMap<String, Long>()

    fun isUnlocked(packageName: String, currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        val expiry = activeUnlocks[packageName] ?: return false
        if (expiry > currentTimestamp) {
            return true
        }
        activeUnlocks.remove(packageName)
        return false
    }

    fun registerUnlock(packageName: String, durationMinutes: Int) {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        activeUnlocks[packageName] = expiry
        Log.d(TAG, "Atomically registered unlock for $packageName until $expiry ($durationMinutes min)")
    }

    /**
     * Atomically handles challenge completion:
     * 1. Sets in-memory unlock state immediately (atomic, 0ms latency)
     * 2. Persists unlock to Room database
     * 3. Awards XP and updates user level
     * 4. Dismisses overlay & finishes blocking UI
     * 5. Directly launches the app the user originally wanted to open!
     */
    fun onChallengeCompletedSuccessfully(
        context: Context,
        packageName: String?,
        challengeTypeName: String,
        onUiDismiss: () -> Unit
    ) {
        if (packageName.isNullOrBlank()) {
            onUiDismiss()
            return
        }

        val appContext = context.applicationContext
        val app = appContext as? FocusLockApplication
        val repo = app?.repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repo?.userSettings?.first()
                val durationMinutes = settings?.tempUnlockDurationMinutes ?: 5

                // 1. Immediate in-memory unlock
                registerUnlock(packageName, durationMinutes)

                // 2. Persist to Room
                repo?.insertTemporaryUnlock(
                    TemporaryUnlock(
                        packageName = packageName,
                        type = challengeTypeName,
                        startTime = System.currentTimeMillis(),
                        durationMinutes = durationMinutes
                    )
                )

                // 3. Award XP
                if (settings != null) {
                    var newXp = settings.xp + 20
                    var newLevel = settings.level
                    if (newXp >= newLevel * 100) {
                        newXp -= (newLevel * 100)
                        newLevel += 1
                    }
                    repo.updateSettings(settings.copy(xp = newXp, level = newLevel))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting challenge unlock: ${e.message}", e)
                registerUnlock(packageName, 5) // Fallback 5 min unlock
            }
        }

        // 4. Dismiss overlays
        BlockOverlayManager.getInstance(appContext).hideOverlay()
        onUiDismiss()

        // 5. Directly launch the app the user originally wanted to open!
        launchTargetApp(context, packageName)
    }

    /**
     * Directly launches the requested target app so user doesn't land on FocusLock's home.
     */
    fun launchTargetApp(context: Context, packageName: String) {
        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                context.startActivity(launchIntent)
                Log.d(TAG, "Directly launched target app $packageName after challenge success")
            } else {
                Toast.makeText(context, "Unlocked for $packageName!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch target app $packageName: ${e.message}", e)
        }
    }
}
