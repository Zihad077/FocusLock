package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Adsterra Social Bar format container with cooldown awareness.
 *
 * Rules:
 * - Controlled cooldown to protect user focus and prevent intrusive behavior
 * - Never displayed during active Focus Mode, unlock challenges, or onboarding
 * - Completely omitted for Premium users
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdsterraSocialBar(
    isPremium: Boolean,
    isFocusActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isPremium || isFocusActive) return

    val shouldShow = remember { AdsterraManager.canShowSocialBar(isPremium, isFocusActive) }
    if (!shouldShow) return

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        AdsterraManager.markSocialBarShown()
    }

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
            .height(1.dp) // Non-blocking hidden/overlay runner for script execution
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewInstance = this
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
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

                    val html = AdsterraManager.getSocialBarHtml()
                    loadDataWithBaseURL("https://effectivegatecontent.com", html, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
