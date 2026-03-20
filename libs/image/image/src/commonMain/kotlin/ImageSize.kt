package munchkin.image

import munchkin.resources.runtime.clampTo as clampLoaderImageSize

typealias ImageSize = munchkin.resources.runtime.ImageSize
typealias ResolvableImageSize = munchkin.resources.runtime.ResolvableImageSize
typealias AsyncImageSize = munchkin.resources.runtime.AsyncImageSize

fun ImageSize.clampTo(limit: AsyncImageSizeLimit?): ImageSize = with(this) {
    clampLoaderImageSize(limit)
}
