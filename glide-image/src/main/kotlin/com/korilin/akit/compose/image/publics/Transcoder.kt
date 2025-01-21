package com.korilin.akit.compose.image.publics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation.CHARSET
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.util.Util
import java.security.MessageDigest
import kotlin.math.max


interface ImageTranscoder<T> {
    fun key(): String
    fun transcode(context: Context, resource: T, width: Int, height: Int): T
}


abstract class BitmapTranscoder : ImageTranscoder<Bitmap>, Transformation<Bitmap> {
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
        val transformed: Bitmap = transcode(context, toTransform, targetWidth, targetHeight)
        return if (toTransform == transformed) resource
        else BitmapResource(transformed, bitmapPool)
    }
}

abstract class DrawableTranscoder : ImageTranscoder<Drawable>, Transformation<Drawable> {

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
        val transformed = transcode(context, toTransform, targetWidth, targetHeight)

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