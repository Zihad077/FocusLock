package com.example.ads

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Adsterra Social Bar format container.
 *
 * Uses [AdsterraManager]'s tab-scoped [KeepAliveAdWebView] pool so:
 * - The Social Bar in the active tab is preloaded immediately and stays loaded/running
 *   even when scrolled outside the LazyColumn viewport.
 * - Switching to another tab removes and destroys the previous tab's Social Bar.
 */
@Composable
fun AdsterraSocialBar(
    isPremium: Boolean,
    isFocusActive: Boolean = false,
    modifier: Modifier = Modifier,
    slotKey: String = "social_bar"
) {
    if (isPremium || isFocusActive) return

    val context = LocalContext.current
    val screenKey = LocalAdScreenKey.current
    val readyScreenKey by AdsterraManager.readyScreenKey.collectAsState()
    val isReadyForCurrentTab = readyScreenKey == screenKey

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        if (isReadyForCurrentTab) {
            val webView = remember(screenKey, slotKey) {
                AdsterraManager.getOrCreateWebView(
                    context = context,
                    screenKey = screenKey,
                    slotKey = slotKey,
                    slotType = AdSlotType.SOCIAL_BAR
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.keepRunningInBackground = true
                    webView.onResume()
                    webView.resumeTimers()
                    webView
                },
                update = { view ->
                    view.keepRunningInBackground = true
                    view.onResume()
                }
            )
        }
    }
}
