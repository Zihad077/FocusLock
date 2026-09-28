package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.FocusLockApplication
import com.example.database.EscapeAttempt
import com.example.database.effectiveAntiDeleteProtection
import com.example.database.effectiveEscapeAttemptDetection
import com.example.database.effectiveStableLockMode
import com.example.presentation.blocking.BlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusLockAccessibilityService : AccessibilityService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var activeAppTimerJob: Job? = null
    private var currentForegroundPackage: String? = null
    private var lastTamperInterceptTimestamp: Long = 0L

    private val appRepository by lazy {
        (applicationContext as FocusLockApplication).repository
    }

    private val usageTracker by lazy {
        UsageTracker(applicationContext, appRepository)
    }

    private val enforcementEngine by lazy {
        EnforcementEngine(applicationContext, appRepository, usageTracker)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        this.serviceInfo = info
        Log.d("FocusLock", "Accessibility Service Connected and Configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // If our own app is foreground (e.g. BlockActivity or main app)
        if (packageName == applicationContext.packageName) {
            currentForegroundPackage = packageName
            activeAppTimerJob?.cancel()
            return
        }

        // Skip system overlays, status bar, and soft keyboards
        if (packageName == "com.android.systemui" || packageName.contains("inputmethod")) return

        // 1. Anti-Delete & Tamper Protection for System Settings & Package Installers
        if (isSettingsOrInstallerPackage(packageName)) {
            val eventText = buildString {
                append(event.text.joinToString(" "))
                append(" ")
                append(event.contentDescription?.toString() ?: "")
            }
            val windowText = extractActiveWindowText()
            val combinedLower = "$eventText $windowText".lowercase()

            serviceScope.launch {
                val settings = appRepository.userSettings.first()
                if (settings.effectiveAntiDeleteProtection) {
                    val mentionsOurApp = combinedLower.contains("focuslock") || combinedLower.contains("focus lock")
                    val hasTamperKeyword = combinedLower.contains("force stop") ||
                            combinedLower.contains("uninstall") ||
                            combinedLower.contains("disable") ||
                            combinedLower.contains("clear storage") ||
                            combinedLower.contains("clear data") ||
                            combinedLower.contains("do you want to uninstall") ||
                            combinedLower.contains("focuslock app blocker") ||
                            combinedLower.contains("remove permission")

                    if (mentionsOurApp && hasTamperKeyword) {
                        val now = System.currentTimeMillis()
                        if (now - lastTamperInterceptTimestamp > 1500L) {
                            lastTamperInterceptTimestamp = now
                            Log.w("FocusLock", "Anti-Delete Protection intercepted tamper attempt in $packageName")

                            performGlobalAction(GLOBAL_ACTION_BACK)
                            performGlobalAction(GLOBAL_ACTION_HOME)

                            if (settings.effectiveEscapeAttemptDetection) {
                                appRepository.insertEscapeAttempt(
                                    EscapeAttempt(
                                        packageName = packageName,
                                        type = "ANTI_DELETE_BLOCKED: Settings / Uninstall Tamper"
                                    )
                                )
                            }

                            blockApp(
                                appName = "System Settings (Tamper Guard)",
                                packageName = packageName,
                                usedMinutes = 0,
                                limitMinutes = 0,
                                blockReason = "ANTI_DELETE_PROTECTION"
                            )
                        }
                    }
                }
            }
        }

        // For content-changed events outside Settings/Installer, avoid restarting the foreground timer
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            currentForegroundPackage == packageName &&
            activeAppTimerJob?.isActive == true
        ) {
            return
        }

        // If launcher is active, clear tracking and cancel active session timer
        if (isLauncherPackage(packageName)) {
            currentForegroundPackage = packageName
            activeAppTimerJob?.cancel()
            BlockOverlayManager.getInstance(applicationContext).hideOverlay()
            return
        }

        // If already tracking this package and the timer coroutine is active, do not recreate
        if (currentForegroundPackage == packageName && activeAppTimerJob?.isActive == true) {
            return
        }

        currentForegroundPackage = packageName
        activeAppTimerJob?.cancel()

        activeAppTimerJob = serviceScope.launch {
            val settings = appRepository.userSettings.first()
            val limit = appRepository.getLimit(packageName)
            val shouldEvaluate = settings.isFocusModeActive || settings.bedtimeEnabled || (limit != null && limit.isEnabled)
            if (shouldEvaluate) {
                val debounceMs = if (settings.effectiveStableLockMode) 800L else 2500L
                // Initial check (sessionElapsed = 0)
                val blockedImmediately = checkAndBlock(packageName, sessionElapsedMillis = 0L, debounceMs = debounceMs)
                if (blockedImmediately) return@launch

                val sessionStartTime = System.currentTimeMillis()

                while (isActive && currentForegroundPackage == packageName) {
                    delay(1000) // 1-second precision timer
                    if (currentForegroundPackage == packageName) {
                        val sessionElapsed = System.currentTimeMillis() - sessionStartTime
                        val blocked = checkAndBlock(packageName, sessionElapsed, debounceMs = debounceMs)
                        if (blocked) {
                            break
                        }
                    }
                }
            }
        }
    }

    private fun isSettingsOrInstallerPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower == "com.android.settings" ||
                lower.contains("packageinstaller") ||
                lower.contains("permissioncontroller") ||
                lower == "com.miui.securitycenter" ||
                lower == "com.samsung.android.settings" ||
                lower == "com.coloros.safecenter" ||
                lower == "com.oplus.safecenter" ||
                lower == "com.vivo.permissionmanager"
    }

    private fun extractActiveWindowText(): String {
        return try {
            val root = rootInActiveWindow ?: return ""
            val sb = StringBuilder()
            var count = 0
            fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
                if (node == null || depth > 7 || count > 60) return
                count++
                node.text?.let {
                    if (it.isNotBlank()) {
                        sb.append(it).append(' ')
                    }
                }
                node.contentDescription?.let {
                    if (it.isNotBlank()) {
                        sb.append(it).append(' ')
                    }
                }
                val childCount = node.childCount
                for (i in 0 until childCount) {
                    if (count > 60) break
                    traverse(node.getChild(i), depth + 1)
                }
            }
            traverse(root, 0)
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun isLauncherPackage(pkg: String): Boolean {
        if (pkg.contains("launcher", ignoreCase = true) ||
            pkg.contains("home", ignoreCase = true) ||
            pkg == "com.google.android.apps.nexuslauncher" ||
            pkg == "com.sec.android.app.launcher" ||
            pkg == "com.android.systemui" ||
            pkg == "com.android.quickstep"
        ) {
            return true
        }
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.activityInfo?.packageName == pkg) return true
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }

    /**
     * Checks restrictions via centralized EnforcementEngine and triggers block if needed.
     * Returns true if the app was blocked.
     */
    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    private suspend fun checkAndBlock(
        packageName: String,
        sessionElapsedMillis: Long,
        debounceMs: Long = 2000L
    ): Boolean {
        val now = System.currentTimeMillis()
        if (lastBlockedPackage == packageName && (now - lastBlockTimestamp) < debounceMs) {
            return true
        }
        when (val decision = enforcementEngine.evaluate(packageName, sessionElapsedMillis, now)) {
            is EnforcementDecision.Allow -> {
                return false
            }
            is EnforcementDecision.Block -> {
                Log.d("FocusLock", "Accessibility block triggered: ${decision.packageName} due to ${decision.reason}")
                blockApp(
                    appName = decision.appName,
                    packageName = decision.packageName,
                    usedMinutes = decision.usedMinutes,
                    limitMinutes = decision.limitMinutes,
                    blockReason = decision.reason.name
                )
                return true
            }
        }
    }

    private fun blockApp(appName: String, packageName: String, usedMinutes: Int, limitMinutes: Int, blockReason: String = "") {
        Log.d("FocusLock", "Enforcing block on $appName ($packageName) reason: $blockReason")
        lastBlockedPackage = packageName
        lastBlockTimestamp = System.currentTimeMillis()

        // 1. Show immediate system window overlay directly over the restricted app
        try {
            BlockOverlayManager.getInstance(applicationContext)
                .showOverlay(appName, packageName, usedMinutes, limitMinutes, blockReason)
        } catch (e: Exception) {
            Log.e("FocusLock", "Error displaying overlay: ${e.message}")
        }

        // 2. Launch BlockActivity directly over the distracting app
        val intent = Intent(this, BlockActivity::class.java).apply {
            putExtra("APP_NAME", appName)
            putExtra("PACKAGE_NAME", packageName)
            putExtra("USED_MINUTES", usedMinutes)
            putExtra("LIMIT_MINUTES", limitMinutes)
            putExtra("BLOCK_REASON", blockReason)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FocusLock", "Error launching BlockActivity: ${e.message}")
        }

        // 3. Post full-screen alarm notification as bulletproof backup
        try {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.showBlockFullScreenNotification(appName, packageName, usedMinutes, limitMinutes)
        } catch (e: Exception) {
            Log.e("FocusLock", "Error showing block notification: ${e.message}")
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
