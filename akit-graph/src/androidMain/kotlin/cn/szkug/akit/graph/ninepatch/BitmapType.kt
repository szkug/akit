package cn.szkug.akit.graph.ninepatch

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.NinePatch as AndroidNinePatch
import android.graphics.drawable.NinePatchDrawable
import kotlin.math.roundToInt

enum class BitmapType {
    NinePatch {
        override fun createChunk(bitmap: Bitmap): NinePatchChunk =
            NinePatchChunk.parse(bitmap.ninePatchChunk!!)
    },
    RawNinePatch {
        override fun createChunk(bitmap: Bitmap): NinePatchChunk {
            return try {
                NinePatchChunk.createChunkFromRawBitmap(bitmap, false)
            } catch (e: WrongPaddingException) {
                NinePatchChunk.createEmptyChunk()
            } catch (e: DivLengthException) {
                NinePatchChunk.createEmptyChunk()
            }
        }

        override fun modifyBitmap(
            resources: Resources,
            bitmap: Bitmap,
            chunk: NinePatchChunk
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
    },
    PlainImage {
        override fun createChunk(bitmap: Bitmap): NinePatchChunk = NinePatchChunk.createEmptyChunk()
    },
    NULL {
        override fun createNinePatchDrawable(
            resources: Resources,
            bitmap: Bitmap?,
            srcName: String?
        ): NinePatchDrawable? = null
    };

    internal open fun createChunk(bitmap: Bitmap): NinePatchChunk =
        NinePatchChunk.createEmptyChunk()

    internal open fun modifyBitmap(
        resources: Resources,
        bitmap: Bitmap,
        chunk: NinePatchChunk
    ): Bitmap = bitmap

    public open fun createNinePatchDrawable(
        resources: Resources,
        bitmap: Bitmap?,
        srcName: String?
    ): NinePatchDrawable? {
        if (bitmap == null) return null
        val chunk = createChunk(bitmap)
        return NinePatchDrawable(
            resources,
            modifyBitmap(resources, bitmap, chunk),
            chunk.toBytes(),
            chunk.padding.toAndroidRect(),
            srcName
        )
    }

    companion object {

        private fun isNinePatchChunk(bytes: ByteArray?) = AndroidNinePatch.isNinePatchChunk(bytes)

        fun determineBitmapType(bitmap: Bitmap?): BitmapType {
            if (bitmap == null) return NULL
            val ninePatchChunk = bitmap.ninePatchChunk
            if (ninePatchChunk != null && isNinePatchChunk(ninePatchChunk)) return NinePatch
            if (NinePatchChunk.isRawNinePatchBitmap(bitmap)) return RawNinePatch
            return PlainImage
        }

        fun getNinePatchDrawable(
            resources: Resources,
            bitmap: Bitmap?,
            srcName: String?
        ): NinePatchDrawable? =
            determineBitmapType(bitmap).createNinePatchDrawable(resources, bitmap, srcName)
    }
}


private fun NinePatchChunk.Companion.createChunkFromRawBitmap(
    bitmap: Bitmap,
    checkBitmap: Boolean
): NinePatchChunk = createChunkFromRawImage(bitmap.asNinePatchImage(), checkBitmap)


fun NinePatchRect.toAndroidRect() = Rect(
    left, top, right, bottom
)