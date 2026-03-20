package munchkin.resources.runtime.coil

import munchkin.graph.lottie.LottieResource
import munchkin.resources.runtime.ResourceId
import munchkin.resources.runtime.resolveBundleResourcePath

internal actual fun Any?.resolveCoilImageData(): Any? {
    return when (this) {
        is LottieResource -> resource.resolveCoilImageData()
        is ResourceId -> resolveBundleResourcePath(this) ?: this
        else -> this
    }
}
