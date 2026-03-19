package munchkin.image

import munchkin.resources.loader.clampTo as clampLoaderImageSize

typealias ImageSize = munchkin.resources.loader.ImageSize
typealias ResolvableImageSize = munchkin.resources.loader.ResolvableImageSize
typealias AsyncImageSize = munchkin.resources.loader.AsyncImageSize

fun ImageSize.clampTo(limit: AsyncImageSizeLimit?): ImageSize = with(this) {
    clampLoaderImageSize(limit)
}
