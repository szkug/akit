package munchkin.resources.loader.coil

import munchkin.resources.loader.BinarySource

internal actual fun BinarySource.resolveCoilBinaryData(): Any {
    return when (this) {
        is BinarySource.Bytes -> value
        is BinarySource.FilePath -> path
        is BinarySource.Raw -> id
        is BinarySource.UriPath -> value
        is BinarySource.Url -> value
    }
}
