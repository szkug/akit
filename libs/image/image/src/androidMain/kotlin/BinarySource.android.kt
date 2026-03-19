package munchkin.image

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Composable
actual fun rememberBinarySourceLoader(): BinarySourceLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidBinarySourceLoader(context) }
}

private class AndroidBinarySourceLoader(
    private val context: Context,
) : BinarySourceLoader {

    override suspend fun load(source: BinarySource): BinaryPayload = withContext(Dispatchers.IO) {
        when (source) {
            is BinarySource.Bytes -> BinaryPayload(source.value, source.cacheKey, source)
            is BinarySource.FilePath -> {
                val bytes = File(source.path).readBytes()
                BinaryPayload(bytes, source.path, source)
            }

            is BinarySource.Raw -> {
                val bytes = context.resources.openRawResource(source.id).use { it.readBytes() }
                BinaryPayload(bytes, "android-raw:${source.id}", source)
            }

            is BinarySource.UriPath -> {
                val bytes = openUri(source.value)
                BinaryPayload(bytes, source.value, source)
            }

            is BinarySource.Url -> {
                val connection = (URL(source.value).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    source.headers.forEach { (key, value) -> setRequestProperty(key, value) }
                }
                connection.inputStream.use { input ->
                    BinaryPayload(input.readBytes(), source.value, source)
                }.also {
                    connection.disconnect()
                }
            }
        }
    }

    private fun openUri(uriString: String): ByteArray {
        val uri = Uri.parse(uriString)
        val scheme = uri.scheme.orEmpty().lowercase()
        return when (scheme) {
            "content" -> contentResolver().openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to open content uri: $uriString")

            "file" -> File(requireNotNull(uri.path) { "Missing file path for $uriString" }).readBytes()
            "android.resource" -> contentResolver().openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to open resource uri: $uriString")

            else -> File(uriString).readBytes()
        }
    }

    private fun contentResolver(): ContentResolver = context.contentResolver
}
