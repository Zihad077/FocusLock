package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdsManager
import com.example.database.UserSettings
import com.example.ui.theme.FocusLockTheme
import com.example.navigation.FocusLockApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request UMP Privacy Consent & initialize AdMob SDK
        AdsManager.requestConsentAndInit(this)

        // Start foreground monitoring service for blocklist enforcement
        com.example.service.AppMonitorService.startService(this)

        setContent {
            val app = application as FocusLockApplication
            val settings by app.repository.userSettings.collectAsStateWithLifecycle(initialValue = UserSettings())
            
            val isDark = when (settings.theme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            FocusLockTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FocusLockApp()
                }
            }
        }
    }
}
