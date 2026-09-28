package com.example.ads

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.liquidGlass

/**
 * Liquid Glass 320x50 Fixed Mobile Banner Container for FocusLock.
 *
 * Uses [AdsterraManager]'s tab-scoped [KeepAliveAdWebView] pool so:
 * - All 320x50 banners in the active tab are preloaded immediately and remain loaded/running
 *   in the background even when scrolled outside the LazyColumn viewport.
 * - Switching to another tab removes and destroys the previous tab's banners.
 */
@Composable
fun LiquidGlassAdaptiveBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    slotKey: String = "banner_top"
) {
    if (isPremium) return

    val context = LocalContext.current
    val screenKey = LocalAdScreenKey.current
    val readyScreenKey by AdsterraManager.readyScreenKey.collectAsState()
    val isReadyForCurrentTab = readyScreenKey == screenKey

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                isElevated = false,
                borderWidth = 1.dp
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SPONSORED",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = Color(0xFF7E92AA),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (isReadyForCurrentTab) {
                val webView = remember(screenKey, slotKey) {
                    AdsterraManager.getOrCreateWebView(
                        context = context,
                        screenKey = screenKey,
                        slotKey = slotKey,
                        slotType = AdSlotType.BANNER_320_50
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .width(320.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp)),
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
            } else {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0E1829))
                )
            }
        }
    }
}
