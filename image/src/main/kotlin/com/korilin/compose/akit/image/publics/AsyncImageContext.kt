package com.korilin.compose.akit.image.publics

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.DrawableTransformation

data class AsyncImageContext private constructor(
    val context: Context,
    val requestBuilder: (Context) -> RequestBuilder<Drawable>,
    val bitmapTransformation: List<BitmapTransformation>? = null,
    val drawableTransformation: List<DrawableTransformation>? = null,
    val ignoreImagePadding: Boolean = false,
    val enableLog: Boolean = false,
)

@Composable
fun rememberAsyncImageContext(
    context: Context,
    requestBuilder: (Context) -> RequestBuilder<Drawable>,
    bitmapTransformation: List<BitmapTransformation>? = null,
    drawableTransformation: List<DrawableTransformation>? = null,
    ignoreImagePadding: Boolean = false,
    enableLog: Boolean = false,
) = AsyncImageContext(
    context = context,
    requestBuilder = requestBuilder,
    bitmapTransformation = bitmapTransformation,
    drawableTransformation = drawableTransformation,
    ignoreImagePadding = ignoreImagePadding,
    enableLog = enableLog
)