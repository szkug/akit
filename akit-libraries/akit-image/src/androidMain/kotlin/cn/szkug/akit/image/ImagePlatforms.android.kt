package cn.szkug.akit.image


import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import cn.szkug.akit.graph.toPainter
import cn.szkug.akit.image.glide.DrawableAsyncLoadData
import cn.szkug.akit.image.glide.GlideRequestEngine
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL

actual val SDK_SIZE_ORIGINAL: Int = SIZE_ORIGINAL


actual typealias PlatformImageContext = Context
actual val LocalPlatformImageContext: ProvidableCompositionLocal<PlatformImageContext> = LocalContext

actual object DefaultPlatformAsyncImageLogger : AsyncImageLogger {

    private const val TAG = "AkitAsyncImage"
    private var level = AsyncImageLogger.Level.ERROR

    actual override fun setLevel(level: AsyncImageLogger.Level) {
        this.level = level
    }

    actual override fun debug(tag: String, message: () -> String) {
        if (AsyncImageLogger.Level.DEBUG < level) return
        Log.d("$TAG[$tag]", message())
    }

    actual override fun info(tag: String, message: () -> String) {
        if (AsyncImageLogger.Level.INFO < level) return
        Log.i("$TAG[$tag]", message())
    }

    actual override fun warn(tag: String, message: String) {
        if (AsyncImageLogger.Level.WARN < level) return
        Log.w("$TAG[$tag]", message)
    }

    actual override fun error(tag: String, exception: Exception?) {
        if (AsyncImageLogger.Level.ERROR < level || exception == null) return
        Log.e("$TAG[$tag]", "${exception::class.simpleName}: ${exception.message.orEmpty()}")
        Log.e("$TAG[$tag]", exception.stackTraceToString())
    }

    actual override fun error(tag: String, message: String) {
        if (AsyncImageLogger.Level.ERROR < level) return
        Log.e("$TAG[$tag]", message)
    }
}


actual typealias PlatformAsyncLoadData = DrawableAsyncLoadData

data class DrawableModel(val drawable: Drawable) : ResourceModel

interface DrawableRequestEngine : AsyncRequestEngine<PlatformAsyncLoadData>

actual val LocalPlatformAsyncRequestEngine: ProvidableCompositionLocal<AsyncRequestEngine<PlatformAsyncLoadData>> =
    compositionLocalOf { GlideRequestEngine.Normal }
