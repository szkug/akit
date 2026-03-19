package munchkin.svga

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

private val sharedPathParser = PathParser()

data class SvgaMovie(
    val version: String,
    val width: Float,
    val height: Float,
    val fps: Int,
    val frames: Int,
    val sprites: List<SvgaSprite>,
    val bitmapAssets: Map<String, ByteArray>,
    val audioAssets: Map<String, SvgaAudioAsset>,
)

data class SvgaAudioAsset(
    val key: String,
    val bytes: ByteArray,
    val startFrame: Int,
    val endFrame: Int,
    val startTimeMillis: Int,
    val totalTimeMillis: Int,
)

data class SvgaSprite(
    val imageKey: String,
    val matteKey: String?,
    val frames: List<SvgaFrame>,
)

data class SvgaFrame(
    val alpha: Float,
    val layout: SvgaLayout,
    val transform: SvgaTransform,
    val clipPathData: String?,
    val shapes: List<SvgaShape>,
) {
    fun clipPathOrNull(): Path? {
        val data = clipPathData ?: return null
        if (data.isBlank()) return null
        return sharedPathParser.parsePathString(data).toPath()
    }

    fun mappedBounds(): Rect {
        return transform.toComposeMatrix().map(layout.asRect())
    }
}

data class SvgaLayout(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
) {
    fun asRect(): Rect = Rect(x, y, x + width, y + height)
}

data class SvgaTransform(
    val a: Float = 1f,
    val b: Float = 0f,
    val c: Float = 0f,
    val d: Float = 1f,
    val tx: Float = 0f,
    val ty: Float = 0f,
) {
    fun toComposeMatrix(): Matrix {
        val matrix = Matrix()
        matrix[0, 0] = a
        matrix[0, 1] = b
        matrix[1, 0] = c
        matrix[1, 1] = d
        matrix[3, 0] = tx
        matrix[3, 1] = ty
        return matrix
    }

    fun map(offset: Offset): Offset = toComposeMatrix().map(offset)
}

enum class SvgaShapeType {
    Shape,
    Rect,
    Ellipse,
    Keep,
}

data class SvgaShape(
    val type: SvgaShapeType,
    val data: SvgaShapeData?,
    val style: SvgaShapeStyle?,
    val transform: SvgaTransform?,
)

sealed interface SvgaShapeData {
    data class PathData(val value: String) : SvgaShapeData

    data class RectData(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val cornerRadius: Float,
    ) : SvgaShapeData

    data class EllipseData(
        val x: Float,
        val y: Float,
        val radiusX: Float,
        val radiusY: Float,
    ) : SvgaShapeData
}

data class SvgaShapeStyle(
    val fill: Color? = null,
    val stroke: Color? = null,
    val strokeWidth: Float = 0f,
    val lineCap: SvgaLineCap = SvgaLineCap.Butt,
    val lineJoin: SvgaLineJoin = SvgaLineJoin.Miter,
    val miterLimit: Float = 0f,
    val lineDash: FloatArray = floatArrayOf(),
)

enum class SvgaLineCap {
    Butt,
    Round,
    Square,
}

enum class SvgaLineJoin {
    Bevel,
    Miter,
    Round,
}

internal fun normalizeBitmapAssetKey(key: String): String = key.removeSuffix(".matte")
