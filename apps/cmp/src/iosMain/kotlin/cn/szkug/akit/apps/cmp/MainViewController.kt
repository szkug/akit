package cn.szkug.akit.apps.cmp

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSUserDefaults

fun MainViewController() = ComposeUIViewController {
    val languageCode = currentLanguageCode(NSUserDefaults.standardUserDefaults)
    val direction = if (languageCode != null && isRtlLanguage(languageCode)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        AkitCmpApp()
    }
}