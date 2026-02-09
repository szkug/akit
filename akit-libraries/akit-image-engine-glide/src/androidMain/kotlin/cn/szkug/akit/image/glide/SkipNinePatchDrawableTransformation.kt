package cn.szkug.akit.image.glide

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import java.security.MessageDigest

class SkipNinePatchDrawableTransformation(
    private val delegate: Transformation<Drawable>
) : Transformation<Drawable> {
    override fun transform(
        context: Context,
        resource: Resource<Drawable>,
        outWidth: Int,
        outHeight: Int
    ): Resource<Drawable> {
        val drawable = resource.get()
        return if (drawable is NinePatchDrawable) resource
        else delegate.transform(context, resource, outWidth, outHeight)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        delegate.updateDiskCacheKey(messageDigest)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SkipNinePatchDrawableTransformation) return false
        return delegate == other.delegate
    }

    override fun hashCode(): Int = delegate.hashCode()
}