package munchkin.resources.loader.coil

import munchkin.resources.loader.BinarySource
import munchkin.resources.runtime.resolveBundleResourcePath

internal actual fun BinarySource.resolveCoilBinaryData(): Any {
    return when (this) {
        is BinarySource.Bytes -> value
        is BinarySource.FilePath -> path
        is BinarySource.Raw -> resolveBundleResourcePath(id) ?: error("Unable to resolve raw resource: $id")
        is BinarySource.UriPath -> value
        is BinarySource.Url -> value
    }
}
