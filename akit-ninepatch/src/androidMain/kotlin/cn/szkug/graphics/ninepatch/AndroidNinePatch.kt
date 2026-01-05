package cn.szkug.graphics.ninepatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.NinePatchDrawable
import java.io.InputStream

private class AndroidNinePatchImage(private val bitmap: Bitmap) : NinePatchImage {
    override val width: Int
        get() = bitmap.width
    override val height: Int
        get() = bitmap.height

    override fun getPixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)
}

private fun Bitmap.asNinePatchImage(): NinePatchImage = AndroidNinePatchImage(this)

fun NinePatchChunk.Companion.create9PatchDrawable(
    context: Context,
    bitmap: Bitmap?,
    srcName: String?
): NinePatchDrawable? {
    if (bitmap == null) return null
    return BitmapType.getNinePatchDrawable(context.resources, bitmap, srcName)
        ?: NinePatchDrawable(context.resources, bitmap, null, android.graphics.Rect(), srcName)
}

fun NinePatchChunk.Companion.create9PatchDrawable(
    context: Context,
    inputStream: InputStream,
    srcName: String?
): NinePatchDrawable? = create9PatchDrawable(context, inputStream, DEFAULT_DENSITY, srcName)

fun NinePatchChunk.Companion.create9PatchDrawable(
    context: Context,
    inputStream: InputStream,
    imageDensity: Int,
    srcName: String?
): NinePatchDrawable? {
    val loadingResult = createChunkFromRawBitmap(context, inputStream, imageDensity)
    return loadingResult.getNinePatchDrawable(context.resources, srcName)
}

fun NinePatchChunk.Companion.createChunkFromRawBitmap(bitmap: Bitmap?): NinePatchChunk {
    if (bitmap == null) return createEmptyChunk()
    return try {
        createChunkFromRawImage(bitmap.asNinePatchImage(), true)
    } catch (e: Exception) {
        createEmptyChunk()
    }
}

fun NinePatchChunk.Companion.createChunkFromRawBitmap(
    context: Context,
    inputStream: InputStream
): ImageLoadingResult = createChunkFromRawBitmap(context, inputStream, DEFAULT_DENSITY)

fun NinePatchChunk.Companion.createChunkFromRawBitmap(
    context: Context,
    inputStream: InputStream,
    imageDensity: Int
): ImageLoadingResult {
    val opts = BitmapFactory.Options().apply {
        inDensity = imageDensity
        inTargetDensity = imageDensity
    }
    val bitmap = BitmapFactory.decodeStream(inputStream, android.graphics.Rect(), opts)
    return createChunkFromRawBitmap(context, bitmap)
}

fun NinePatchChunk.Companion.createChunkFromRawBitmap(
    context: Context,
    bitmap: Bitmap?
): ImageLoadingResult {
    if (bitmap == null) return ImageLoadingResult(null, createEmptyChunk())
    val type = BitmapType.determineBitmapType(bitmap)
    val chunk = type.createChunk(bitmap)
    val modified = type.modifyBitmap(context.resources, bitmap, chunk)
    return ImageLoadingResult(modified, chunk)
}

internal fun NinePatchChunk.Companion.createChunkFromRawBitmap(
    bitmap: Bitmap,
    checkBitmap: Boolean
): NinePatchChunk = createChunkFromRawImage(bitmap.asNinePatchImage(), checkBitmap)

fun NinePatchChunk.Companion.isRawNinePatchBitmap(bitmap: Bitmap?): Boolean {
    if (bitmap == null) return false
    return isRawNinePatchImage(bitmap.asNinePatchImage())
}
