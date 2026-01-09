package cn.szkug.akit.graph.ninepatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import java.io.InputStream

private class AndroidNinePatchImage(private val bitmap: Bitmap) : NinePatchImage {
    override val width: Int
        get() = bitmap.width
    override val height: Int
        get() = bitmap.height

    override fun getPixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)
}

internal fun Bitmap.asNinePatchImage(): NinePatchImage = AndroidNinePatchImage(this)

fun NinePatchChunk.Companion.create9PatchDrawable(
    context: Context,
    bitmap: Bitmap?,
    srcName: String?
): Drawable? {
    if (bitmap == null) return null
    return BitmapType.getNinePatchDrawable(context.resources, bitmap, srcName)
        ?: BitmapDrawable(context.resources, bitmap)
}

fun NinePatchChunk.Companion.isRawNinePatchBitmap(bitmap: Bitmap?): Boolean {
    if (bitmap == null) return false
    return isRawNinePatchImage(bitmap.asNinePatchImage())
}
