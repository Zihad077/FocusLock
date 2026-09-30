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
    val compositeKey = "$screenKey::$slotKey"
    val readySlots by AdsterraManager.readySlots.collectAsState()
    val reloadGen by AdsterraManager.reloadGeneration.collectAsState()
    val isSlotReady = compositeKey in readySlots

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        if (isSlotReady) {
            val webView = remember(compositeKey, reloadGen) {
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
                    webView.onResume()
                    webView
                },
                update = { view ->
                    view.onResume()
                }
            )
        }
    }
}
