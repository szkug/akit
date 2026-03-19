package munchkin.svga

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

internal actual suspend fun decodeSvgaBitmap(bytes: ByteArray): ImageBitmap? = withContext(Dispatchers.Default) {
    if (bytes.isEmpty()) return@withContext null
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}
