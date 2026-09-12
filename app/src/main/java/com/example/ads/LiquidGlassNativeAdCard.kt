package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
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
import androidx.compose.ui.graphics.Brush
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
 * - Non-intrusive embedded WebView with sandboxed external link navigation
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
    var isLoaded by remember { mutableStateOf(false) }
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

                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                // Let the ad network iframe and invoke scripts load freely without interference
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
                                            Log.e("AdsterraNative", "Error opening link: ${e.message}")
                                            
                                            if (url.startsWith("intent://")) {
                                                try {
                                                    val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                                                    if (fallbackUrl != null) {
                                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                                        return true
                                                    }
                                                } catch (fallbackE: Exception) {
                                                    Log.e("AdsterraNative", "Fallback error: ${fallbackE.message}")
                                                }
                                            }
                                        }
                                    }
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoaded = true
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                Log.e("AdsterraNative", "WebView error: ${error?.description}")
                                super.onReceivedError(view, request, error)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.d("AdsterraNative", "JS: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        val html = AdsterraManager.getNativeBannerHtml(isDark)
                        loadDataWithBaseURL("https://effectivegatecontent.com", html, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    // Re-render if theme shifts
                }
            )
        }
    }
}
