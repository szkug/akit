package munchkin.resources.loader

import android.graphics.drawable.Drawable
import android.util.Log

actual object DefaultPlatformMunchkinLogger : MunchkinLogger {

    private const val TAG = "Munchkin"
    private var level = MunchkinLogger.Level.ERROR

    actual override fun setLevel(level: MunchkinLogger.Level) {
        this.level = level
    }

    actual override fun debug(feature: String, message: () -> String) {
        if (MunchkinLogger.Level.DEBUG < level) return
        Log.d(TAG, "[$feature] ${message()}")
    }

    actual override fun info(feature: String, message: () -> String) {
        if (MunchkinLogger.Level.INFO < level) return
        Log.i(TAG, "[$feature] ${message()}")
    }

    actual override fun warn(feature: String, message: String) {
        if (MunchkinLogger.Level.WARN < level) return
        Log.w(TAG, "[$feature] $message")
    }

    actual override fun error(feature: String, exception: Exception?) {
        if (MunchkinLogger.Level.ERROR < level || exception == null) return
        Log.e(TAG, "[$feature] ${exception::class.simpleName}: ${exception.message.orEmpty()}")
        Log.e(TAG, "[$feature] ${exception.stackTraceToString()}")
    }

    actual override fun error(feature: String, message: String) {
        if (MunchkinLogger.Level.ERROR < level) return
        Log.e(TAG, "[$feature] $message")
    }
}

data class DrawableModel(val drawable: Drawable) : ResourceModel
