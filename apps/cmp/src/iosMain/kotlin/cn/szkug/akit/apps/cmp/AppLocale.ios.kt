package cn.szkug.akit.apps.cmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification
import platform.UIKit.UIApplication
import platform.UIKit.UISemanticContentAttributeForceLeftToRight
import platform.UIKit.UISemanticContentAttributeForceRightToLeft
import platform.UIKit.UISemanticContentAttributeUnspecified
import platform.UIKit.UIView
import platform.UIKit.UIWindow

private const val appLanguageKey = "akit.app.language"
private const val appleLanguagesKey = "AppleLanguages"

private class IosAppLanguageManager(
    private val defaults: NSUserDefaults,
) : AppLanguageManager {

    init {
        applyLayoutDirection(loadLanguage())
    }

    override fun getAppLanguage(): String? {
        return loadLanguage()
    }

    override fun setAppLanguage(languageCode: String?) {
        val normalized = languageCode?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == null) {
            defaults.removeObjectForKey(appLanguageKey)
            defaults.removeObjectForKey(appleLanguagesKey)
        } else {
            defaults.setObject(normalized, forKey = appLanguageKey)
            defaults.setObject(listOf(normalized), forKey = appleLanguagesKey)
        }
        defaults.synchronize()
        NSNotificationCenter.defaultCenter.postNotificationName(
            NSUserDefaultsDidChangeNotification,
            null,
        )
        applyLayoutDirection(normalized)
    }

    private fun loadLanguage(): String? {
        return currentLanguageCode(defaults)
    }
}

internal fun currentLanguageCode(defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults): String? {
    val raw = defaults.stringForKey(appLanguageKey)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (raw != null) return raw
    val languages = defaults.objectForKey(appleLanguagesKey) as? List<*>
    val first = languages?.firstOrNull() as? String
    return first?.trim()?.takeIf { it.isNotEmpty() }
}

private fun applyLayoutDirection(languageCode: String?) {
    val attribute = when {
        languageCode.isNullOrBlank() -> UISemanticContentAttributeUnspecified
        isRtlLanguage(languageCode) -> UISemanticContentAttributeForceRightToLeft
        else -> UISemanticContentAttributeForceLeftToRight
    }
    UIView.appearance().semanticContentAttribute = attribute
    val app = UIApplication.sharedApplication
    app.windows.forEach { window ->
        window as UIWindow
        window.semanticContentAttribute = attribute
        window.rootViewController?.view?.semanticContentAttribute = attribute
        window.rootViewController?.view?.setNeedsLayout()
        window.rootViewController?.view?.layoutIfNeeded()
    }
    app.keyWindow?.let { window ->
        window.semanticContentAttribute = attribute
        window.rootViewController?.view?.semanticContentAttribute = attribute
        window.rootViewController?.view?.setNeedsLayout()
        window.rootViewController?.view?.layoutIfNeeded()
    }
}

internal fun isRtlLanguage(languageCode: String): Boolean {
    val base = languageCode.replace('_', '-').lowercase().substringBefore('-')
    return base in setOf("ar", "fa", "he", "ur", "dv", "ku", "ps", "sd", "ug", "yi")
}

@Composable
actual fun rememberAppLanguageManager(): AppLanguageManager {
    return remember {
        IosAppLanguageManager(NSUserDefaults.standardUserDefaults)
    }
}
