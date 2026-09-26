package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
 * Implements publisher's authentic Adsterra 320x50 Banner:
 * - Direct execution of official Adsterra invoke tag (Key: ce907ceee43c8f2cbf521675591e593e)
 * - Zero dummy ads or fallback posters
 * - 1:1 CSS pixel viewport (320x50) so the iframe never shrinks or clips
 * - Subframe-safe WebViewClient & multi-window click handler so ad iframes load cleanly
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiquidGlassAdaptiveBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Retain WebView instance to prevent recompositions from reloading or destroying ad creative
    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(AndroidColor.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                // Keep 1:1 mobile dp viewport so 320x50 banner is never zoomed out to 980px
                loadWithOverviewMode = false
                useWideViewPort = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                safeBrowsingEnabled = false
                setSupportMultipleWindows(true)
                userAgentString = userAgentString
                    .replace("; wv", "")
                    .replace("Version/4.0 ", "")
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    val isMainFrame = request.isForMainFrame
                    val hasGesture = request.hasGesture()
                    Log.d("AdsterraAdaptive", "[Navigation] main=$isMainFrame gesture=$hasGesture url=$url")

                    // Direct launch for Play Store and market links
                    if (url.startsWith("market://") || url.startsWith("intent://") || url.contains("play.google.com/store")) {
                        return launchExternalUrl(context, url)
                    }

                    // CRITICAL: Allow all ad iframes and automatic RTB/DSP subframe redirects to load inside WebView
                    if (!isMainFrame && !hasGesture) {
                        return false
                    }

                    // Internal ad network scripts and base URLs must load internally
                    if (url == "about:blank" || url.startsWith("data:") || url.startsWith("javascript:") ||
                        url.contains("highrevenueformat.com") || url.contains("profitableratecpmnetwork.com") ||
                        url.contains("effectivegatecontent.com") || url.contains("adsterra.com")) {
                        return false
                    }

                    // Genuine user tap on the ad opens external destination while preserving banner
                    if (hasGesture && (url.startsWith("http://") || url.startsWith("https://"))) {
                        Log.i("AdsterraAdaptive", "[User Click] Launching ad destination: $url")
                        return launchExternalUrl(context, url)
                    }

                    return false
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    Log.w("AdsterraAdaptive", "[SSL Warning] $error")
                    handler?.proceed()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    if (view == null || resultMsg == null) return false
                    val hitTestResult = view.hitTestResult
                    val extraUrl = hitTestResult.extra
                    if (!extraUrl.isNullOrBlank() && (extraUrl.startsWith("http://") || extraUrl.startsWith("https://") || extraUrl.startsWith("market://") || extraUrl.startsWith("intent://"))) {
                        launchExternalUrl(context, extraUrl)
                        return false
                    }
                    val tempWebView = WebView(view.context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                val targetUrl = req?.url?.toString()
                                if (!targetUrl.isNullOrBlank()) {
                                    launchExternalUrl(context, targetUrl)
                                }
                                v?.destroy()
                                return true
                            }
                        }
                    }
                    val transport = resultMsg.obj as? WebView.WebViewTransport
                    transport?.webView = tempWebView
                    resultMsg.sendToTarget()
                    return true
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    val level = consoleMessage?.messageLevel()
                    val msg = "[JS $level] ${consoleMessage?.message()}"
                    when (level) {
                        ConsoleMessage.MessageLevel.ERROR -> Log.e("AdsterraAdaptive", msg)
                        ConsoleMessage.MessageLevel.WARNING -> Log.w("AdsterraAdaptive", msg)
                        else -> Log.d("AdsterraAdaptive", msg)
                    }
                    return true
                }
            }

            val html = AdsterraManager.getBanner320x50Html()
            loadDataWithBaseURL(AdsterraManager.BANNER_320_50_BASE_URL, html, "text/html", "UTF-8", null)
        }
    }

    DisposableEffect(webView) {
        webView.onResume()
        onDispose {
            // Keep WebView alive across scroll/recomposition
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlass(
                shape = RoundedCornerShape(16.dp),
                isElevated = false,
                borderWidth = 0.7.dp
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SPONSORED • 320×50 AD",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = if (isDark) Color(0x9900E5FF) else Color(0x990077D6),
                modifier = Modifier.padding(bottom = 3.dp)
            )

            AndroidView(
                modifier = Modifier
                    .width(320.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x18101E2E)),
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

private fun launchExternalUrl(context: Context, url: String): Boolean {
    try {
        if (url.startsWith("intent://")) {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        }
    } catch (e: Exception) {
        Log.e("AdsterraAdaptive", "Error opening link: ${e.message}")
        if (url.startsWith("intent://")) {
            try {
                val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                if (fallbackUrl != null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    return true
                }
            } catch (fallbackE: Exception) {
                Log.e("AdsterraAdaptive", "Fallback error: ${fallbackE.message}")
            }
        }
    }
    return false
}
