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
                                        val limit = repo.getLimit(it)
                                        if (limit != null) {
                                            // Grant 5 extra minutes upon challenge completion
                                            repo.insertLimit(limit.copy(dailyLimitMinutes = limit.dailyLimitMinutes + 5))
                                        }
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
                                // Go home
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
                                        val limit = repo.getLimit(it)
                                        if (limit != null) {
                                            // Temporarily disable the limit
                                            repo.insertLimit(limit.copy(isEnabled = false))
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
