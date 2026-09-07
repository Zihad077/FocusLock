package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.FocusLockApplication
import com.example.database.TemporaryUnlock
import com.example.presentation.blocking.BlockScreen
import com.example.presentation.challenge.ChallengeScreen
import com.example.ui.theme.FocusLockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages the floating WindowManager system overlay view that directly blocks apps
 * right above any screen when SYSTEM_ALERT_WINDOW permission is granted.
 */
class BlockOverlayManager private constructor(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "BlockOverlayManager"

        @Volatile
        private var instance: BlockOverlayManager? = null

        fun getInstance(context: Context): BlockOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: BlockOverlayManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var isOverlayShowing = false

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val mainHandler = Handler(Looper.getMainLooper())

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun isShowing(): Boolean = isOverlayShowing

    /**
     * Shows full-screen blocking overlay over the target distracting app
     */
    fun showOverlay(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int) {
        if (!Settings.canDrawOverlays(context)) {
            Log.d(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        mainHandler.post {
            try {
                if (isOverlayShowing && overlayView != null) {
                    // Update content on existing overlay
                    updateOverlayContent(appName, packageName, usedMinutes, limitMinutes)
                    return@post
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                val composeView = ComposeView(context).apply {
                    setViewTreeLifecycleOwner(this@BlockOverlayManager)
                    setViewTreeSavedStateRegistryOwner(this@BlockOverlayManager)
                }

                overlayView = composeView
                updateOverlayContent(appName, packageName, usedMinutes, limitMinutes)

                windowManager.addView(composeView, params)
                isOverlayShowing = true
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                Log.d(TAG, "System overlay successfully displayed for $appName ($packageName)")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying system overlay: ${e.message}", e)
            }
        }
    }

    private fun updateOverlayContent(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int) {
        overlayView?.setContent {
            FocusLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showChallenge by remember { mutableStateOf(false) }

                    if (showChallenge) {
                        ChallengeScreen(
                            onChallengeComplete = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val app = context.applicationContext as FocusLockApplication
                                        val repo = app.repository
                                        val settings = repo.userSettings.first()

                                        var newXp = settings.xp + 20
                                        var newLevel = settings.level
                                        if (newXp >= newLevel * 100) {
                                            newXp -= (newLevel * 100)
                                            newLevel += 1
                                        }

                                        repo.updateSettings(settings.copy(xp = newXp, level = newLevel))
                                        repo.insertTemporaryUnlock(
                                            TemporaryUnlock(
                                                packageName = packageName,
                                                type = "CHALLENGE",
                                                startTime = System.currentTimeMillis(),
                                                durationMinutes = 5
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error saving challenge unlock: ${e.message}")
                                    }
                                }
                                hideOverlay()
                            },
                            onCancel = {
                                showChallenge = false
                            }
                        )
                    } else {
                        BlockScreen(
                            appName = appName,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            onWaitClick = {
                                exitToHome()
                                hideOverlay()
                            },
                            onChallengeClick = {
                                showChallenge = true
                            },
                            onEmergencyUnlockClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val app = context.applicationContext as FocusLockApplication
                                        val repo = app.repository
                                        val settings = repo.userSettings.first()
                                        if (settings.emergencyUnlocksRemaining > 0) {
                                            repo.updateSettings(
                                                settings.copy(emergencyUnlocksRemaining = settings.emergencyUnlocksRemaining - 1)
                                            )
                                            repo.insertTemporaryUnlock(
                                                TemporaryUnlock(
                                                    packageName = packageName,
                                                    type = "EMERGENCY",
                                                    startTime = System.currentTimeMillis(),
                                                    durationMinutes = 5
                                                )
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error saving emergency unlock: ${e.message}")
                                    }
                                }
                                hideOverlay()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun exitToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error redirecting to home: ${e.message}")
        }
    }

    fun hideOverlay() {
        mainHandler.post {
            try {
                if (isOverlayShowing && overlayView != null) {
                    windowManager.removeView(overlayView)
                    overlayView = null
                    isOverlayShowing = false
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                    Log.d(TAG, "System overlay dismissed successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing system overlay: ${e.message}", e)
            }
        }
    }
}
