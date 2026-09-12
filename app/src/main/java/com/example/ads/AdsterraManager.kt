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
     */
    fun getBanner320x50Html(isDark: Boolean): String {
        val bgColor = if (isDark) "#0d1520" else "#f0f5fa"
        val textColor = if (isDark) "#88a0ba" else "#5c7080"
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        background-color: transparent;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 50px;
                        width: 100%;
                        overflow: hidden;
                    }
                    #ad-container {
                        width: 320px;
                        height: 50px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        margin: 0 auto;
                    }
                </style>
            </head>
            <body>
                <div id="ad-container">
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
    fun getNativeBannerHtml(isDark: Boolean): String {
        val textColor = if (isDark) "#ffffff" else "#0a192f"
        val subTextColor = if (isDark) "#88a0ba" else "#4a5568"
        val cardBg = if (isDark) "rgba(22, 36, 58, 0.7)" else "rgba(255, 255, 255, 0.85)"
        val borderColor = if (isDark) "rgba(0, 229, 255, 0.25)" else "rgba(0, 119, 214, 0.25)"
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                    body {
                        background-color: transparent;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        align-items: center;
                        width: 100%;
                        overflow: hidden;
                        padding: 2px;
                    }
                    #container-$NATIVE_BANNER_KEY {
                        width: 100%;
                        max-width: 100%;
                        min-height: 90px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                    }
                </style>
            </head>
            <body>
                <div id="container-$NATIVE_BANNER_KEY"></div>
                <script async="async" data-cfasync="false" src="$NATIVE_BANNER_SRC"></script>
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
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; }
                    body { background-color: transparent; width: 100%; height: 100%; overflow: hidden; }
                </style>
            </head>
            <body>
                <script type='text/javascript' src='$SOCIAL_BAR_SRC'></script>
            </body>
            </html>
        """.trimIndent()
    }
}
