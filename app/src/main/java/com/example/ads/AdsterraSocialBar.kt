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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AdsterraManager.markSocialBarShown()
    }

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
                    Log.d("AdsterraSocial", "[Navigation] isMainFrame=$isMainFrame, hasGesture=$hasUserGesture, url=$url")

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
                        Log.i("AdsterraSocial", "[User Click] Opening external ad destination: $url")
                        return launchExternalUrl(context, url)
                    }

                    // Automated redirects and script navigations remain in WebView
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.i("AdsterraSocial", "[Page Started] url=$url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.i("AdsterraSocial", "[Page Finished] url=$url")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.e("AdsterraSocial", "[Resource Error] code=${error?.errorCode}: ${error?.description} for ${request?.url} (mainFrame=${request?.isForMainFrame})")
                    super.onReceivedError(view, request, error)
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    Log.w("AdsterraSocial", "[HTTP Error] ${errorResponse?.statusCode} [${errorResponse?.reasonPhrase}] for ${request?.url}")
                    super.onReceivedHttpError(view, request, errorResponse)
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    Log.w("AdsterraSocial", "[SSL Warning] $error for ${error?.url}")
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
            loadDataWithBaseURL(AdsterraManager.SOCIAL_BAR_BASE_URL, html, "text/html", "UTF-8", null)
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
            .height(60.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
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
        Log.e("AdsterraSocial", "Error opening link: ${e.message}")
        if (url.startsWith("intent://")) {
            try {
                val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                if (fallbackUrl != null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    return true
                }
            } catch (fallbackE: Exception) {
                Log.e("AdsterraSocial", "Fallback error: ${fallbackE.message}")
            }
        }
    }
    return false
}
