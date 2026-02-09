package cn.szkug.akit.image.coil.support

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Bitmap
import coil3.Image
import coil3.request.Options

internal expect object NinePatchPlatform {
    fun decodeBitmap(bytes: ByteArray): Bitmap?
    fun bitmapToImageBitmap(bitmap: Bitmap): ImageBitmap
    fun bitmapToCoilImage(bitmap: Bitmap): Image
    fun cropNinePatchBitmap(bitmap: Bitmap): Bitmap
    fun scaleBitmap(bitmap: Bitmap, options: Options): Bitmap
    fun ninePatchChunkBytes(bitmap: Bitmap): ByteArray?
}
