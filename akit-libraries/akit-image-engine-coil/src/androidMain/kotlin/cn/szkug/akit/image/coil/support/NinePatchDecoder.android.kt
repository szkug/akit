package cn.szkug.akit.image.coil.support

import android.graphics.Bitmap.createBitmap
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Bitmap
import coil3.BitmapImage
import coil3.asImage
import coil3.request.Options

internal actual val BitmapImage.ninePatchChunk: ByteArray?
    get() = bitmap.ninePatchChunk

internal actual val BitmapImage.asComposeImageBimap: ImageBitmap get() = bitmap.asImageBitmap()


actual fun decodeBitmap(bytes: ByteArray): BitmapImage? {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImage()
}

actual fun scaleImage(image: BitmapImage, options: Options): BitmapImage {
    val srcWidth = image.width
    val srcHeight = image.height
    val (outWidth, outHeight) = computeOutputSize(srcWidth, srcHeight, options)
    if (outWidth == srcWidth && outHeight == srcHeight) return image
    return createScaledBitmap(image.bitmap, outWidth, outHeight, true).asImage()
}


actual fun cropNinePatchContent(image: BitmapImage): BitmapImage {
    val contentWidth = (image.width - 2).coerceAtLeast(1)
    val contentHeight = (image.height - 2).coerceAtLeast(1)
    return createBitmap(image.bitmap, 1, 1, contentWidth, contentHeight).asImage()
}