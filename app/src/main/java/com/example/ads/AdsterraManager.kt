package com.example.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adsterra monetization configuration and runtime manager for FocusLock.
 *
 * Exclusively executes the authentic Adsterra ad tags provided for publisher monetization:
 * 1. 320x50 Fixed Mobile Banner (ce907ceee43c8f2cbf521675591e593e)
 * 2. Native Banner (43cfe3dc4791cdf4c02fababba57f14d)
 * 3. Social Bar Format (f4002865e3ad4ae912683730e0522dc8)
 *
 * Zero dummy ads or placeholder posters: Only the real Adsterra revenue codes are executed.
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

    private val _isSocialBarActive = MutableStateFlow(false)
    val isSocialBarActive: StateFlow<Boolean> = _isSocialBarActive.asStateFlow()

    fun canShowSocialBar(isPremium: Boolean, isFocusActive: Boolean = false): Boolean {
        return !isPremium && !isFocusActive
    }

    fun markSocialBarShown() {
        _isSocialBarActive.value = true
    }

    /**
     * Constructs HTML snippet for Adsterra 320x50 Mobile Banner.
     * Pure Adsterra script execution - NO dummy ads or fake posters.
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
                        background-color: transparent;
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
     * Pure Adsterra script execution - NO dummy ads or fake posters.
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
                        background-color: transparent;
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        min-height: 250px;
                        overflow-x: hidden;
                    }
                    #container-$NATIVE_BANNER_KEY {
                        width: 100%;
                        min-height: 250px;
                        margin: 0 auto;
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
     * Pure Adsterra script execution - NO dummy ads or fake posters.
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
