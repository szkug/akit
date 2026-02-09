package cn.szkug.akit.image

import android.graphics.drawable.Drawable
import android.util.Log


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


data class DrawableModel(val drawable: Drawable) : ResourceModel
