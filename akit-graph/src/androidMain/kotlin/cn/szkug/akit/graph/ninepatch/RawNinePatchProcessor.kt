package cn.szkug.akit.graph.ninepatch

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.NinePatchDrawable
import kotlin.math.roundToInt

internal object RawNinePatchProcessor {
    fun createDrawable(
        resources: Resources,
        bitmap: Bitmap,
        chunk: NinePatchChunk,
        srcName: String?,
    ): NinePatchDrawable {
        val content = cropAndScale(resources, bitmap, chunk)
        return NinePatchDrawable(
            resources,
            content,
            chunk.toBytes(),
            chunk.padding.toAndroidRect(),
            srcName
        )
    }

    private fun cropAndScale(
        resources: Resources,
        bitmap: Bitmap,
        chunk: NinePatchChunk,
    ): Bitmap {
        var content = Bitmap.createBitmap(bitmap, 1, 1, bitmap.width - 2, bitmap.height - 2)
        val targetDensity = resources.displayMetrics.densityDpi
        val densityChange = targetDensity.toFloat() / bitmap.density
        if (densityChange != 1f) {
            val dstWidth = (content.width * densityChange).roundToInt()
            val dstHeight = (content.height * densityChange).roundToInt()
            content = Bitmap.createScaledBitmap(content, dstWidth, dstHeight, true)
            content.density = targetDensity
            chunk.padding = NinePatchRect(
                (chunk.padding.left * densityChange).roundToInt(),
                (chunk.padding.top * densityChange).roundToInt(),
                (chunk.padding.right * densityChange).roundToInt(),
                (chunk.padding.bottom * densityChange).roundToInt()
            )
            recalculateDivs(densityChange, chunk.xDivs)
            recalculateDivs(densityChange, chunk.yDivs)
        }
        return content
    }

    private fun recalculateDivs(densityChange: Float, divs: MutableList<NinePatchDiv>) {
        divs.forEach { div ->
            div.start = Math.round(div.start * densityChange)
            div.stop = Math.round(div.stop * densityChange)
        }
    }
}
