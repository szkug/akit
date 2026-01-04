package com.korilin.akit.publics

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.korilin.akit.compose.image.glide.toPainter

internal data class AndroidAsyncImageContextData(
    val context: Context,
    val requestBuilder: (Context) -> RequestBuilder<Drawable>,
    val bitmapTransformations: List<BitmapTransformation>?,
    val drawableTransformations: List<DrawableTransformation>?,
)

internal fun AsyncImageContext.requireAndroidData(defaultContext: Context): AndroidAsyncImageContextData {
    return platformData as? AndroidAsyncImageContextData
        ?: AndroidAsyncImageContextData(
            context = defaultContext,
            requestBuilder = AsyncImageContext.NormalGlideRequestBuilder,
            bitmapTransformations = null,
            drawableTransformations = null,
        )
}

internal fun AsyncImageContext.withAndroidData(data: AndroidAsyncImageContextData): AsyncImageContext {
    return AsyncImageContext(
        enableLog = enableLog,
        ignoreImagePadding = ignoreImagePadding,
        platformData = data,
    )
}

val AsyncImageContext.Companion.NormalGlideRequestBuilder: (Context) -> RequestBuilder<Drawable>
    get() = { Glide.with(it).asDrawable() }

@Composable
fun rememberAsyncImageContext(
    vararg keys: Any?,
    enableLog: Boolean = false,
    ignoreImagePadding: Boolean = false,
    bitmapTransformation: List<BitmapTransformation>? = null,
    drawableTransformation: List<DrawableTransformation>? = null,
    requestBuilder: (Context) -> RequestBuilder<Drawable>,
): AsyncImageContext {
    val context = LocalContext.current
    return remember(
        requestBuilder,
        bitmapTransformation,
        drawableTransformation,
        ignoreImagePadding,
        enableLog,
        context,
        *keys
    ) {
        AsyncImageContext(
            enableLog = enableLog,
            ignoreImagePadding = ignoreImagePadding,
            platformData = AndroidAsyncImageContextData(
                context = context,
                requestBuilder = requestBuilder,
                bitmapTransformations = bitmapTransformation,
                drawableTransformations = drawableTransformation,
            ),
        )
    }
}

fun glideAsyncImageContext(
    context: Context,
    enableLog: Boolean = false,
    ignoreImagePadding: Boolean = false,
    bitmapTransformation: List<BitmapTransformation>? = null,
    drawableTransformation: List<DrawableTransformation>? = null,
    requestBuilder: (Context) -> RequestBuilder<Drawable> = AsyncImageContext.NormalGlideRequestBuilder,
): AsyncImageContext {
    return AsyncImageContext(
        enableLog = enableLog,
        ignoreImagePadding = ignoreImagePadding,
        platformData = AndroidAsyncImageContextData(
            context = context,
            requestBuilder = requestBuilder,
            bitmapTransformations = bitmapTransformation,
            drawableTransformations = drawableTransformation,
        ),
    )
}

fun PainterModel.Companion.fromId(id: Int?, context: Context): PainterModel? {
    if (id == null) return null
    val drawable = AppCompatResources.getDrawable(context, id) ?: return null
    return PainterModel(drawable.toPainter())
}

@Composable
fun PainterModel.Companion.fromId(id: Int?): PainterModel? {
    if (id == null) return null
    val context = LocalContext.current
    val drawable = AppCompatResources.getDrawable(context, id) ?: return null
    return PainterModel(drawable.toPainter())
}
