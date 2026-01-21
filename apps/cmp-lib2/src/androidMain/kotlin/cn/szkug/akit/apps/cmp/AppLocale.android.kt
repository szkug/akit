package cn.szkug.akit.apps.cmp

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import java.util.Locale

private const val languagePrefsName = "akit.app.language"
private const val languagePrefsKey = "language_code"

object AndroidAppLanguageStore {
    fun load(context: Context): String? {
        return context.getSharedPreferences(languagePrefsName, Context.MODE_PRIVATE)
            .getString(languagePrefsKey, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun save(context: Context, languageCode: String?) {
        val normalized = languageCode?.trim()?.takeIf { it.isNotEmpty() }
        context.getSharedPreferences(languagePrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(languagePrefsKey, normalized)
            .apply()
    }

    fun wrap(base: Context): Context {
        val languageCode = load(base) ?: return base
        val locale = Locale.forLanguageTag(languageCode)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = if (currentLocales.isEmpty) null else currentLocales.get(0)?.toLanguageTag()
        if (currentTag != languageCode) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun applyAppLanguage(context: Context, languageCode: String?) {
    val locales = if (languageCode.isNullOrBlank()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageCode)
    }
    AppCompatDelegate.setApplicationLocales(locales)
    context.findActivity()?.recreate()
}


private class AndroidAppLanguageManager(private val context: Context): AppLanguageManager {
    override fun getAppLanguage(): String? {
        return AndroidAppLanguageStore.load(context)
    }

    override fun setAppLanguage(languageCode: String?) {
        AndroidAppLanguageStore.save(context, languageCode)
        applyAppLanguage(context, languageCode)
    }
}


@Composable
actual fun rememberAppLanguageManager(): AppLanguageManager {
    val context = LocalContext.current
    return remember(context) {
        AndroidAppLanguageManager(context)
    }
}
