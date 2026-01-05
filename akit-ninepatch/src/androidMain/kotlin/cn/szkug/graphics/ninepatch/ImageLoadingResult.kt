package cn.szkug.graphics.ninepatch

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.NinePatchDrawable

class ImageLoadingResult internal constructor(
    val bitmap: Bitmap?,
    val chunk: NinePatchChunk?
) {
    fun getNinePatchDrawable(resources: Resources, strName: String?): NinePatchDrawable? {
        val currentBitmap = bitmap ?: return null
        val currentChunk = chunk ?: return NinePatchDrawable(resources, currentBitmap, null, android.graphics.Rect(), strName)
        return NinePatchDrawable(resources, currentBitmap, currentChunk.toBytes(), currentChunk.padding.toAndroidRect(), strName)
    }
}

fun Rect.toAndroidRect() = android.graphics.Rect(
    left, top, right, bottom
)