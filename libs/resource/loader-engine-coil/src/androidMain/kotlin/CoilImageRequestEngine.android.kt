package munchkin.resources.loader.coil

import munchkin.graph.lottie.LottieResource
import munchkin.resources.runtime.ResourceId

internal actual fun Any?.resolveCoilImageData(): Any? {
    return when (this) {
        is LottieResource -> resource.resolveCoilImageData()
        is ResourceId -> this
        else -> this
    }
}
