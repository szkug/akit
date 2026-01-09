package cn.szkug.akit.image

import android.content.Context
import android.util.Log
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.platform.LocalContext

actual typealias PlatformContext = Context
actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> = LocalContext

actual object DefaultPlatformAsyncImageLogger : AsyncImageLogger {

    private const val TAG = "AkitAsyncImage"
    var level = Log.ERROR

    actual override fun debug(tag: String, message: () -> String) {
        if (Log.DEBUG < level) return
        Log.d("$TAG [$tag]", message())
    }

    actual override fun info(tag: String, message: () -> String) {
        if (Log.INFO < level) return
        Log.i("$TAG [$tag]", message())
    }

    actual override fun warn(tag: String, message: String) {
        if (Log.WARN < level) return
        Log.w("$TAG [$tag]", message)
    }

    actual override fun error(tag: String, exception: Exception?) {
        if (Log.ERROR < level || exception == null) return
        Log.e("$TAG [$tag]", "${exception::class.simpleName}: ${exception.message.orEmpty()}")
        Log.e("$TAG [$tag]", exception.stackTraceToString())
    }

    actual override fun error(tag: String, message: String) {
        if (Log.ERROR < level) return
        Log.e("$TAG [$tag]", message)
    }
}