package cn.szkug.akit.resources.runtime

import platform.Foundation.NSURL

/**
 * Base type for iOS resource identifiers.
 *
 * Responsibility: expose a stable path string that encodes prefix and value.
 * Implementation: backed by NSURL to keep the ID stable across platform APIs.
 */
abstract class NSResourceId {
    abstract val path: String?
}

/**
 * NSURL-backed resource id implementation.
 */
class NSURLResourceId(val url: NSURL) : NSResourceId() {
    override val path: String?
        get() = url.path
}

/**
 * Decoded resource metadata from the URL path.
 *
 * The prefix is the compose-resources bundle name; value is the resource key/path.
 */
internal data class ResourceInfo(
    val prefix: String,
    val value: String,
)

/**
 * Parsed resource path components.
 */
internal data class ResourcePath(
    val directory: String,
    val name: String,
    val extension: String,
)

/**
 * Resource ID parsing helpers.
 *
 * Performance: parsing is pure and allocation-light; callers should `remember(id)`
 * in composables to avoid repeated splits during recomposition.
 */
internal object ResourceIdsIos {
    fun decodeResourceId(id: ResourceId): ResourceInfo {
        val rawPath = id.path?.trimStart('/') ?: ""
        val decodedPath = rawPath.replace("%7C", "|").replace("%7c", "|")
        val parts = decodedPath.split('|', limit = 3)
        val (prefix, value) = when (parts.size) {
            3 -> parts[1] to parts[2]
            2 -> parts[0] to parts[1]
            else -> "" to parts.getOrNull(0).orEmpty()
        }
        return ResourceInfo(prefix, value)
    }

    fun parseResourcePath(value: String): ResourcePath {
        val normalized = value.trimStart('/')
        val directory = normalized.substringBeforeLast('/', "")
        val fileName = normalized.substringAfterLast('/', normalized)
        val name = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "")
        if (extension.isBlank()) {
            error("Missing extension in resource path: $value")
        }
        return ResourcePath(directory = directory, name = name, extension = extension)
    }
}

/**
 * Factory for iOS resource ids used by generated code.
 *
 * Implementation: encode prefix and value into a single URL path using "prefix|value".
 */
fun <T : ResourceId> resourceId(prefix: String, value: String): T {
    val url = if (prefix.isBlank()) NSURL.fileURLWithPath(value)
    else NSURL.fileURLWithPath("$prefix|$value")
    @Suppress("UNCHECKED_CAST")
    return NSURLResourceId(url) as T
}
