package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.navigation.FocusLockApp
import com.example.ui.theme.FocusLockTheme
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val shortcutDestinationState = mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.getSavedLanguage(this)
        enableEdgeToEdge()

        extractShortcutDestination(intent)
        registerDynamicLauncherShortcuts()

        setContent {
            val currentLanguage by LocaleHelper.languageFlow.collectAsState()
            val localizedContext = remember(currentLanguage) {
                LocaleHelper.createLocalizedContextWrapper(this@MainActivity, currentLanguage)
            }
            val localizedConfiguration = remember(currentLanguage, localizedContext) {
                localizedContext.resources.configuration
            }
            val layoutDirection = if (currentLanguage == "ar") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            LaunchedEffect(currentLanguage) {
                registerDynamicLauncherShortcuts(localizedContext)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalLayoutDirection provides layoutDirection
            ) {
                FocusLockTheme(darkTheme = true) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        FocusLockApp(
                            shortcutDestination = shortcutDestinationState.value,
                            onShortcutConsumed = { shortcutDestinationState.value = null }
                        )
                    }
                }
            }
        }

        // Start foreground monitoring service after the initial window frame completes
        window.decorView.post {
            com.example.service.AppMonitorService.startService(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractShortcutDestination(intent)
    }

    private fun extractShortcutDestination(intent: Intent?) {
        val dest = intent?.getStringExtra("shortcut_destination")
        if (!dest.isNullOrBlank()) {
            shortcutDestinationState.value = dest
            intent.removeExtra("shortcut_destination")
        }
    }

    /**
     * Registers launcher long-press shortcuts dynamically (in addition to static xml/shortcuts.xml)
     * so every launcher displays the 4 quick actions with localized labels.
     */
    private fun registerDynamicLauncherShortcuts(resContext: Context = this) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return

            fun createShortcut(
                id: String,
                rank: Int,
                shortLabelRes: Int,
                longLabelRes: Int,
                iconRes: Int,
                destination: String
            ): ShortcutInfo {
                val shortcutIntent = Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_destination", destination)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                return ShortcutInfo.Builder(this, id)
                    .setRank(rank)
                    .setShortLabel(resContext.getString(shortLabelRes))
                    .setLongLabel(resContext.getString(longLabelRes))
                    .setIcon(Icon.createWithResource(this, iconRes))
                    .setIntent(shortcutIntent)
                    .build()
            }

            val shortcuts = listOf(
                createShortcut(
                    id = "shortcut_start_focus",
                    rank = 0,
                    shortLabelRes = R.string.shortcut_focus_short,
                    longLabelRes = R.string.shortcut_focus_long,
                    iconRes = R.drawable.ic_shortcut_focus,
                    destination = "FOCUS"
                ),
                createShortcut(
                    id = "shortcut_block_apps",
                    rank = 1,
                    shortLabelRes = R.string.shortcut_apps_short,
                    longLabelRes = R.string.shortcut_apps_long,
                    iconRes = R.drawable.ic_shortcut_apps,
                    destination = "APPS"
                ),
                createShortcut(
                    id = "shortcut_view_stats",
                    rank = 2,
                    shortLabelRes = R.string.shortcut_stats_short,
                    longLabelRes = R.string.shortcut_stats_long,
                    iconRes = R.drawable.ic_shortcut_stats,
                    destination = "STATS"
                ),
                createShortcut(
                    id = "shortcut_share_badges",
                    rank = 3,
                    shortLabelRes = R.string.shortcut_share_short,
                    longLabelRes = R.string.shortcut_share_long,
                    iconRes = R.drawable.ic_shortcut_share,
                    destination = "SHARE_ACHIEVEMENTS"
                )
            )
            shortcutManager.dynamicShortcuts = shortcuts
        } catch (_: Exception) {
            // Ignore on unsupported launchers
        }
    }
}
