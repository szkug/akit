package cn.szkug.akit.resources.runtime

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import cn.szkug.akit.graph.toPainter

actual typealias ResourceId = Int

@Composable
actual fun stringResource(id: ResourceId, vararg formatArgs: Any): String {
    return androidx.compose.ui.res.stringResource(id, *formatArgs)
}

@Composable
actual fun painterResource(id: ResourceId): Painter {
    val context = LocalContext.current
    return remember(context, id) {
        AppCompatResources.getDrawable(context, id)!!.toPainter()
    }
}

actual fun resolveResourcePath(id: ResourceId, localeOverride: String?): String? = null
