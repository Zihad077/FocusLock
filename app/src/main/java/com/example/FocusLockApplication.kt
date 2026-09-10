package com.example

import android.app.Application
import android.os.Build
import android.webkit.WebView
import com.example.data.AppRepository
import com.example.database.AppDatabase

class FocusLockApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.focusDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // Ensure chromium webview isolated data directory name in case background services or ads run in isolated contexts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (packageName != processName) {
                try {
                    WebView.setDataDirectorySuffix(processName)
                } catch (_: Exception) {
                }
            }
        }

        // Proactively ensure WebView code cache directories exist to prevent first-launch Chromium enumerator warnings
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            if (!webViewCacheDir.exists()) {
                java.io.File(webViewCacheDir, "js").mkdirs()
                java.io.File(webViewCacheDir, "wasm").mkdirs()
            }
        } catch (_: Exception) {
        }
    }
}
