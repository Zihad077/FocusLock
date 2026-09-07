package com.example.presentation.blocking

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.FocusLockApplication
import com.example.presentation.challenge.ChallengeScreen
import com.example.ui.theme.FocusLockTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BlockActivity : ComponentActivity() {

    private var appNameState = mutableStateOf("Distracting App")
    private var packageNameState = mutableStateOf<String?>(null)
    private var usedMinutesState = mutableIntStateOf(0)
    private var limitMinutesState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure screen turns on and shows over keyguard if locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Intercept back gesture / key and redirect safely to Home launcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitToHome()
            }
        })

        extractIntentData(intent)

        setContent {
            var showChallenge by remember { mutableStateOf(false) }

            val appName by appNameState
            val packageName by packageNameState
            val usedMinutes by usedMinutesState
            val limitMinutes by limitMinutesState

            FocusLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showChallenge) {
                        ChallengeScreen(
                            onChallengeComplete = {
                                packageName?.let { pkg ->
                                    lifecycleScope.launch {
                                        val repo = (application as FocusLockApplication).repository
                                        val settings = repo.userSettings.first()
                                        
                                        var newXp = settings.xp + 20
                                        var newLevel = settings.level
                                        if (newXp >= newLevel * 100) {
                                            newXp -= (newLevel * 100)
                                            newLevel += 1
                                        }
                                        
                                        repo.updateSettings(settings.copy(xp = newXp, level = newLevel))
                                        
                                        repo.insertTemporaryUnlock(
                                            com.example.database.TemporaryUnlock(
                                                packageName = pkg,
                                                type = "CHALLENGE",
                                                startTime = System.currentTimeMillis(),
                                                durationMinutes = 5
                                            )
                                        )
                                        finish()
                                    }
                                } ?: finish()
                            },
                            onCancel = { showChallenge = false }
                        )
                    } else {
                        BlockScreen(
                            appName = appName,
                            usedMinutes = usedMinutes,
                            limitMinutes = limitMinutes,
                            onWaitClick = {
                                exitToHome()
                            },
                            onChallengeClick = {
                                showChallenge = true
                            },
                            onEmergencyUnlockClick = {
                                packageName?.let { pkg ->
                                    lifecycleScope.launch {
                                        val repo = (application as FocusLockApplication).repository
                                        val settings = repo.userSettings.first()
                                        if (settings.emergencyUnlocksRemaining > 0) {
                                            repo.updateSettings(settings.copy(emergencyUnlocksRemaining = settings.emergencyUnlocksRemaining - 1))
                                            repo.insertTemporaryUnlock(
                                                com.example.database.TemporaryUnlock(
                                                    packageName = pkg,
                                                    type = "EMERGENCY",
                                                    startTime = System.currentTimeMillis(),
                                                    durationMinutes = 5 // 5 minutes emergency unlock
                                                )
                                            )
                                        }
                                        finish()
                                    }
                                } ?: finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentData(intent)
    }

    private fun extractIntentData(intent: Intent?) {
        if (intent == null) return
        val name = intent.getStringExtra("APP_NAME") ?: "Distracting App"
        val pkg = intent.getStringExtra("PACKAGE_NAME")
        val used = intent.getIntExtra("USED_MINUTES", 0)
        val limit = intent.getIntExtra("LIMIT_MINUTES", 0)

        appNameState.value = name
        packageNameState.value = pkg
        usedMinutesState.intValue = used
        limitMinutesState.intValue = limit
    }

    private fun exitToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
