package com.example.ads

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CompositionLocal identifying the screen/tab that owns an ad slot.
 */
val LocalAdScreenKey = compositionLocalOf { "HOME" }

enum class AdSlotType {
    BANNER_320_50,
    NATIVE_BANNER,
    SOCIAL_BAR
}

data class AdSlotSpec(
    val slotKey: String,
    val slotType: AdSlotType
)

/**
 * Adsterra monetization configuration and tab-scoped WebView lifecycle manager for FocusLock.
 *
 * Behavior:
 * - Preloads all ad slots belonging to the currently active tab/screen with a safe staggered delay
 *   so they stay loaded in memory even when scrolled outside the LazyColumn viewport.
 * - Detaches and destroys all ad WebViews from the previous tab when switching to a different tab,
 *   waiting for Compose exit animations to finish so WebViews are never destroyed while drawing.
 * - Implements [WebViewClient.onRenderProcessGone] returning `true` on all WebViews to prevent
 *   `aw_browser_terminator.cc` renderer crashes.
 */
object AdsterraManager {

    private const val TAG = "AdsterraManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    // Adsterra Native Banner script URL / container key
    const val NATIVE_BANNER_KEY = "43cfe3dc4791cdf4c02fababba57f14d"
    const val NATIVE_BANNER_SRC = "https://pl31271904.profitableratecpmnetwork.com/43cfe3dc4791cdf4c02fababba57f14d/invoke.js"
    const val NATIVE_BANNER_BASE_URL = "https://pl31271904.profitableratecpmnetwork.com/"

    // Adsterra 320x50 Banner config
    const val BANNER_320_50_KEY = "ce907ceee43c8f2cbf521675591e593e"
    const val BANNER_320_50_SRC = "https://www.highrevenueformat.com/ce907ceee43c8f2cbf521675591e593e/invoke.js"
    const val BANNER_320_50_BASE_URL = "https://www.highrevenueformat.com/"

    // Adsterra Social Bar format
    const val SOCIAL_BAR_SRC = "https://pl31271905.profitableratecpmnetwork.com/f4/00/28/f4002865e3ad4ae912683730e0522dc8.js"
    const val SOCIAL_BAR_BASE_URL = "https://pl31271905.profitableratecpmnetwork.com/"

    private val _isSocialBarActive = MutableStateFlow(false)
    val isSocialBarActive: StateFlow<Boolean> = _isSocialBarActive.asStateFlow()

    // Set of composite keys ("SCREEN::SLOT") that are ready to be displayed
    private val _readySlots = MutableStateFlow<Set<String>>(emptySet())
    val readySlots: StateFlow<Set<String>> = _readySlots.asStateFlow()

    // Active WebViews keyed by "SCREEN_KEY::SLOT_KEY"
    private val activeWebViews = mutableMapOf<String, WebView>()
    private var currentScreenKey: String? = null

    fun canShowSocialBar(isPremium: Boolean, isFocusActive: Boolean = false): Boolean {
        return !isPremium && !isFocusActive
    }

    fun markSocialBarShown() {
        _isSocialBarActive.value = true
    }

    private fun getSlotsForScreen(screenKey: String, isFocusActive: Boolean): List<AdSlotSpec> {
        return when (screenKey) {
            "HOME", "APPS", "STATS" -> buildList {
                add(AdSlotSpec("banner_top", AdSlotType.BANNER_320_50))
                add(AdSlotSpec("native_main", AdSlotType.NATIVE_BANNER))
                add(AdSlotSpec("banner_bottom", AdSlotType.BANNER_320_50))
                if (!isFocusActive) {
                    add(AdSlotSpec("social_bar", AdSlotType.SOCIAL_BAR))
                }
            }
            "FOCUS" -> if (isFocusActive) {
                emptyList()
            } else {
                listOf(
                    AdSlotSpec("banner_top", AdSlotType.BANNER_320_50),
                    AdSlotSpec("native_main", AdSlotType.NATIVE_BANNER),
                    AdSlotSpec("social_bar", AdSlotType.SOCIAL_BAR)
                )
            }
            "SETTINGS" -> listOf(
                AdSlotSpec("banner_top", AdSlotType.BANNER_320_50),
                AdSlotSpec("banner_bottom", AdSlotType.BANNER_320_50)
            )
            "GOALS", "INSIGHTS" -> buildList {
                add(AdSlotSpec("native_main", AdSlotType.NATIVE_BANNER))
                add(AdSlotSpec("banner_bottom", AdSlotType.BANNER_320_50))
                if (!isFocusActive) {
                    add(AdSlotSpec("social_bar", AdSlotType.SOCIAL_BAR))
                }
            }
            else -> emptyList()
        }
    }

