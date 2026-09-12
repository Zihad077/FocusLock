package com.example.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import java.util.Locale

/**
 * Robust locale manager ensuring language selections persist across app restarts,
 * process deaths, and configuration changes.
 */
object LocaleHelper {
    private const val TAG = "LocaleHelper"
    private const val PREFS_NAME = "focuslock_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun applyLocale(context: Context, languageCode: String, recreateActivity: Boolean = true) {
        try {
            // 1. Save to SharedPreferences for synchronous cold-start loading
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

            val locale = if (languageCode.contains("-")) {
                val parts = languageCode.split("-")
                Locale(parts[0], parts[1])
            } else {
                Locale(languageCode)
            }
            Locale.setDefault(locale)

            // 2. Android 13+ (Tiramisu) per-app language API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
            }

            // 3. Update Configuration on resources
            val resources = context.resources
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)

            // 4. Also update Application context configuration
            val appContext = context.applicationContext
            if (appContext !== context) {
                val appConfig = Configuration(appContext.resources.configuration)
                appConfig.setLocale(locale)
                appConfig.setLayoutDirection(locale)
                @Suppress("DEPRECATION")
                appContext.resources.updateConfiguration(appConfig, appContext.resources.displayMetrics)
            }

            Log.d(TAG, "Locale successfully applied to: $languageCode")

            // 5. Recreate activity if requested so Compose completely reloads localized strings
            if (recreateActivity && context is Activity) {
                context.recreate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying locale: ${e.message}", e)
        }
    }

    fun wrapContext(base: Context): Context {
        val languageCode = getSavedLanguage(base)
        val locale = if (languageCode.contains("-")) {
            val parts = languageCode.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(languageCode)
        }
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
