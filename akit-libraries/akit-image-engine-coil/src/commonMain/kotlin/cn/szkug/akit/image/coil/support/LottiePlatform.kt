package cn.szkug.akit.image.coil.support

import cn.szkug.akit.image.coil.LottieDecodeResult
import coil3.request.Options

internal expect object LottiePlatform {
    fun decode(bytes: ByteArray, options: Options): LottieDecodeResult
}
