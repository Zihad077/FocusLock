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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FocusLockSemantics
import com.example.ui.theme.SemanticTone
import com.example.ui.theme.liquidGlass

/**
 * Liquid Glass Styled 1:1 Square Adsterra Native Banner Container (NativeBanner_1) for FocusLock.
 *
 * Renders the real Adsterra NativeBanner_1 WebView creative at a strict 1:1 square aspect ratio.
 */
@Composable
fun LiquidGlassNativeAdCard(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    slotKey: String = "native_main"
) {
    if (isPremium) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val screenKey = LocalAdScreenKey.current
    val compositeKey = "$screenKey::$slotKey"
    val readySlots by AdsterraManager.readySlots.collectAsState()
    val reloadGen by AdsterraManager.reloadGeneration.collectAsState()
    val isSlotReady = compositeKey in readySlots
    val semantics = FocusLockSemantics.colors

    Box(
        modifier = modifier
            .testTag("native_banner_1_card")
            .fillMaxWidth()
            .wrapContentHeight()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                isElevated = false,
                borderWidth = 1.dp,
                semanticTone = SemanticTone.NEUTRAL
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(semantics.info.container)
                            .border(0.8.dp, semantics.info.border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = semantics.info.text
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPONSORED RECOMMENDATION",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = semantics.textSecondary
                    )
                }

                Text(
                    text = "NATIVE AD",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = semantics.textMuted
                )
            }

            // Responsive Native Banner Ad Viewport
            BoxWithConstraints(
                modifier = Modifier
                    .testTag("native_banner_viewport")
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1829))
                    .border(0.8.dp, semantics.outlineSubtle, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val targetWidthPx = with(density) { maxWidth.roundToPx() }
                val targetHeightPx = with(density) { 250.dp.roundToPx() }

                if (isSlotReady) {
                    val webView = remember(compositeKey, reloadGen) {
                        AdsterraManager.getOrCreateWebView(
                            context = context,
                            screenKey = screenKey,
                            slotKey = slotKey,
                            slotType = AdSlotType.NATIVE_BANNER
                        )
                    }

                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webView.minimumWidth = targetWidthPx
                            webView.minimumHeight = targetHeightPx
                            webView.onResume()
                            webView
                        },
                        update = { view ->
                            if (view.minimumWidth != targetWidthPx || view.minimumHeight != targetHeightPx) {
                                view.minimumWidth = targetWidthPx
                                view.minimumHeight = targetHeightPx
                                view.requestLayout()
                            }
                            view.onResume()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0E1829)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading Sponsored Content...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = semantics.textMuted
                        )
                    }
                }
            }
        }
    }
}
