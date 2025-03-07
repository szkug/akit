package com.korilin.akit.compose.image.publics

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder



open class AsyncImageContext(
    val context: Context,
    val enableLog: Boolean = false,
    val requestBuilder: (Context) -> RequestBuilder<Drawable> = NormalGlideRequestBuilder,

    // internal support fields
    val ignoreImagePadding: Boolean = false,

    // transformations
    val bitmapTransformations: List<BitmapTransformation>? = null,
    val drawableTransformations: List<DrawableTransformation>? = null,
) {
    companion object {

        val NormalGlideRequestBuilder: (context: Context) -> RequestBuilder<Drawable> = {
            Glide.with(it).asDrawable()
        }
    }
}

@Composable
fun rememberAsyncImageContext(
    enableLog: Boolean = false,
    ignoreImagePadding: Boolean = false,
    bitmapTransformation: List<BitmapTransformation>? = null,
    drawableTransformation: List<DrawableTransformation>? = null,
    requestBuilder: (Context) -> RequestBuilder<Drawable> = AsyncImageContext.NormalGlideRequestBuilder,
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
            bitmapTransformations = bitmapTransformation,
            drawableTransformations = drawableTransformation,
            ignoreImagePadding = ignoreImagePadding,
            enableLog = enableLog
        )
    }
}