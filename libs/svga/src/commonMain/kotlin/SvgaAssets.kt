package munchkin.svga

import androidx.compose.ui.graphics.ImageBitmap

internal data class PreparedSvgaMovie(
    val movie: SvgaMovie,
    val bitmaps: Map<String, ImageBitmap>,
)

internal suspend fun prepareSvgaMovie(movie: SvgaMovie): PreparedSvgaMovie {
    val bitmaps = linkedMapOf<String, ImageBitmap>()
    movie.bitmapAssets.forEach { (key, bytes) ->
        decodeSvgaBitmap(bytes)?.let { bitmaps[key] = it }
    }
    return PreparedSvgaMovie(movie = movie, bitmaps = bitmaps)
}

internal expect suspend fun decodeSvgaBitmap(bytes: ByteArray): ImageBitmap?
