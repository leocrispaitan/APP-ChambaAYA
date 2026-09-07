package com.proyecto.chambaya

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object IdiomaManager {

    private const val PREFS_NAME = "app_language"
    private const val KEY_LANGUAGE = "language"
    private const val SPANISH = "es"
    private const val ENGLISH = "en"
    private const val QUECHUA = "qu"

    val LANGUAGES = listOf(SPANISH, ENGLISH, QUECHUA)

    fun applySavedLanguage(context: Context) {
        val language = getSavedLanguage(context)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current != language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
    }

    fun createLocalizedContext(context: Context): Context {
        val language = getSavedLanguage(context)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(configuration)
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SPANISH) ?: SPANISH
    }

    fun saveLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }
}
