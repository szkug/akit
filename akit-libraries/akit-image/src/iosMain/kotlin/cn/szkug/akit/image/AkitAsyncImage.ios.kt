package cn.szkug.akit.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import cn.szkug.akit.image.coil.CoilRequestEngine
import cn.szkug.akit.image.coil.PainterAsyncLoadData

actual typealias PlatformAsyncLoadData = PainterAsyncLoadData
actual val LocalPlatformAsyncRequestEngine: ProvidableCompositionLocal<AsyncRequestEngine<PlatformAsyncLoadData>> =
    compositionLocalOf { CoilRequestEngine.Normal }


@Composable
actual fun Any?.toResourceModel(): ResourceModel? = when (this) {
    is Painter -> PainterModel(this)
    is ImageBitmap -> PainterModel(BitmapPainter(this))
    else -> null
}


@Composable
actual fun Any?.toPainterModel(): PainterModel? = when (this) {
    is Painter -> PainterModel(this)
    is ImageBitmap -> PainterModel(BitmapPainter(this))
    else -> null
}