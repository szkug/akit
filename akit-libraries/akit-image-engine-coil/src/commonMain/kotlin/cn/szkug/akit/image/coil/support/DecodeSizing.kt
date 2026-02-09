package cn.szkug.akit.image.coil.support

import coil3.decode.DecodeUtils
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
internal fun computeOutputSize(
    srcWidth: Int,
    srcHeight: Int,
    options: Options,
): Pair<Int, Int> {
    val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        targetSize = options.size,
        scale = options.scale,
        maxSize = options.maxBitmapSize,
    )
    var multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = dstWidth,
        dstHeight = dstHeight,
        scale = options.scale,
    )
    if (options.precision == Precision.INEXACT) {
        multiplier = multiplier.coerceAtMost(1.0)
    }
    val outWidth = (multiplier * srcWidth).toInt()
    val outHeight = (multiplier * srcHeight).toInt()
    return outWidth to outHeight
}
