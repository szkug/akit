package cn.szkug.akit.image.glide

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Option
import com.bumptech.glide.visibleGlideContext
import cn.szkug.akit.image.EngineContext
import kotlin.math.min

data class LargeBitmapLimitConfig(val maxWidth: Int, val maxHeight: Int)

val LargeBitmapLimitOption =
    Option.memory<LargeBitmapLimitConfig>(LargeBitmapLimitConfig::class.qualifiedName!!)

/**
 * Fix compose draw too large bitmap
 */
internal object LargeBitmapLimitTransformation : BitmapTransformation() {

    /**
     * [max] method is used internally to compare with RecordingCanvas.getPanelFrameSize,
     * so the minimum value is 100 MB.
     */
    private const val MAX_SIZE = 100f * 1024 * 1024 // 100 MB;

    override fun key(): String {
        return "LargeBitmapLimitTransformation"
    }

    override fun transform(context: EngineContext, resource: Bitmap, width: Int, height: Int): Bitmap {
        val defaultOptions = Glide.get(context.context).visibleGlideContext.defaultRequestOptions
        val config = defaultOptions.options.get(LargeBitmapLimitOption)

        val sizeScale = if (resource.byteCount < MAX_SIZE) 1f else MAX_SIZE / resource.byteCount

        val maxWidth = config?.maxWidth?.toFloat() ?: Float.MAX_VALUE
        val maxHeight = config?.maxHeight?.toFloat() ?: Float.MAX_VALUE

        val widthScale = if (maxWidth > 0 && width > maxWidth) maxWidth / width else 1f
        val heightScale = if (maxHeight > 0 && height > maxHeight) maxHeight / height else 1f

        val sideScale = min(heightScale, widthScale)

        val scale = min(sideScale, sizeScale)

        Log.d("LargeBitmapLimitTransformation", "transform sizeScale=$sizeScale sideScale=$sideScale")

        return if (scale != 1f) resource.scale(
            (scale * width).toInt(),
            (scale * height).toInt(),
            false
        ) else resource
    }

}
