package munchkin.resources.runtime.coil

import munchkin.resources.runtime.BinarySource
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
