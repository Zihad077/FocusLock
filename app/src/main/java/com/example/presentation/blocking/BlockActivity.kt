package com.example.presentation.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appName = intent.getStringExtra("APP_NAME") ?: "Unknown App"
        val usedMinutes = intent.getIntExtra("USED_MINUTES", 0)
        val limitMinutes = intent.getIntExtra("LIMIT_MINUTES", 0)
        val packageName = intent.getStringExtra("PACKAGE_NAME")
        
        setContent {
            var showChallenge by remember { mutableStateOf(false) }

            FocusLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showChallenge) {
                        ChallengeScreen(
                            onChallengeComplete = {
                                packageName?.let {
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
                                                packageName = it,
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
                                val homeIntent = Intent(Intent.ACTION_MAIN)
                                homeIntent.addCategory(Intent.CATEGORY_HOME)
                                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(homeIntent)
                                finish()
                            },
                            onChallengeClick = {
                                showChallenge = true
                            },
                            onEmergencyUnlockClick = {
                                packageName?.let {
                                    lifecycleScope.launch {
                                        val repo = (application as FocusLockApplication).repository
                                        val settings = repo.userSettings.first()
                                        if (settings.emergencyUnlocksRemaining > 0) {
                                            repo.updateSettings(settings.copy(emergencyUnlocksRemaining = settings.emergencyUnlocksRemaining - 1))
                                            repo.insertTemporaryUnlock(
                                                com.example.database.TemporaryUnlock(
                                                    packageName = it,
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
    
    override fun onBackPressed() {
        // Prevent back button from dismissing block screen
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        finish()
    }
}
