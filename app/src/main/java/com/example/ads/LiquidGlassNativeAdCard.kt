package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.liquidGlass

/**
 * Liquid Glass Styled Adsterra Native Banner Container for FocusLock.
 *
 * Visually pairs with the Obsidian / Glacial theme, featuring:
 * - Liquid glass translucent card backdrop with specular border
 * - Clear "Ad" / "Sponsored" badge for Play Policy compliance
 * - Non-intrusive embedded WebView with retained lifecycle and sandboxed link navigation
 * - Hides completely for Premium users (`isPremium == true`)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiquidGlassNativeAdCard(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val primaryCyan = if (isDark) Color(0xFF00E5FF) else Color(0xFF0077D6)

    // Retain WebView instance to prevent recompositions from reloading or destroying ad creative
    val webView = remember(context) {
        WebView(context).apply {
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

            // Mobile Chrome user-agent ensures ad network scripts execute properly
            val defaultUa = settings.userAgentString
            settings.userAgentString = defaultUa.replace("; wv", "").replace("Version/4.0 ", "")

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    val isMainFrame = request.isForMainFrame
                    val hasUserGesture = request.hasGesture()
                    Log.d("AdsterraNative", "[Navigation] isMainFrame=$isMainFrame, hasGesture=$hasUserGesture, url=$url")

                    // Subframes/iframes must load freely inside WebView
                    if (!isMainFrame) {
                        return false
                    }

                    // Internal navigation / ad network domains should proceed inside WebView
                    if (url == "about:blank" || url.startsWith("data:") || url.startsWith("javascript:") ||
                        url.contains("highrevenueformat.com") || url.contains("profitableratecpmnetwork.com") ||
                        url.contains("effectivegatecontent.com") || url.contains("adsterra.com")) {
                        return false
                    }

                    // Special schemas (market://, intent://) must be opened externally
                    if (url.startsWith("market://") || url.startsWith("intent://")) {
                        return launchExternalUrl(context, url)
                    }

                    // For HTTP/HTTPS: ONLY handle as external navigation if initiated by a genuine user tap/gesture
                    if (hasUserGesture) {
                        Log.i("AdsterraNative", "[User Click] Opening external ad destination: $url")
                        return launchExternalUrl(context, url)
                    }

                    // Automated redirects and script navigations remain in WebView
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.i("AdsterraNative", "[Page Started] url=$url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.i("AdsterraNative", "[Page Finished] url=$url")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.e("AdsterraNative", "[Resource Error] code=${error?.errorCode}: ${error?.description} for ${request?.url} (mainFrame=${request?.isForMainFrame})")
                    super.onReceivedError(view, request, error)
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    Log.w("AdsterraNative", "[HTTP Error] ${errorResponse?.statusCode} [${errorResponse?.reasonPhrase}] for ${request?.url}")
                    super.onReceivedHttpError(view, request, errorResponse)
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    Log.w("AdsterraNative", "[SSL Warning] $error for ${error?.url}")
                    handler?.proceed()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    val level = consoleMessage?.messageLevel()
                    val msg = "[JS $level] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})"
                    when (level) {
                        ConsoleMessage.MessageLevel.ERROR -> Log.e("AdsterraNative", msg)
                        ConsoleMessage.MessageLevel.WARNING -> Log.w("AdsterraNative", msg)
                        else -> Log.d("AdsterraNative", msg)
                    }
                    return true
                }
            }

            val html = AdsterraManager.getNativeBannerHtml(isDark)
            loadDataWithBaseURL(AdsterraManager.NATIVE_BANNER_BASE_URL, html, "text/html", "UTF-8", null)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.onPause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                isElevated = false,
                borderWidth = 0.8.dp
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
                            .background(primaryCyan.copy(alpha = 0.15f))
                            .border(0.5.dp, primaryCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SPONSORED RECOMMENDATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Web-based Native Banner Container
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp)),
                factory = {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.onResume()
                    webView
                },
                update = {
                    // Retain existing ad view without reloading on recomposition
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
        Log.e("AdsterraNative", "Error opening link: ${e.message}")
        if (url.startsWith("intent://")) {
            try {
                val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                if (fallbackUrl != null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    return true
                }
            } catch (fallbackE: Exception) {
                Log.e("AdsterraNative", "Fallback error: ${fallbackE.message}")
            }
        }
    }
    return false
}
