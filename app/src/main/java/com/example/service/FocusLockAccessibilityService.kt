package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.FocusLockApplication
import com.example.presentation.blocking.BlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusLockAccessibilityService : AccessibilityService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private val appRepository by lazy {
        (applicationContext as FocusLockApplication).repository
    }
    
    private val usageTracker by lazy {
        UsageTracker(applicationContext, appRepository)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        this.serviceInfo = info
        Log.d("FocusLock", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Skip checking our own app to prevent block loops
            if (packageName == applicationContext.packageName) return
            
            serviceScope.launch {
                val limit = appRepository.getLimit(packageName)
                if (limit != null && limit.isEnabled) {
                    val settings = appRepository.userSettings.first()
                    val usedMillis = usageTracker.updateUsageForPackage(packageName)
                    val usedMinutes = (usedMillis / (1000 * 60)).toInt()
                    
                    if (settings.isFocusModeActive || usedMinutes >= limit.dailyLimitMinutes) {
                        Log.d("FocusLock", "App $packageName blocked. Focus Mode: ${settings.isFocusModeActive}, Limit exceeded: ${usedMinutes >= limit.dailyLimitMinutes}")
                        val intent = Intent(this@FocusLockAccessibilityService, BlockActivity::class.java).apply {
                            putExtra("APP_NAME", limit.appName)
                            putExtra("PACKAGE_NAME", packageName)
                            putExtra("USED_MINUTES", usedMinutes)
                            putExtra("LIMIT_MINUTES", limit.dailyLimitMinutes)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("FocusLock", "Accessibility Service Interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
