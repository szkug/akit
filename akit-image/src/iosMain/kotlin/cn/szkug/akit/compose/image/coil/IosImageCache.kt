package cn.szkug.akit.compose.image.coil

import androidx.compose.ui.graphics.ImageBitmap
import cn.szkug.graphics.ninepatch.NinePatchChunk

internal data class IosCachedImage(
    val image: ImageBitmap,
    val chunk: NinePatchChunk?,
)

internal class IosImageMemoryCache(
    private val maxEntries: Int,
) {
    private val entries = LinkedHashMap<String, IosCachedImage>(0, 0.75f, true)

    fun get(key: String): IosCachedImage? = entries[key]

    fun put(key: String, value: IosCachedImage) {
        entries[key] = value
        trimToSize(maxEntries)
    }

    fun clear() {
        entries.clear()
    }

    private fun trimToSize(maxSize: Int) {
        while (entries.size > maxSize) {
            val iterator = entries.entries.iterator()
            if (!iterator.hasNext()) return
            iterator.next()
            iterator.remove()
        }
    }
}
