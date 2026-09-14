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
    const val NATIVE_BANNER_KEY = "cfa2281fa0e49be9d4a8ee92ae2499d3"
    const val NATIVE_BANNER_SRC = "https://pl28108157.effectivegatecontent.com/cfa2281fa0e49be9d4a8ee92ae2499d3/invoke.js"

    // Adsterra 320x50 Banner config
    const val BANNER_320_50_KEY = "64c8fcf61dbd7590886da12d1b54cce0"
    const val BANNER_320_50_SRC = "https://pl28108204.effectivegatecontent.com/64c8fcf61dbd7590886da12d1b54cce0/invoke.js"

    // Adsterra Social Bar format
    const val SOCIAL_BAR_SRC = "https://pl28108215.effectivegatecontent.com/5f/aa/76/5faa764065eb0090875c74ea6fc95ae8.js"

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
     * Incorporates safe document.write polyfill and ad rendering observer.
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
                    #ad-container {
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
                    // Polyfill document.write to prevent Chromium from blocking post-parse execution
                    (function() {
                        var originalWrite = document.write.bind(document);
                        var originalWriteln = document.writeln.bind(document);
                        function safeAppend(html) {
                            var target = document.getElementById('ad-container') || document.body;
                            var temp = document.createElement('div');
                            temp.innerHTML = html;
                            var scripts = [];
                            var nodes = Array.prototype.slice.call(temp.childNodes);
                            for (var i = 0; i < nodes.length; i++) {
                                if (nodes[i].tagName === 'SCRIPT') {
                                    scripts.push(nodes[i]);
                                } else {
                                    target.appendChild(nodes[i]);
                                }
                            }
                            for (var j = 0; j < scripts.length; j++) {
                                var s = document.createElement('script');
                                var old = scripts[j];
                                for (var a = 0; a < old.attributes.length; a++) {
                                    s.setAttribute(old.attributes[a].name, old.attributes[a].value);
                                }
                                s.text = old.text;
                                target.appendChild(s);
                            }
                        }
                        document.write = function(content) {
                            if (document.readyState === 'loading') {
                                try {
                                    originalWrite(content);
                                    return;
                                } catch(e) {
                                    // fall through to safeAppend
                                }
                            }
                            safeAppend(content);
                        };
                        document.writeln = function(content) {
                            document.write(content + '\n');
                        };
                    })();

                    // Global Adsterra Banner Options
                    window.atOptions = {
                        'key': '$BANNER_320_50_KEY',
                        'format': 'iframe',
                        'height': 50,
                        'width': 320,
                        'params': {}
                    };
                    var atOptions = window.atOptions;

                    // Diagnostic Creative Observer
                    window.addEventListener('load', function() {
                        console.log('[Adsterra Banner] Page loaded, observing ad creative delivery...');
                        setTimeout(function() {
                            var container = document.getElementById('ad-container');
                            var iframes = container ? container.getElementsByTagName('iframe') : [];
                            var imgs = container ? container.getElementsByTagName('img') : [];
                            var links = container ? container.getElementsByTagName('a') : [];
                            if (iframes.length > 0 || imgs.length > 0 || links.length > 0) {
                                console.log('[Adsterra Banner] Real ad creative rendered successfully! Elements found: iframes=' + iframes.length + ', imgs=' + imgs.length);
                            } else {
                                console.warn('[Adsterra Banner] [NO-FILL or PENDING] No active ad creative elements found in container after timeout. Adsterra has no matching campaign for this bid/region.');
                            }
                        }, 4000);
                    });
                </script>
            </head>
            <body>
                <div id="ad-container">
                    <script type="text/javascript" src="$BANNER_320_50_SRC"></script>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Constructs HTML snippet for Adsterra Native Banner.
     * Incorporates safe document.write polyfill and native ad rendering observer.
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
                        overflow-x: hidden;
                    }
                    #container-$NATIVE_BANNER_KEY {
                        width: 100%;
                        max-width: 100%;
                        min-height: 80px;
                        display: block;
                        margin: 0 auto;
                    }
                </style>
                <script type="text/javascript">
                    (function() {
                        var originalWrite = document.write.bind(document);
                        var originalWriteln = document.writeln.bind(document);
                        function safeAppend(html) {
                            var target = document.getElementById('container-$NATIVE_BANNER_KEY') || document.body;
                            var temp = document.createElement('div');
                            temp.innerHTML = html;
                            var scripts = [];
                            var nodes = Array.prototype.slice.call(temp.childNodes);
                            for (var i = 0; i < nodes.length; i++) {
                                if (nodes[i].tagName === 'SCRIPT') {
                                    scripts.push(nodes[i]);
                                } else {
                                    target.appendChild(nodes[i]);
                                }
                            }
                            for (var j = 0; j < scripts.length; j++) {
                                var s = document.createElement('script');
                                var old = scripts[j];
                                for (var a = 0; a < old.attributes.length; a++) {
                                    s.setAttribute(old.attributes[a].name, old.attributes[a].value);
                                }
                                s.text = old.text;
                                target.appendChild(s);
                            }
                        }
                        document.write = function(content) {
                            if (document.readyState === 'loading') {
                                try {
                                    originalWrite(content);
                                    return;
                                } catch(e) {
                                    // fall through to safeAppend
                                }
                            }
                            safeAppend(content);
                        };
                        document.writeln = function(content) {
                            document.write(content + '\n');
                        };
                    })();

                    window.addEventListener('load', function() {
                        console.log('[Adsterra Native] Page loaded, observing native ad insertion...');
                        setTimeout(function() {
                            var container = document.getElementById('container-$NATIVE_BANNER_KEY');
                            var hasContent = container && (container.children.length > 0 || container.innerText.trim().length > 0);
                            if (hasContent) {
                                console.log('[Adsterra Native] Native ad creative rendered with ' + container.children.length + ' elements!');
                            } else {
                                console.warn('[Adsterra Native] [NO-FILL or PENDING] Native banner container is empty. Adsterra has no matching campaign for this bid/region.');
                            }
                        }, 4500);
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
                    console.log('[Adsterra SocialBar] Initializing Social Bar script tag...');
                </script>
            </head>
            <body>
                <script type='text/javascript' src='$SOCIAL_BAR_SRC'></script>
            </body>
            </html>
        """.trimIndent()
    }
}
