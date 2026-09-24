package com.example.ads

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adsterra monetization configuration and runtime manager for FocusLock.
 *
 * Monetization Formats:
 * 1. Native Banner Ad (4:1 responsive widget)
 * 2. 320x50 Fixed Mobile Banner
 * 3. Social Bar Format (Interactive top/bottom engagement bar with cooldown)
 *
 * Compliance & Safety Rules:
 * - 0 ads for Premium users (`isPremium == true`)
 * - 0 ads during active Focus Mode timer sessions
 * - 0 ads during unlock challenges (Mind, Focus, Typing, PIN)
 * - 0 ads over critical blocking action buttons or interactive controls
 * - 0 ads during onboarding & permission setup
 * - Frequency & cooldown limits for Social Bar to prevent intrusion
 */
object AdsterraManager {

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

    // Social Bar Cooldown Management: Minimum 90 seconds between social bar triggers
    private const val SOCIAL_BAR_COOLDOWN_MS = 90_000L
    private var lastSocialBarDisplayTime = 0L

    private val _isSocialBarActive = MutableStateFlow(false)
    val isSocialBarActive: StateFlow<Boolean> = _isSocialBarActive.asStateFlow()

    /**
     * Checks if Social Bar is eligible to be shown based on premium status and cooldown.
     */
    fun canShowSocialBar(isPremium: Boolean, isFocusActive: Boolean = false): Boolean {
        if (isPremium || isFocusActive) return false
        val now = SystemClock.elapsedRealtime()
        return (now - lastSocialBarDisplayTime) >= SOCIAL_BAR_COOLDOWN_MS
    }

    /**
     * Marks that the Social Bar has been triggered.
     */
    fun markSocialBarShown() {
        lastSocialBarDisplayTime = SystemClock.elapsedRealtime()
    }

    /**
     * Constructs HTML snippet for Adsterra 320x50 Mobile Banner.
     * Executes Adsterra script naturally in the HTML document without document.write interception.
     */
    fun getBanner320x50Html(isDark: Boolean): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        background: transparent;
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                    }
                    #banner-container {
                        width: 320px;
                        height: 50px;
                        min-width: 320px;
                        min-height: 50px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        position: relative;
                        margin: 0 auto;
                    }
                </style>
                <script type="text/javascript">
                    console.log('[Adsterra Banner] Parsing HTML document for 320x50 Banner...');
                    window.addEventListener('DOMContentLoaded', function() {
                        console.log('[Adsterra Banner] DOMContentLoaded: container ready');
                    });
                    window.addEventListener('load', function() {
                        console.log('[Adsterra Banner] Page loaded. Observing creative placement...');
                        var container = document.getElementById('banner-container');
                        if (container && window.MutationObserver) {
                            var observer = new MutationObserver(function() {
                                var iframes = container.getElementsByTagName('iframe');
                                var imgs = container.getElementsByTagName('img');
                                if (iframes.length > 0 || imgs.length > 0) {
                                    console.log('[Adsterra Banner] Ad creative detected! iframes=' + iframes.length + ', imgs=' + imgs.length);
                                }
                            });
                            observer.observe(container, { childList: true, subtree: true });
                        }
                    });
                </script>
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
     * Executes Adsterra script naturally in the HTML document without document.write interception.
     */
    fun getNativeBannerHtml(isDark: Boolean): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                    html, body {
                        background-color: transparent;
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    #container-$NATIVE_BANNER_KEY {
                        width: 100%;
                        max-width: 100%;
                        min-height: 250px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto;
                    }
                    iframe {
                        max-width: 100% !important;
                        min-height: 250px !important;
                        border: none !important;
                        border-radius: 12px;
                        display: block;
                        margin: 0 auto;
                    }
                    img, video {
                        max-width: 100% !important;
                        height: auto;
                        border-radius: 12px;
                        object-fit: contain;
                        display: block;
                        margin: 0 auto;
                    }
                </style>
                <script type="text/javascript">
                    console.log('[Adsterra Native] Parsing HTML document for Native Banner...');
                    window.addEventListener('DOMContentLoaded', function() {
                        var container = document.getElementById('container-$NATIVE_BANNER_KEY');
                        console.log('[Adsterra Native] DOMContentLoaded: container exists=' + (container !== null));
                    });
                    window.addEventListener('load', function() {
                        console.log('[Adsterra Native] Page loaded. Observing native ad insertion...');
                        var container = document.getElementById('container-$NATIVE_BANNER_KEY');
                        if (container && window.MutationObserver) {
                            var observer = new MutationObserver(function() {
                                if (container.children.length > 0 || container.innerText.trim().length > 0) {
                                    console.log('[Adsterra Native] Native ad creative rendered with ' + container.children.length + ' elements!');
                                }
                            });
                            observer.observe(container, { childList: true, subtree: true });
                        }
                    });
                </script>
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
                    * { margin: 0; padding: 0; }
                    html, body { background-color: transparent; width: 100%; height: 100%; overflow: hidden; }
                </style>
                <script type="text/javascript">
                    console.log('[Adsterra SocialBar] Initializing Social Bar script tag: $SOCIAL_BAR_SRC');
                    window.addEventListener('load', function() {
                        console.log('[Adsterra SocialBar] Social Bar container window load event completed.');
                    });
                </script>
            </head>
            <body>
                <script type="text/javascript" src="$SOCIAL_BAR_SRC"></script>
            </body>
            </html>
        """.trimIndent()
    }
}
