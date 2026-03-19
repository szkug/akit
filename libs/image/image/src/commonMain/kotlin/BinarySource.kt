package munchkin.image

import androidx.compose.runtime.Composable
import munchkin.resources.runtime.RawResourceId

sealed interface BinarySource {
    data class Url(
        val value: String,
        val headers: Map<String, String> = emptyMap(),
    ) : BinarySource

    data class Raw(
        val id: RawResourceId,
    ) : BinarySource

    data class FilePath(
        val path: String,
    ) : BinarySource

    data class UriPath(
        val value: String,
    ) : BinarySource

    data class Bytes(
        val value: ByteArray,
        val cacheKey: String = "inline-bytes",
    ) : BinarySource
}

data class BinaryPayload(
    val bytes: ByteArray,
    val cacheKey: String,
    val source: BinarySource,
)

interface BinarySourceLoader {
    suspend fun load(source: BinarySource): BinaryPayload
}

@Composable
expect fun rememberBinarySourceLoader(): BinarySourceLoader
