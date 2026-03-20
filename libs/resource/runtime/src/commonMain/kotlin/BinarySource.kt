package munchkin.resources.runtime
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

fun BinarySource.cacheKey(): String {
    return when (this) {
        is BinarySource.Bytes -> cacheKey
        is BinarySource.FilePath -> path
        is BinarySource.Raw -> id.toString()
        is BinarySource.UriPath -> value
        is BinarySource.Url -> value
    }
}
