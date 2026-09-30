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
 * Uses 100% real Adsterra ad units in WebViews across all screens:
 * - 1:1 Square Native Banner (NativeBanner_1: 43cfe3dc4791cdf4c02fababba57f14d)
 * - 320x50 Banner (ce907ceee43c8f2cbf521675591e593e)
 * - Social Bar (f4002865e3ad4ae912683730e0522dc8)
 *
 * Automatically recovers and reloads the real Adsterra WebView if the system WebView renderer
 * process ever restarts (`onRenderProcessGone`), without using any dummy/fake ads.
 */
object AdsterraManager {

    private const val TAG = "AdsterraManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    // Adsterra Native Banner script URL / container key (1:1 NativeBanner_1)
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

    // Incremented when a slot recovers from renderer exit so Compose re-attaches the fresh WebView
    private val _reloadGeneration = MutableStateFlow(0)
    val reloadGeneration: StateFlow<Int> = _reloadGeneration.asStateFlow()

    // Active WebViews keyed by "SCREEN_KEY::SLOT_KEY"
    private val activeWebViews = mutableMapOf<String, WebView>()
    private var currentScreenKey: String? = null
    private val isChromiumEngineWarmedUp = AtomicBoolean(false)

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
     * Switches the active ad tab to [newScreenKey] and loads the real Adsterra WebViews.
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
        _readySlots.value = _readySlots.value.filter { it.startsWith(prefix) }.toSet()

        val keysToRemove = activeWebViews.keys.filter { !it.startsWith(prefix) }
        val webViewsToDestroy = keysToRemove.mapNotNull { activeWebViews.remove(it) }

        if (webViewsToDestroy.isNotEmpty()) {
            delay(320)
            webViewsToDestroy.forEach { destroyWebViewSafely(it) }
        } else {
            delay(200)
        }

        if (currentScreenKey != newScreenKey) return

        val slots = getSlotsForScreen(newScreenKey, isFocusActive)
        for ((index, spec) in slots.withIndex()) {
            if (currentScreenKey != newScreenKey) return
            val compositeKey = "$newScreenKey::${spec.slotKey}"
            val isFirstColdWebView = isChromiumEngineWarmedUp.compareAndSet(false, true)
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
            // Give Chromium SimpleCache (HTTP Cache & Code Cache/js) time to complete one-time cold init
            if (isFirstColdWebView && index == 0) {
                delay(500)
            } else {
                delay(240)
            }
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

        val appContext = context.applicationContext
        com.example.FocusLockApplication.configureMesaSoftwareEnvironment()
        (appContext as? com.example.FocusLockApplication)?.ensureChromiumSimpleCacheStructure()

        val created = createConfiguredAdWebView(appContext, screenKey, slotKey, slotType)
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
        screenKey: String,
        slotKey: String,
        slotType: AdSlotType
    ): WebView {
        val compositeKey = "$screenKey::$slotKey"
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
            com.example.util.LocaleHelper.restoreLocaleAfterWebView(context)
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
                databaseEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                loadWithOverviewMode = false
                useWideViewPort = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                mediaPlaybackRequiresUserGesture = true
                javaScriptCanOpenWindowsAutomatically = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = false
                }
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

                    if (url.startsWith("market://") || url.startsWith("intent://") || url.contains("play.google.com/store")) {
                        return launchExternalUrl(context, url)
                    }

                    if (url == "about:blank" || url.startsWith("data:") || url.startsWith("javascript:") || isImageAssetUrl(url)) {
                        return false
                    }

                    if (!hasGesture && (url.contains("/invoke.js") || url.contains("/watchnew") ||
                        url.contains("highrevenueformat.com") || url.contains("profitableratecpmnetwork.com") ||
                        url.contains("effectivegatecontent.com") || url.contains("adsterra.com"))) {
                        return false
                    }

                    if (!isMainFrame && !hasGesture) {
                        return false
                    }

                    if ((hasGesture || isMainFrame) && (url.startsWith("http://") || url.startsWith("https://")) &&
                        !url.contains("/invoke.js") && !url.contains("/watchnew") &&
                        url != baseUrl
                    ) {
                        return launchExternalUrl(context, url)
                    }

                    return false
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed()
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
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
                    // Automatically recreate the real Adsterra WebView if this screen is still active
                    mainHandler.postDelayed({
                        if (currentScreenKey == screenKey) {
                            try {
                                getOrCreateWebView(context, screenKey, slotKey, slotType)
                                _reloadGeneration.value = _reloadGeneration.value + 1
                                _readySlots.value = _readySlots.value + compositeKey
                            } catch (_: Exception) {}
                        }
                    }, 1200)
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
                                launchExternalUrl(context, hrefUrl)
                            }
                        }
                        true
                    }
                    view.requestFocusNodeHref(hrefHandler.obtainMessage())

                    val tempWebView = WebView(view.context).apply {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
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
     * Safety shim injected before ad scripts to disable WebGL/WebGPU/WebRTC hardware probes
     * that crash software/Mesa renderers while keeping all Adsterra ad scripts 100% functional.
     */
    private const val RENDERER_SAFETY_SHIM = """
        <script type="text/javascript">
            (function() {
                try {
                    var origGetContext = HTMLCanvasElement.prototype.getContext;
                    HTMLCanvasElement.prototype.getContext = function(type, attrs) {
                        if (type && (type.indexOf('webgl') !== -1 || type.indexOf('gpu') !== -1)) {
                            return null;
                        }
                        return origGetContext.call(this, type, attrs);
                    };
                    window.RTCPeerConnection = undefined;
                    window.webkitRTCPeerConnection = undefined;
                    window.SharedWorker = undefined;
                } catch (e) {}
            })();
        </script>
    """

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
                $RENDERER_SAFETY_SHIM
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
     * Constructs HTML snippet for Adsterra 1:1 Square Native Banner (NativeBanner_1).
     * Ensures the creative fills the full 1:1 square viewport cleanly without horizontal cropping or height compression.
     */
    fun getNativeBannerHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                $RENDERER_SAFETY_SHIM
                <style>
                    * {
                        box-sizing: border-box;
                    }
                    html, body {
                        background-color: #0E1829;
                        color: #F8FAFC;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        width: 100vw;
                        height: 100vh;
                        min-width: 100%;
                        min-height: 100vw;
                        aspect-ratio: 1 / 1;
                        overflow: hidden;
                        display: flex;
                        align-items: stretch;
                        justify-content: center;
                    }
                    #container-$NATIVE_BANNER_KEY {
                        width: 100%;
                        height: 100%;
                        min-height: 100vw;
                        aspect-ratio: 1 / 1;
                        margin: 0 auto;
                        padding: 4px;
                        background-color: #0E1829;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        align-items: stretch;
                        overflow: hidden;
                    }
                    #container-$NATIVE_BANNER_KEY > div,
                    #container-$NATIVE_BANNER_KEY iframe {
                        width: 100% !important;
                        max-width: 100% !important;
                        height: 100% !important;
                        min-height: calc(100vw - 8px) !important;
                        margin: 0 auto !important;
                        border: 0 !important;
                    }
                    #container-$NATIVE_BANNER_KEY img {
                        max-width: 100% !important;
                        max-height: 72vh !important;
                        width: auto !important;
                        height: auto !important;
                        object-fit: contain !important;
                        display: block !important;
                        margin: 0 auto !important;
                        border-radius: 10px;
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
                $RENDERER_SAFETY_SHIM
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
