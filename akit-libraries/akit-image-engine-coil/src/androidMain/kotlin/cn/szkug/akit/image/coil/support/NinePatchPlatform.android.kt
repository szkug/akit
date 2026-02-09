package cn.szkug.akit.image.coil.support

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Bitmap
import coil3.Image
import coil3.asImage
import coil3.request.Options

internal actual object NinePatchPlatform {
    actual fun decodeBitmap(bytes: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    actual fun bitmapToImageBitmap(bitmap: Bitmap): ImageBitmap {
        return bitmap.asImageBitmap()
    }

    actual fun bitmapToCoilImage(bitmap: Bitmap): Image {
        return bitmap.asImage()
    }

    actual fun cropNinePatchBitmap(bitmap: Bitmap): Bitmap {
        val contentWidth = (bitmap.width - 2).coerceAtLeast(1)
        val contentHeight = (bitmap.height - 2).coerceAtLeast(1)
        return android.graphics.Bitmap.createBitmap(bitmap, 1, 1, contentWidth, contentHeight)
    }

    actual fun scaleBitmap(bitmap: Bitmap, options: Options): Bitmap {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height
        val (outWidth, outHeight) = computeOutputSize(srcWidth, srcHeight, options)
        if (outWidth == srcWidth && outHeight == srcHeight) return bitmap
        return android.graphics.Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true)
    }

    actual fun ninePatchChunkBytes(bitmap: Bitmap): ByteArray? {
        return bitmap.ninePatchChunk
    }
}
