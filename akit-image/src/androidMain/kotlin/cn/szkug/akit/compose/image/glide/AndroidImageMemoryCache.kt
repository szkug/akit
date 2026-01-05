package cn.szkug.akit.compose.image.glide

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import java.util.WeakHashMap

internal object AndroidImageMemoryCache {

    private val caches = WeakHashMap<Context, LruCache<String, Drawable>>()

    fun get(context: Context): LruCache<String, Drawable> = synchronized(caches) {
        caches[context] ?: createCache().also { caches[context] = it }
    }

    private fun createCache(): LruCache<String, Drawable> {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(1)
        return object : LruCache<String, Drawable>(maxKb) {
            override fun sizeOf(key: String, value: Drawable): Int {
                val width = value.intrinsicWidth
                val height = value.intrinsicHeight
                if (width > 0 && height > 0) {
                    val size = (width.toLong() * height.toLong() * 4L / 1024L).toInt()
                    return size.coerceAtLeast(1)
                }
                return 1
            }
        }
    }
}
