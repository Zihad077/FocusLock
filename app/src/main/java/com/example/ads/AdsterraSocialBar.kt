package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
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
            .height(60.dp) // Updated to give it actual height so we can see if it renders
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
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            
                            // Let the social bar scripts and iframes execute without interference
                            val isAdScriptHost = url == "about:blank" || 
                                url.contains("effectivegatecontent.com") || 
                                url.contains("adsterra.com") ||
                                url.contains("pl28108")
                            
                            if (isAdScriptHost && request?.hasGesture() != true) {
                                return false
                            }

                            if (request?.hasGesture() == true || url.startsWith("market://") || url.startsWith("intent://")) {
                                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("market://") || url.startsWith("intent://")) {
                                    try {
                                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        ctx.startActivity(intent)
                                        return true
                                    } catch (e: Exception) {
                                        Log.e("AdsterraSocial", "Error opening link: ${e.message}")
                                        
                                        if (url.startsWith("intent://")) {
                                            try {
                                                val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                                                if (fallbackUrl != null) {
                                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                                    return true
                                                }
                                            } catch (fallbackE: Exception) {
                                                Log.e("AdsterraSocial", "Fallback error: ${fallbackE.message}")
                                            }
                                        }
                                    }
                                }
                            }
                            return false
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            Log.e("AdsterraSocial", "WebView error: ${error?.description}")
                            super.onReceivedError(view, request, error)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("AdsterraSocial", "JS: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    val html = AdsterraManager.getSocialBarHtml()
                    loadDataWithBaseURL("https://effectivegatecontent.com", html, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
