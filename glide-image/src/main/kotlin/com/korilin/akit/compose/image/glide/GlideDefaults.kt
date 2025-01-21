package com.korilin.akit.compose.image.glide

import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.load.engine.GlideException


interface IGlideImageLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: () -> String)
    fun warn(tag: String, message: String)
    fun error(tag: String, exception: GlideException?)
    fun error(tag: String, message: String)
}

object GlideDefaults {
    @Suppress("ConstPropertyName")
    const val DefaultAlpha = androidx.compose.ui.graphics.DefaultAlpha
    val DefaultContentScale = ContentScale.Fit
    val DefaultAlignment = Alignment.Center
    val DefaultColorFilter = null

    // Custom Logger
    var logger = object : IGlideImageLogger {
        override fun debug(tag: String, message: String) {
            Log.d(tag, message)
        }

        override fun info(tag: String, message: () -> String) {
            Log.i(tag, message())
        }

        override fun warn(tag: String, message: String) {
            Log.w(tag, message)
        }

        override fun error(tag: String, exception: GlideException?) {
            Log.e(tag, exception?.stackTraceToString() ?: "GlideException is null")
        }

        override fun error(tag: String, message: String) {
            Log.e(tag, message)
        }
    }
}