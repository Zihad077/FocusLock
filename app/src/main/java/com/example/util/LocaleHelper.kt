package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val regionsSummary: String
)

/**
 * Robust multi-country locale manager ensuring language selections work instantly in Jetpack Compose,
 * survive WebView initialization (which otherwise resets Android resource locales), and persist
 * across app restarts and configuration changes.
 */
object LocaleHelper {
    private const val TAG = "LocaleHelper"
    private const val PREFS_NAME = "focuslock_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    /**
     * Widely used languages spoken across multiple countries around the world.
     */
    val SUPPORTED_LANGUAGES: List<SupportedLanguage> = listOf(
        SupportedLanguage(
            code = "bn",
            nativeName = "বাংলা",
            englishName = "Bengali",
            regionsSummary = "Bangladesh • India"
        ),
        SupportedLanguage(
            code = "en",
            nativeName = "English",
            englishName = "English (Global)",
            regionsSummary = "USA • UK • Canada • Australia • 50+ Countries"
        ),
        SupportedLanguage(
            code = "es",
            nativeName = "Español",
            englishName = "Spanish",
            regionsSummary = "Spain • Mexico • Argentina • 20+ Countries"
        ),
        SupportedLanguage(
            code = "fr",
            nativeName = "Français",
            englishName = "French",
            regionsSummary = "France • Canada • Belgium • 29 Countries"
        ),
        SupportedLanguage(
            code = "ar",
            nativeName = "العربية",
            englishName = "Arabic",
            regionsSummary = "Saudi Arabia • UAE • Egypt • 22+ Countries"
        ),
        SupportedLanguage(
            code = "pt",
            nativeName = "Português",
            englishName = "Portuguese",
            regionsSummary = "Brazil • Portugal • Angola • 9 Countries"
        )
    )

    private val supportedCodes = SUPPORTED_LANGUAGES.map { it.code }.toSet()

    private val _languageFlow = MutableStateFlow("en")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    fun normalizeLanguageCode(code: String?): String {
        val normalized = code?.lowercase(Locale.ROOT)?.substringBefore("-")?.trim() ?: "en"
        return if (normalized in supportedCodes) normalized else "en"
    }

    fun getLanguageDisplayName(code: String?): String {
        val clean = normalizeLanguageCode(code)
        val match = SUPPORTED_LANGUAGES.find { it.code == clean }
        return if (match != null) {
            if (match.code == "en") "English" else "${match.nativeName} (${match.englishName})"
        } else {
            "English"
        }
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LANGUAGE, "en")
        val valid = normalizeLanguageCode(raw)
        if (raw != valid) {
            prefs.edit().putString(KEY_LANGUAGE, valid).apply()
        }
        if (_languageFlow.value != valid) {
            _languageFlow.value = valid
        }
        return valid
    }

    fun findActivity(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx != null) {
            if (ctx is Activity) return ctx
            if (ctx is ContextWrapper) {
                ctx = ctx.baseContext
            } else {
                break
            }
        }
        return null
    }

    fun toLocale(languageCode: String): Locale {
        val clean = normalizeLanguageCode(languageCode)
        return Locale(clean)
    }

    fun applyLocale(context: Context, languageCode: String, recreateActivity: Boolean = false) {
        try {
            val validCode = normalizeLanguageCode(languageCode)

            // 1. Save to SharedPreferences synchronously
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, validCode).commit()

            // 2. Emit to reactive StateFlow so Jetpack Compose updates immediately
            _languageFlow.value = validCode

            val locale = toLocale(validCode)
            Locale.setDefault(locale)

            // 3. Update Configuration on current and application resources
            updateResourcesLocale(context, locale)
            val appContext = context.applicationContext
            if (appContext != null && appContext !== context) {
                updateResourcesLocale(appContext, locale)
            }

            Log.d(TAG, "Locale applied: $validCode")

            // 4. Optional Activity recreation if requested by legacy callers outside Compose
            if (recreateActivity) {
                val activity = findActivity(context)
                activity?.recreate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying locale: ${e.message}", e)
        }
    }

    /**
     * Restores the saved app locale after Android WebView initialization
     * (WebView constructor is known to reset system/app Resources locale to device default).
     */
    fun restoreLocaleAfterWebView(context: Context) {
        try {
            val savedCode = getSavedLanguage(context)
            val locale = toLocale(savedCode)
            Locale.setDefault(locale)
            updateResourcesLocale(context, locale)
            context.applicationContext?.let { updateResourcesLocale(it, locale) }
        } catch (_: Exception) {}
    }

    private fun updateResourcesLocale(context: Context, locale: Locale) {
        val resources = context.resources ?: return
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun wrapContext(base: Context): Context {
        val languageCode = getSavedLanguage(base)
        val locale = toLocale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        base.resources.updateConfiguration(config, base.resources.displayMetrics)

        return base.createConfigurationContext(config)
    }

    /**
     * Creates a ContextWrapper that preserves the underlying Activity/Context reference
     * while serving localized Resources that cannot be overwritten by WebView initialization.
     */
    fun createLocalizedContextWrapper(base: Context, languageCode: String): ContextWrapper {
        val validCode = normalizeLanguageCode(languageCode)
        val locale = toLocale(validCode)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        base.resources.updateConfiguration(config, base.resources.displayMetrics)

        val localizedConfigContext = base.createConfigurationContext(config)
        val localizedResources = localizedConfigContext.resources

        return object : ContextWrapper(base) {
            override fun getResources(): Resources = localizedResources
            override fun getAssets(): AssetManager = localizedResources.assets
        }
    }
}
