package munchkin.resources.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import munchkin.resources.runtime.resolveBundleResourcePath
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

@Composable
internal actual fun rememberPlatformBinarySourceLoader(): PlatformBinarySourceLoader = remember { AppleBinarySourceLoader() }

private class AppleBinarySourceLoader : PlatformBinarySourceLoader {

    override suspend fun load(source: BinarySource): BinaryPayload = withContext(Dispatchers.Default) {
        when (source) {
            is BinarySource.Bytes -> BinaryPayload(source.value, source.cacheKey, source)
            is BinarySource.FilePath -> BinaryPayload(readFile(source.path), source.cacheKey(), source)
            is BinarySource.Raw -> {
                val path = resolveBundleResourcePath(source.id)
                    ?: error("Unable to resolve raw resource: ${source.id}")
                BinaryPayload(readFile(path), source.cacheKey(), source)
            }

            is BinarySource.UriPath -> {
                val value = source.value
                BinaryPayload(readUri(value), source.cacheKey(), source)
            }

            is BinarySource.Url -> {
                BinaryPayload(readUrl(source.value), source.cacheKey(), source)
            }
        }
    }

    private fun readUrl(url: String): ByteArray {
        val nsUrl = NSURL.URLWithString(url) ?: error("Invalid url: $url")
        val data = NSData.dataWithContentsOfURL(nsUrl) ?: error("Unable to load url: $url")
        return data.toByteArray()
    }

    private fun readUri(value: String): ByteArray {
        val url = NSURL.URLWithString(value)
        return if (url != null && url.scheme != null) {
            val data = NSData.dataWithContentsOfURL(url) ?: error("Unable to load uri: $value")
            data.toByteArray()
        } else {
            readFile(value)
        }
    }

    private fun readFile(path: String): ByteArray {
        val data = NSData.dataWithContentsOfFile(path) ?: error("Unable to read file: $path")
        return data.toByteArray()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val source = this.bytes ?: return ByteArray(0)
    val buffer = ByteArray(length)
    buffer.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, this.length)
    }
    return buffer
}
