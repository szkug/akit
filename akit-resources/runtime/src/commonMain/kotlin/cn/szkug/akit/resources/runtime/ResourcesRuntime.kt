package cn.szkug.akit.resources.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

expect class ResourceId

data class ResourceLocale(val languageCode: String?)

val LocalResourceLocale = staticCompositionLocalOf {
    ResourceLocaleManager.locale
}

object ResourceLocaleManager {
    var locale: ResourceLocale by mutableStateOf(ResourceLocale(null))
        private set

    fun update(code: String?) {
        locale = ResourceLocale(code)
    }

}

@Composable
fun ProvideResourceLocale(
    languageCode: String?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalResourceLocale provides ResourceLocale(languageCode), content = content)
}

@Composable
expect fun stringResource(id: ResourceId, vararg formatArgs: Any?): String

@Composable
expect fun painterResource(id: ResourceId): Painter
