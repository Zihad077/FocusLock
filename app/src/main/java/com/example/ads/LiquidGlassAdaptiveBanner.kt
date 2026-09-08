package com.example.ads

import android.app.Activity
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.liquidGlass
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Liquid Glass Adaptive Banner Ad Component for FocusLock.
 *
 * Automatically sizes itself according to the current window width,
 * handles lifecycle cleanup safely, and hides automatically for Premium users.
 */
@Composable
fun LiquidGlassAdaptiveBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isDark = isSystemInDarkTheme()
    var isLoaded by remember { mutableStateOf(false) }
    var adViewInstance by remember { mutableStateOf<AdView?>(null) }

    val adWidth = configuration.screenWidthDp

    DisposableEffect(Unit) {
        onDispose {
            adViewInstance?.destroy()
            adViewInstance = null
        }
    }

    AnimatedVisibility(
        visible = isLoaded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(18.dp),
                    isElevated = false,
                    borderWidth = 0.6.dp
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Subtle Ad attribution tag
                Text(
                    text = "SPONSORED ADVERTISEMENT",
                    fontSize = 8.sp,
                    color = if (isDark) Color(0x8000E5FF) else Color(0x800077D6),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        val adView = AdView(ctx).apply {
                            setAdUnitId(AdsManager.getBannerAdUnitId())
                            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)
                            setAdSize(adSize)
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    isLoaded = true
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    isLoaded = false
                                }
                            }
                        }
                        adViewInstance = adView
                        adView.loadAd(AdRequest.Builder().build())
                        adView
                    }
                )
            }
        }
    }
}
