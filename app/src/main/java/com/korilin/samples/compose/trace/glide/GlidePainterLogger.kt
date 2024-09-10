package com.korilin.samples.compose.trace.glide

import android.util.Log
import com.bumptech.glide.load.engine.GlideException
import com.korilin.samples.compose.trace.BuildConfig

interface IGlidePainterLogger {
    fun log(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, exception: GlideException?)
    fun error(tag: String, message: String)
}

object GlidePainterLogger {

    const val LOGGER_ENABLE = BuildConfig.DEBUG

    // Custom Logger
    var logger = object : IGlidePainterLogger {
        override fun log(tag: String, message: String) {
            Log.d(tag, message)
        }

        override fun warn(tag: String, message: String) {
            Log.w(tag, message)
        }

        override fun error(tag: String, exception: GlideException?) {
            exception?.logRootCauses(tag)
        }

        override fun error(tag: String, message: String) {
            Log.e(tag, message)
        }
    }

    inline fun log(tag: String, message: () -> String) {
        if (LOGGER_ENABLE) logger.log(tag, message())
    }

    inline fun warn(tag: String, message: () -> String) {
        if (LOGGER_ENABLE) logger.warn(tag, message())
    }

    fun error(tag: String, exception: GlideException?) {
        if (LOGGER_ENABLE) logger.error(tag, exception)
    }

    inline fun error(tag: String, message: () -> String) {
        if (LOGGER_ENABLE) logger.error(tag, message())
    }
}