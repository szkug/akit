package com.korilin.compose.akit.image.publics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.Transformation


private val NormalGlideRequestBuilder: (context: Context) -> RequestBuilder<Drawable> = {
    Glide.with(it).asDrawable()
}

data class AsyncImageContext(
    val context: Context,
    val requestBuilder: (Context) -> RequestBuilder<Drawable> = NormalGlideRequestBuilder,
    val bitmapTransformation: List<Transformation<Bitmap>>? = null,
    val drawableTransformation: List<Transformation<Drawable>>? = null,
    val ignoreImagePadding: Boolean = false,
    val enableLog: Boolean = false,
)

@Composable
fun rememberAsyncImageContext(
    enableLog: Boolean = false,
    ignoreImagePadding: Boolean = false,
    bitmapTransformation: List<BitmapTranscoder>? = null,
    drawableTransformation: List<DrawableTranscoder>? = null,
    requestBuilder: (Context) -> RequestBuilder<Drawable> = NormalGlideRequestBuilder,
): AsyncImageContext {
    val context = LocalContext.current
    return remember(
        requestBuilder,
        bitmapTransformation,
        drawableTransformation,
        ignoreImagePadding,
        enableLog
    ) {
        AsyncImageContext(
            context = context,
            requestBuilder = requestBuilder,
            bitmapTransformation = bitmapTransformation,
            drawableTransformation = drawableTransformation,
            ignoreImagePadding = ignoreImagePadding,
            enableLog = enableLog
        )
    }
}