package cn.szkug.akit.image.coil.support

import android.graphics.Bitmap
import android.graphics.Canvas
import cn.szkug.akit.graph.toPainter
import cn.szkug.akit.image.coil.LottieCoilImage
import cn.szkug.akit.image.coil.LottieDecodeResult
import coil3.asImage
import coil3.getExtra
import coil3.request.Options
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import java.io.ByteArrayInputStream

internal actual object LottiePlatform {
    actual fun decode(bytes: ByteArray, options: Options): LottieDecodeResult {
        val input = ByteArrayInputStream(bytes)
        val result = LottieCompositionFactory.fromJsonInputStreamSync(input, null)
        val composition = result.value ?: error("Lottie composition decode failed")
        val iterations = options.getExtra(LottieIterationsKey)
        val drawable = LottieDrawable().apply {
            setComposition(composition)
            repeatMode = LottieDrawable.RESTART
            repeatCount = if (iterations < 0) LottieDrawable.INFINITE else iterations
        }
        val width = composition.bounds.width().coerceAtLeast(1)
        val height = composition.bounds.height().coerceAtLeast(1)
        drawable.setBounds(0, 0, width, height)

        val firstFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(firstFrame)
        drawable.progress = 0f
        drawable.draw(canvas)

        val image = LottieCoilImage(
            firstFrame = firstFrame.asImage(),
            painter = drawable.toPainter(),
            width = width,
            height = height,
            size = width.toLong() * height.toLong() * 4L,
            shareable = false,
        )
        return LottieDecodeResult(image = image, isSampled = false)
    }
}
