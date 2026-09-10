package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.liquidGlass

/**
 * Liquid Glass 320x50 Fixed Mobile Banner Container for FocusLock.
 *
 * Implements Adsterra's 320x50 Banner in a sleek glass container:
 * - Subtle sponsored header tag
 * - Clean safe WebView lifecycle disposal
 * - External link handling that opens standard browser
 * - Hides completely for Premium users (`isPremium == true`)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiquidGlassAdaptiveBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val isDark = isSystemInDarkTheme()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewInstance?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearHistory()
                    removeAllViews()
                    destroy()
                }
            } catch (e: Exception) {
                // Ignore
            }
            webViewInstance = null
        }
    }

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
            // Subtle attribution tag
            Text(
                text = "SPONSORED ADVERTISEMENT",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = if (isDark) Color(0x8000E5FF) else Color(0x800077D6),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            AndroidView(
                modifier = Modifier
                    .width(320.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewInstance = this
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        ctx.startActivity(intent)
                                        return true
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                return false
                            }
                        }

                        webChromeClient = WebChromeClient()

                        val html = AdsterraManager.getBanner320x50Html(isDark)
                        loadDataWithBaseURL("https://effectivegatecontent.com", html, "text/html", "UTF-8", null)
                    }
                }
            )
        }
    }
}
