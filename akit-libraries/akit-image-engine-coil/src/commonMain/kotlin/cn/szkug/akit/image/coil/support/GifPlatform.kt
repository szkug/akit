package cn.szkug.akit.image.coil.support

import cn.szkug.akit.image.coil.GifDecodeResult
import coil3.request.Options

internal expect object GifPlatform {
    fun decode(bytes: ByteArray, options: Options): GifDecodeResult
}
