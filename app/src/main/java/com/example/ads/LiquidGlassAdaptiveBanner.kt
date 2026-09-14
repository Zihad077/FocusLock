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
                                Log.d("AdsterraAdaptive", "shouldOverrideUrlLoading: isForMainFrame=${request?.isForMainFrame}, url=$url")
                                
                                // Let iframe and subframe resources load freely without interception
                                if (request?.isForMainFrame == false) {
                                    return false
                                }

                                // Ignore about:blank and internal base url navigation
                                if (url == "about:blank" || url.startsWith("data:") || url.contains("effectivegatecontent.com")) {
                                    return false
                                }

                                // User tapped/clicked on the ad banner or clicked an external ad destination
                                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("market://") || url.startsWith("intent://")) {
                                    try {
                                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        ctx.startActivity(intent)
                                        return true
                                    } catch (e: Exception) {
                                        Log.e("AdsterraAdaptive", "Error opening link: ${e.message}")
                                        
                                        if (url.startsWith("intent://")) {
                                            try {
                                                val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                                                if (fallbackUrl != null) {
                                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                                    return true
                                                }
                                            } catch (fallbackE: Exception) {
                                                Log.e("AdsterraAdaptive", "Fallback error: ${fallbackE.message}")
                                            }
                                        }
                                    }
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("AdsterraAdaptive", "onPageFinished: $url")
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                Log.e("AdsterraAdaptive", "WebView error [${error?.errorCode}]: ${error?.description} for ${request?.url}")
                                super.onReceivedError(view, request, error)
                            }

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                Log.w("AdsterraAdaptive", "HTTP error ${errorResponse?.statusCode} [${errorResponse?.reasonPhrase}] for ${request?.url}")
                                super.onReceivedHttpError(view, request, errorResponse)
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                Log.w("AdsterraAdaptive", "SSL error: $error for ${error?.url}")
                                handler?.proceed()
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                val level = consoleMessage?.messageLevel()
                                val msg = "[JS $level] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})"
                                when (level) {
                                    ConsoleMessage.MessageLevel.ERROR -> Log.e("AdsterraAdaptive", msg)
                                    ConsoleMessage.MessageLevel.WARNING -> Log.w("AdsterraAdaptive", msg)
                                    else -> Log.d("AdsterraAdaptive", msg)
                                }
                                return true
                            }
                        }

                        val html = AdsterraManager.getBanner320x50Html(isDark)
                        loadDataWithBaseURL("https://pl28108204.effectivegatecontent.com/", html, "text/html", "UTF-8", null)
                    }
                }
            )
        }
    }
}