    /**
     * Switches the active ad tab to [newScreenKey]:
     * 1. Immediately hides previous tab's slots from Compose so AndroidView detaches cleanly.
     * 2. Waits for NavHost's 220ms exit animation to finish, then safely stops and destroys all
     *    WebViews belonging to the previous tab.
     * 3. Preloads all ad slots for [newScreenKey] with a staggered delay so they stay loaded
     *    even if scrolled outside the viewport, without overwhelming the Chromium renderer.
     */
    suspend fun switchActiveTab(
        context: Context,
        newScreenKey: String,
        isPremium: Boolean,
        isFocusActive: Boolean
    ) {
        currentScreenKey = newScreenKey

        if (isPremium || newScreenKey == "NONE" || (newScreenKey == "FOCUS" && isFocusActive)) {
            _readySlots.value = emptySet()
            delay(300)
            destroyAll()
            return
        }

        val prefix = "$newScreenKey::"
        // Keep only slots belonging to newScreenKey in readySlots; hide old screen slots first
        _readySlots.value = _readySlots.value.filter { it.startsWith(prefix) }.toSet()

        // Remove old tab WebViews from map and destroy them AFTER NavHost's 220ms exit transition finishes
        val keysToRemove = activeWebViews.keys.filter { !it.startsWith(prefix) }
        val webViewsToDestroy = keysToRemove.mapNotNull { activeWebViews.remove(it) }

        if (webViewsToDestroy.isNotEmpty()) {
            delay(320)
            webViewsToDestroy.forEach { destroyWebViewSafely(it) }
            Log.d(TAG, "Destroyed ${webViewsToDestroy.size} ad WebViews from previous tab")
        } else {
            delay(280)
        }

        if (currentScreenKey != newScreenKey) return

        // Preload all slots for the active tab one by one (staggered to avoid renderer memory spikes)
        val slots = getSlotsForScreen(newScreenKey, isFocusActive)
        for (spec in slots) {
            if (currentScreenKey != newScreenKey) return
            val compositeKey = "$newScreenKey::${spec.slotKey}"
            getOrCreateWebView(
                context = context,
                screenKey = newScreenKey,
                slotKey = spec.slotKey,
                slotType = spec.slotType
            )
            if (spec.slotType == AdSlotType.SOCIAL_BAR) {
                markSocialBarShown()
            }
            _readySlots.value = _readySlots.value + compositeKey
            delay(180)
        }
    }

    /**
     * Returns the preloaded [WebView] for ([screenKey], [slotKey]), or creates it if needed.
     */
    fun getOrCreateWebView(
        context: Context,
        screenKey: String,
        slotKey: String,
        slotType: AdSlotType
    ): WebView {
        val compositeKey = "$screenKey::$slotKey"
        val existing = activeWebViews[compositeKey]
        if (existing != null) {
            existing.onResume()
            return existing
        }

        val created = createConfiguredAdWebView(context, compositeKey, slotType)
        activeWebViews[compositeKey] = created
        return created
    }

    /**
     * Destroys all active ad WebViews across all tabs.
     */
    fun destroyAll() {
        _readySlots.value = emptySet()
        val all = activeWebViews.values.toList()
        activeWebViews.clear()
        all.forEach { webView ->
            mainHandler.postDelayed({
                destroyWebViewSafely(webView)
            }, 250)
        }
    }

