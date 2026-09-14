package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
            .height(60.dp)
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
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        mediaPlaybackRequiresUserGesture = false
                        javaScriptCanOpenWindowsAutomatically = true
                        safeBrowsingEnabled = false
                        setSupportMultipleWindows(false)
                    }

                    // Use genuine Chrome mobile user-agent to prevent ad networks from dropping embedded webviews
                    val defaultUa = settings.userAgentString
                    settings.userAgentString = defaultUa.replace("; wv", "").replace("Version/4.0 ", "")

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            Log.d("AdsterraSocial", "shouldOverrideUrlLoading: isForMainFrame=${request?.isForMainFrame}, url=$url")
                            
                            // Let iframe and subframe resources load freely without interception
                            if (request?.isForMainFrame == false) {
                                return false
                            }

                            // Ignore about:blank and internal base url navigation
                            if (url == "about:blank" || url.startsWith("data:") || url.contains("effectivegatecontent.com")) {
                                return false
                            }

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
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("AdsterraSocial", "onPageFinished: $url")
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            Log.e("AdsterraSocial", "WebView error [${error?.errorCode}]: ${error?.description} for ${request?.url}")
                            super.onReceivedError(view, request, error)
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            Log.w("AdsterraSocial", "HTTP error ${errorResponse?.statusCode} [${errorResponse?.reasonPhrase}] for ${request?.url}")
                            super.onReceivedHttpError(view, request, errorResponse)
                        }

                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            Log.w("AdsterraSocial", "SSL error: $error for ${error?.url}")
                            handler?.proceed()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            val level = consoleMessage?.messageLevel()
                            val msg = "[JS $level] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})"
                            when (level) {
                                ConsoleMessage.MessageLevel.ERROR -> Log.e("AdsterraSocial", msg)
                                ConsoleMessage.MessageLevel.WARNING -> Log.w("AdsterraSocial", msg)
                                else -> Log.d("AdsterraSocial", msg)
                            }
                            return true
                        }
                    }

                    val html = AdsterraManager.getSocialBarHtml()
                    loadDataWithBaseURL("https://pl28108215.effectivegatecontent.com/", html, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
