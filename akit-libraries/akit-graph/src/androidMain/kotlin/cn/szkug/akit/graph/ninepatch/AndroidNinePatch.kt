package cn.szkug.akit.graph.ninepatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import androidx.core.graphics.drawable.toDrawable

fun NinePatchChunk.Companion.createNinePatchDrawable(
    context: Context,
    bitmap: Bitmap?,
    srcName: String?
): Drawable? {
    if (bitmap == null) return null
    val resources = context.resources
    val parsed = parseNinePatch(bitmap.asNinePatchSource(), bitmap.ninePatchChunk)
    return when (parsed.type) {
        NinePatchType.Chunk -> {
            val chunk = parsed.chunk ?: return bitmap.toDrawable(resources)
            NinePatchDrawable(
                resources,
                bitmap,
                chunk.toBytes(),
                chunk.padding.toAndroidRect(),
                srcName
            )
        }

        NinePatchType.Raw -> {
            val chunk = parsed.chunk ?: return bitmap.toDrawable(resources)
            RawNinePatchProcessor.createDrawable(resources, bitmap, chunk, srcName)
        }

        NinePatchType.None -> bitmap.toDrawable(resources)
    }
}

fun NinePatchChunk.Companion.isRawNinePatch(bitmap: Bitmap?): Boolean {
    if (bitmap == null) return false
    return isRawNinePatch(bitmap.asNinePatchSource(), bitmap.ninePatchChunk)
}