    private fun destroyWebViewSafely(webView: WebView) {
        try {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.onPause()
            webView.webChromeClient = null
            webView.removeAllViews()
            webView.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying WebView: ${e.message}")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredAdWebView(
        context: Context,
        compositeKey: String,
        slotType: AdSlotType
    ): WebView {
        val baseUrl = when (slotType) {
            AdSlotType.BANNER_320_50 -> BANNER_320_50_BASE_URL
            AdSlotType.NATIVE_BANNER -> NATIVE_BANNER_BASE_URL
            AdSlotType.SOCIAL_BAR -> SOCIAL_BAR_BASE_URL
        }

        val html = when (slotType) {
            AdSlotType.BANNER_320_50 -> getBanner320x50Html()
            AdSlotType.NATIVE_BANNER -> getNativeBannerHtml()
            AdSlotType.SOCIAL_BAR -> getSocialBarHtml()
        }

        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(
                if (slotType == AdSlotType.SOCIAL_BAR) AndroidColor.TRANSPARENT
                else AndroidColor.parseColor("#0E1829")
            )
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
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
                    Log.d(TAG, "[$slotType Navigation] main=$isMainFrame gesture=$hasGesture url=$url")

                    if (url.startsWith("market://") || url.startsWith("intent://") || url.contains("play.google.com/store")) {
                        return launchExternalUrl(context, url)
                    }

                    if (url == "about:blank" || url.startsWith("data:") || url.startsWith("javascript:") || isImageAssetUrl(url)) {
                        return false
                    }

                    // Allow initial background ad script and iframe loads without user gesture
                    if (!hasGesture && (url.contains("/invoke.js") || url.contains("/watchnew") ||
                        url.contains("highrevenueformat.com") || url.contains("profitableratecpmnetwork.com") ||
                        url.contains("effectivegatecontent.com") || url.contains("adsterra.com"))) {
                        return false
                    }

                    if (!isMainFrame && !hasGesture) {
                        return false
                    }

                    // Open external browser on user click or top-frame ad redirect
                    if ((hasGesture || isMainFrame) && (url.startsWith("http://") || url.startsWith("https://")) &&
                        !url.contains("/invoke.js") && !url.contains("/watchnew") &&
                        url != baseUrl
                    ) {
                        Log.i(TAG, "[$slotType Click] Launching sponsor website: $url")
                        return launchExternalUrl(context, url)
                    }

                    return false
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    Log.w(TAG, "[$slotType SSL Warning] $error")
                    handler?.proceed()
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.w(TAG, "[$slotType] WebView renderer process exited (didCrash=${detail?.didCrash()}); recovering cleanly")
                    _readySlots.value = _readySlots.value - compositeKey
                    activeWebViews.remove(compositeKey)
                    if (view != null) {
                        mainHandler.post {
                            try {
                                (view.parent as? ViewGroup)?.removeView(view)
                                view.destroy()
                            } catch (_: Exception) {}
                        }
                    }
                    // Return true so Android WebView NEVER crashes the host app process
                    return true
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

                    val hasLaunched = AtomicBoolean(false)

                    // 1. Only use HitTestResult.extra when type is explicitly SRC_ANCHOR_TYPE (text link).
                    // Never use HitTestResult.extra for SRC_IMAGE_ANCHOR_TYPE or IMAGE_TYPE because
                    // Android WebView returns the <img> src URL instead of the <a> href sponsor URL!
                    val hitTestResult = view.hitTestResult
                    if (hitTestResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                        val extraUrl = hitTestResult.extra
                        if (!extraUrl.isNullOrBlank() &&
                            !isImageAssetUrl(extraUrl) &&
                            (extraUrl.startsWith("http://") || extraUrl.startsWith("https://") ||
                                extraUrl.startsWith("market://") || extraUrl.startsWith("intent://"))
                        ) {
                            if (hasLaunched.compareAndSet(false, true)) {
                                launchExternalUrl(context, extraUrl)
                                return false
                            }
                        }
                    }

                    // 2. Extract the enclosing <a> href ("url") rather than <img> src ("src") for image anchors
                    val hrefHandler = Handler(Looper.getMainLooper()) { msg ->
                        val hrefUrl = msg.data?.getString("url")
                        if (!hrefUrl.isNullOrBlank() &&
                            hrefUrl != "about:blank" &&
                            !hrefUrl.startsWith("javascript:") &&
                            !isImageAssetUrl(hrefUrl) &&
                            (hrefUrl.startsWith("http://") || hrefUrl.startsWith("https://") ||
                                hrefUrl.startsWith("market://") || hrefUrl.startsWith("intent://"))
                        ) {
                            if (hasLaunched.compareAndSet(false, true)) {
                                Log.i(TAG, "[$slotType FocusNodeHref] Launching sponsor website: $hrefUrl")
                                launchExternalUrl(context, hrefUrl)
                            }
                        }
                        true
                    }
                    view.requestFocusNodeHref(hrefHandler.obtainMessage())

                    // 3. Attach popup WebView via WebViewTransport to resolve target="_blank" & window.open()
                    // IMPORTANT: Never call v.destroy() synchronously inside onPageStarted/shouldOverrideUrlLoading
                    // as that crashes the Chromium renderer process while JNI is on the stack!
                    val tempWebView = WebView(view.context).apply {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = view.settings.userAgentString
                        }
                        webViewClient = object : WebViewClient() {
                            private fun interceptPopupUrl(v: WebView?, targetUrl: String?): Boolean {
                                if (targetUrl.isNullOrBlank() ||
                                    targetUrl == "about:blank" ||
                                    targetUrl.startsWith("javascript:") ||
                                    targetUrl.startsWith("data:") ||
                                    isImageAssetUrl(targetUrl)
                                ) {
                                    return false
                                }
                                if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://") ||
                                    targetUrl.startsWith("market://") || targetUrl.startsWith("intent://")
                                ) {
                                    if (hasLaunched.compareAndSet(false, true)) {
                                        Log.i(TAG, "[$slotType PopupWebView] Launching sponsor website: $targetUrl")
                                        launchExternalUrl(context, targetUrl)
                                    }
                                    v?.stopLoading()
                                    mainHandler.postDelayed({
                                        try {
                                            v?.destroy()
                                        } catch (_: Exception) {}
                                    }, 500)
                                    return true
                                }
                                return false
                            }

                            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                return interceptPopupUrl(v, req?.url?.toString())
                            }

                            override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(v, url, favicon)
                                interceptPopupUrl(v, url)
                            }

