package com.example.ads

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Liquid Glass Styled Adsterra Native Banner Container for FocusLock.
 *
 * Uses [AdsterraManager]'s tab-scoped [KeepAliveAdWebView] pool so:
 * - The native ad in the active tab is preloaded immediately and stays loaded/running
 *   even when scrolled outside the LazyColumn viewport.
 * - Switching to another tab removes and destroys the previous tab's native ad.
 */
@Composable
fun LiquidGlassNativeAdCard(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    slotKey: String = "native_main"
) {
    if (isPremium) return

    val context = LocalContext.current
    val screenKey = LocalAdScreenKey.current
    val readyScreenKey by AdsterraManager.readyScreenKey.collectAsState()
    val isReadyForCurrentTab = readyScreenKey == screenKey

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isElevated = false,
                borderWidth = 1.dp
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sponsored Attribution Tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2400E5FF))
                            .border(0.8.dp, Color(0x5500E5FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPONSORED RECOMMENDATION",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = Color(0xFFB0C0D4)
                    )
                }
            }

            if (isReadyForCurrentTab) {
                val webView = remember(screenKey, slotKey) {
                    AdsterraManager.getOrCreateWebView(
                        context = context,
                        screenKey = screenKey,
                        slotKey = slotKey,
                        slotType = AdSlotType.NATIVE_BANNER
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(14.dp)),
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
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0E1829))
                )
            }
        }
    }
}
