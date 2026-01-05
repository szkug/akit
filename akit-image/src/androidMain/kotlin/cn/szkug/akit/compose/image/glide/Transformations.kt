package cn.szkug.akit.compose.image.glide

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.DrawableTransformation
import com.bumptech.glide.request.autoCloneEnabled
import cn.szkug.akit.publics.AndroidAsyncImageContextData
import cn.szkug.akit.fixed.LargeBitmapLimitTransformation

internal fun RequestBuilder<Drawable>.setupTransforms(
    contentScale: ContentScale,
    contextData: AndroidAsyncImageContextData,
): RequestBuilder<Drawable> {

    return when (contentScale) {
        ContentScale.Crop -> extendCenterCrop(contextData)

        // Outside compose, glide would use FitCenter() for FIT. But that's probably not a good
        // decision given how unimportant Bitmap re-use is relative to minimizing texture sizes now.
        // So instead we'll do something different and prefer not to upscale, which means using
        // CenterInside(). The UI can still scale the view even if the Bitmap is smaller.
        ContentScale.Fit -> extendCenterInside(contextData)

        // These types are also compatible with using CenterInside to handle adaptive size.
        ContentScale.FillHeight,
        ContentScale.FillWidth,
        ContentScale.FillBounds -> extendCenterInside(contextData)

        ContentScale.Inside -> extendCenterInside(contextData)

        else -> this
    }
}


private fun RequestBuilder<Drawable>.extendCenterCrop(contextData: AndroidAsyncImageContextData) =
    optionalTransforms(
        DownsampleStrategy.CENTER_OUTSIDE,
        CenterCrop() + contextData.bitmapTransformations.orEmpty() + LargeBitmapLimitTransformation,
        contextData.drawableTransformations
    )

private fun RequestBuilder<Drawable>.extendCenterInside(contextData: AndroidAsyncImageContextData) =
    optionalTransforms(
        DownsampleStrategy.CENTER_INSIDE,
        CenterInside() + contextData.bitmapTransformations.orEmpty() + LargeBitmapLimitTransformation,
        contextData.drawableTransformations
    )

private fun <T> RequestBuilder<T>.optionalTransforms(
    strategy: DownsampleStrategy,
    bitmapTransformations: List<Transformation<Bitmap>>?,
    drawableTransformations: List<Transformation<Drawable>>?
): RequestBuilder<T> {

    if (autoCloneEnabled) {
        return clone().optionalTransforms(strategy, bitmapTransformations, drawableTransformations)
    }

    val outsideBitmap = bitmapTransformations?.combineTransformations()
    val outsideDrawable = drawableTransformations?.combineTransformations()

    if (outsideBitmap == null && outsideDrawable == null) return this

    var builder = downsample(strategy)
    val insideDrawables = mutableListOf<Transformation<Drawable>>()

    if (outsideBitmap != null) {
        builder = builder.optionalTransform(outsideBitmap)
        insideDrawables += DrawableTransformation(outsideBitmap, false)
    }

    // Some Drawable type is base on Bitmap,
    // So the Bitmap transformations are used first, and then apply Drawable transformations
    if (outsideDrawable != null) insideDrawables += outsideDrawable

    val insideDrawable = insideDrawables.combineTransformations()

    if (insideDrawable != null) {
        builder = builder.optionalTransform(Drawable::class.java, insideDrawable)
        @Suppress("UNCHECKED_CAST")
        builder = builder.optionalTransform(
            BitmapDrawable::class.java,
            insideDrawable as Transformation<BitmapDrawable>
        )
    }

    return builder

}

private fun <T> List<Transformation<T>>.combineTransformations(): Transformation<T>? {
    val list = this
    return if (list.size > 1) MultiTransformation(this)
    else if (list.size == 1) first()
    else null
}

private operator fun <T> Transformation<T>.plus(list: Collection<Transformation<T>>): List<Transformation<T>> {
    val result = ArrayList<Transformation<T>>(list.size + 1)
    result.add(this)
    result.addAll(list)
    return result
}