                            override fun onRenderProcessGone(v: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                if (v != null) {
                                    mainHandler.post {
                                        try { v.destroy() } catch (_: Exception) {}
                                    }
                                }
                                return true
                            }
                        }
                    }
                    val transport = resultMsg.obj as? WebView.WebViewTransport
                    if (transport != null) {
                        transport.webView = tempWebView
                        resultMsg.sendToTarget()
                        return true
                    }
                    mainHandler.post {
                        try { tempWebView.destroy() } catch (_: Exception) {}
                    }
                    return false
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    return true
                }
            }

            onResume()
            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    }

    /**
     * Constructs HTML snippet for Adsterra 320x50 Mobile Banner.
     */
    fun getBanner320x50Html(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    html, body {
                        background-color: #0E1829;
                        color: #E2E8F0;
                        margin: 0;
                        padding: 0;
                        width: 320px;
                        height: 50px;
                        overflow: hidden;
                        text-align: center;
                    }
                    #banner-container {
                        width: 320px;
                        height: 50px;
                        margin: 0 auto;
                        padding: 0;
                        overflow: hidden;
                        text-align: center;
                        background-color: #0E1829;
                    }
                    iframe {
                        width: 320px !important;
                        height: 50px !important;
                        border: 0 !important;
                        margin: 0 auto !important;
                        display: block !important;
                    }
                </style>
            </head>
            <body>
                <div id="banner-container">
                    <script type="text/javascript">
                        atOptions = {
                            'key' : '$BANNER_320_50_KEY',
                            'format' : 'iframe',
                            'height' : 50,
                            'width' : 320,
                            'params' : {}
                        };
                    </script>
                    <script type="text/javascript" src="$BANNER_320_50_SRC"></script>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Constructs HTML snippet for Adsterra Native Banner.
     */
    fun getNativeBannerHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    html, body {
                        background-color: #0E1829;
                        color: #E2E8F0;
                        font-family: sans-serif;
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        min-height: 140px;
                        overflow-x: hidden;
                    }
                    #container-$NATIVE_BANNER_KEY {
                        width: 100%;
                        min-height: 140px;
                        margin: 0 auto;
                        background-color: #0E1829;
                    }
                </style>
            </head>
            <body>
                <script async="async" data-cfasync="false" src="$NATIVE_BANNER_SRC"></script>
                <div id="container-$NATIVE_BANNER_KEY"></div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Constructs HTML snippet for Adsterra Social Bar format.
     */
    fun getSocialBarHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    html, body {
                        background-color: transparent;
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                    }
                </style>
            </head>
            <body>
                <script type="text/javascript" src="$SOCIAL_BAR_SRC"></script>
            </body>
            </html>
        """.trimIndent()
    }
}

private fun isImageAssetUrl(url: String): Boolean {
    val cleanUrl = url.substringBefore('?').substringBefore('#').lowercase()
    return cleanUrl.endsWith(".jpg") ||
        cleanUrl.endsWith(".jpeg") ||
        cleanUrl.endsWith(".png") ||
        cleanUrl.endsWith(".gif") ||
        cleanUrl.endsWith(".webp") ||
        cleanUrl.endsWith(".bmp") ||
        cleanUrl.endsWith(".svg") ||
        cleanUrl.endsWith(".ico") ||
        cleanUrl.endsWith(".avif")
}

internal fun launchExternalUrl(context: Context, url: String): Boolean {
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
        Log.e("AdsterraManager", "Error opening link: ${e.message}")
        if (url.startsWith("intent://")) {
            try {
                val fallbackUrl = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                if (fallbackUrl != null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    return true
                }
            } catch (fallbackE: Exception) {
                Log.e("AdsterraManager", "Fallback error: ${fallbackE.message}")
            }
        }
    }
    return false
}
