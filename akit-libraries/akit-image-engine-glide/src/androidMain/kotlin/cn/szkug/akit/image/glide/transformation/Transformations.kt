package cn.szkug.akit.image.glide.transformation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.layout.ContentScale
import cn.szkug.akit.image.ImageTransformation
import cn.szkug.akit.image.glide.AndroidContext
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.Key.CHARSET
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.DrawableTransformation
import com.bumptech.glide.request.autoCloneEnabled
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.util.Util
import java.security.MessageDigest
import kotlin.math.max


abstract class BitmapTransformation : ImageTransformation<Bitmap>, Transformation<Bitmap> {
    final override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(key().toByteArray(CHARSET))
    }

    final override fun transform(
        context: Context,
        resource: Resource<Bitmap>,
        outWidth: Int,
        outHeight: Int
    ): Resource<Bitmap> {
        if (!Util.isValidDimensions(outWidth, outHeight)) {
            throw IllegalArgumentException(
                "Cannot apply transformation on width: "
                        + outWidth
                        + " or height: "
                        + outHeight
                        + " less than or equal to zero and not Target.SIZE_ORIGINAL"
            )
        }
        val bitmapPool = Glide.get(context).bitmapPool
        val toTransform = resource.get()
        val targetWidth = if (outWidth == Target.SIZE_ORIGINAL) toTransform.width else outWidth
        val targetHeight = if (outHeight == Target.SIZE_ORIGINAL) toTransform.height else outHeight
        val transformed: Bitmap = transform(AndroidContext(context), toTransform, targetWidth, targetHeight)
        return if (toTransform == transformed) resource
        else BitmapResource(transformed, bitmapPool)
    }
}

abstract class DrawableTransformation : ImageTransformation<Drawable>, Transformation<Drawable> {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(key().toByteArray(CHARSET))
    }

    final override fun transform(
        context: Context,
        resource: Resource<Drawable>,
        outWidth: Int,
        outHeight: Int
    ): Resource<Drawable> {
        if (!Util.isValidDimensions(outWidth, outHeight)) {
            throw IllegalArgumentException(
                "Cannot apply transformation on width: "
                        + outWidth
                        + " or height: "
                        + outHeight
                        + " less than or equal to zero and not Target.SIZE_ORIGINAL"
            )
        }
        val toTransform = resource.get()

        val targetWidth =
            if (outWidth == Target.SIZE_ORIGINAL) toTransform.intrinsicWidth else outWidth
        val targetHeight =
            if (outHeight == Target.SIZE_ORIGINAL) toTransform.intrinsicHeight else outHeight
        val transformed = transform(AndroidContext(context), toTransform, targetWidth, targetHeight)

        return if ((toTransform == transformed)) resource
        else NonOwnedDrawableResource(transformed)
    }

}

private class NonOwnedDrawableResource(drawable: Drawable) :
    DrawableResource<Drawable>(drawable) {
    override fun getResourceClass(): Class<Drawable> {
        return drawable!!.javaClass
    }

    override fun getSize(): Int {
        // 4 bytes per pixel for ARGB_8888 Bitmaps is something of a reasonable approximation. If
        // there are no intrinsic bounds, we can fall back just to 1.
        return max(1.0, (drawable!!.intrinsicWidth * drawable.intrinsicHeight * 4).toDouble())
            .toInt()
    }

    override fun recycle() {
        // Do nothing.
    }
}

internal fun RequestBuilder<Drawable>.setupTransforms(
    contentScale: ContentScale,
    bitmaps: List<Transformation<Bitmap>>,
    drawables: List<Transformation<Drawable>>,
): RequestBuilder<Drawable> {

    return when (contentScale) {
        ContentScale.Crop -> extendCenterCrop(bitmaps, drawables)

        // Outside compose, glide would use FitCenter() for FIT. But that's probably not a good
        // decision given how unimportant Bitmap re-use is relative to minimizing texture sizes now.
        // So instead we'll do something different and prefer not to upscale, which means using
        // CenterInside(). The UI can still scale the view even if the Bitmap is smaller.
        ContentScale.Fit -> extendCenterInside(bitmaps, drawables)

        // These types are also compatible with using CenterInside to handle adaptive size.
        ContentScale.FillHeight,
        ContentScale.FillWidth,
        ContentScale.FillBounds -> extendCenterInside(bitmaps, drawables)

        ContentScale.Inside -> extendCenterInside(bitmaps, drawables)

        else -> this
    }
}

private fun RequestBuilder<Drawable>.extendCenterCrop(
    bitmaps: List<Transformation<Bitmap>>,
    drawables: List<Transformation<Drawable>>,
) =
    optionalCenterCrop().optionalTransforms(
        bitmaps + LargeBitmapLimitTransformation,
        drawables
    )

private fun RequestBuilder<Drawable>.extendCenterInside(
    bitmaps: List<Transformation<Bitmap>>,
    drawables: List<Transformation<Drawable>>,
) =
    optionalCenterInside().optionalTransforms(
        bitmaps + LargeBitmapLimitTransformation,
        drawables
    )

private fun <T> RequestBuilder<T>.optionalTransforms(
    bitmaps: List<Transformation<Bitmap>>?,
    drawables: List<Transformation<Drawable>>?,
): RequestBuilder<T> {

    if (autoCloneEnabled) {
        return clone().optionalTransforms(bitmaps, drawables)
    }

    val outsideBitmap = bitmaps?.combineTransformations()
    val outsideDrawable = drawables?.combineTransformations()

    if (outsideBitmap == null && outsideDrawable == null) return this

    var builder = this
    val insideDrawables = mutableListOf<Transformation<Drawable>>()

    if (outsideBitmap != null) {
        builder = builder.optionalTransform(outsideBitmap)
        insideDrawables += DrawableTransformation(outsideBitmap, false)
    }

    // Some Drawable type is base on Bitmap,
    // So the Bitmap transformations are used first, and then apply Drawable transformations
    if (outsideDrawable != null) insideDrawables += outsideDrawable

    val insideDrawable = insideDrawables.combineTransformations()?.let {
        SkipNinePatchDrawableTransformation(it)
    }

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
