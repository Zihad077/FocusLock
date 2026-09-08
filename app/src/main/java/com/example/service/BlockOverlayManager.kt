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
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import com.example.database.UserSettings
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
 *
 * Implements anti-bypass protections:
 * - Back button intercepts on custom root FrameLayout and redirects to Home launcher rather than dismissing to the blocked app.
 * - Touches cannot bleed through to underlying apps.
 * - Thread-safe Main-Looper singleton initialization to prevent LifecycleRegistry crashes.
 */
class BlockOverlayManager private constructor(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "BlockOverlayManager"

        @Volatile
        private var instance: BlockOverlayManager? = null

        fun getInstance(context: Context): BlockOverlayManager {
            val existing = instance
            if (existing != null) return existing

            return synchronized(this) {
                instance ?: run {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        BlockOverlayManager(context.applicationContext).also { instance = it }
                    } else {
                        // Safely initialize on Main thread
                        var created: BlockOverlayManager? = null
                        val latch = java.util.concurrent.CountDownLatch(1)
                        Handler(Looper.getMainLooper()).post {
                            try {
                                if (instance == null) {
                                    instance = BlockOverlayManager(context.applicationContext)
                                }
                                created = instance
                            } finally {
                                latch.countDown()
                            }
                        }
                        try {
                            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                        } catch (e: InterruptedException) {
                            Log.e(TAG, "Interrupted while waiting for BlockOverlayManager initialization", e)
                        }
                        created ?: instance ?: BlockOverlayManager(context.applicationContext).also { instance = it }
                    }
                }
            }
        }
    }

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootOverlayLayout: FrameLayout? = null
    private var isOverlayShowing = false

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val appNameState = mutableStateOf("Distracting App")
    private val packageNameState = mutableStateOf("")
    private val usedMinutesState = mutableIntStateOf(0)
    private val limitMinutesState = mutableIntStateOf(0)
    private val emergencyRemainingState = mutableIntStateOf(1)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        // Safe lifecycle attachment on main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        } else {
            mainHandler.post {
                savedStateRegistryController.performRestore(null)
                lifecycleRegistry.currentState = Lifecycle.State.CREATED
            }
        }
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

        // Fetch remaining emergency unlocks
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as FocusLockApplication
                val settings = app.repository.userSettings.first()
                mainHandler.post {
                    emergencyRemainingState.intValue = settings.emergencyUnlocksRemaining
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching settings for overlay: ${e.message}")
            }
        }

        mainHandler.post {
            try {
                appNameState.value = appName
                packageNameState.value = packageName
                usedMinutesState.intValue = usedMinutes
                limitMinutesState.intValue = limitMinutes

                if (isOverlayShowing && rootOverlayLayout != null) {
                    return@post
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                // Anti-bypass window flags:
                // - Covers the full screen
                // - Hardware accelerated
                // - Receives focus so back key and touch cannot bypass to underlying app
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                // Custom FrameLayout that intercepts hardware BACK key events
                val rootLayout = object : FrameLayout(context) {
                    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            // Intercept back key: never allow back to dismiss the lock overlay back to the restricted app!
                            exitToHome()
                            return true
                        }
                        return super.dispatchKeyEvent(event)
                    }
                }.apply {
                    setViewTreeLifecycleOwner(this@BlockOverlayManager)
                    setViewTreeSavedStateRegistryOwner(this@BlockOverlayManager)
                }

                val composeView = ComposeView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setViewTreeLifecycleOwner(this@BlockOverlayManager)
                    setViewTreeSavedStateRegistryOwner(this@BlockOverlayManager)
                }

                setupOverlayContent(composeView)
                rootLayout.addView(composeView)

                rootOverlayLayout = rootLayout
                windowManager.addView(rootLayout, params)
                isOverlayShowing = true
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                Log.d(TAG, "System anti-bypass overlay successfully displayed for $appName ($packageName)")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying system overlay: ${e.message}", e)
            }
        }
    }

    private fun setupOverlayContent(view: ComposeView) {
        view.setContent {
            FocusLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showChallenge by remember { mutableStateOf(false) }

                    val currentAppName by appNameState
                    val currentPackage by packageNameState
                    val currentUsed by usedMinutesState
                    val currentLimit by limitMinutesState
                    val currentEmergencyRemaining by emergencyRemainingState
                    
                    var settings by remember { mutableStateOf(UserSettings()) }
                    
                    LaunchedEffect(Unit) {
                        try {
                            val repo = (context.applicationContext as FocusLockApplication).repository
                            settings = repo.userSettings.first()
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }

                    if (showChallenge) {
                        ChallengeScreen(
                            settings = settings,
                            onChallengeComplete = { challengeType ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val app = context.applicationContext as FocusLockApplication
                                        val repo = app.repository
                                        val currentSettings = repo.userSettings.first()

                                        var newXp = currentSettings.xp + 20
                                        var newLevel = currentSettings.level
                                        if (newXp >= newLevel * 100) {
                                            newXp -= (newLevel * 100)
                                            newLevel += 1
                                        }

                                        repo.updateSettings(currentSettings.copy(xp = newXp, level = newLevel))
                                        repo.insertTemporaryUnlock(
                                            TemporaryUnlock(
                                                packageName = currentPackage,
                                                type = challengeType.name,
                                                startTime = System.currentTimeMillis(),
                                                durationMinutes = currentSettings.tempUnlockDurationMinutes
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error saving challenge unlock: ${e.message}")
                                    }
                                }
                                hideOverlay()
                            },
                            onCancel = {
                                // Returning from challenge goes back to lock screen, NEVER to the blocked app
                                showChallenge = false
                            }
                        )
                    } else {
                        BlockScreen(
                            appName = currentAppName,
                            usedMinutes = currentUsed,
                            limitMinutes = currentLimit,
                            emergencyRemaining = currentEmergencyRemaining,
                            onWaitClick = {
                                // Explicit user action: close to launcher
                                exitToHome()
                                hideOverlay()
                            },
                            onChallengeClick = {
                                showChallenge = true
                            },
                            onEmergencyUnlockClick = {
                                if (currentEmergencyRemaining > 0) {
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
                                                        packageName = currentPackage,
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error redirecting to home: ${e.message}")
        }
    }

    fun hideOverlay() {
        mainHandler.post {
            try {
                if (isOverlayShowing && rootOverlayLayout != null) {
                    windowManager.removeView(rootOverlayLayout)
                    rootOverlayLayout = null
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
