package cn.szkug.akit.apps.cmp

import androidx.compose.runtime.Composable
interface AppLanguageManager {
    fun getAppLanguage(): String?
    fun setAppLanguage(languageCode: String?)
}

@Composable
expect fun rememberAppLanguageManager(): AppLanguageManager
