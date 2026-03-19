@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package munchkin.svga

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ProtoMovieEntity(
    @ProtoNumber(1) val version: String? = null,
    @ProtoNumber(2) val params: ProtoMovieParams? = null,
    @ProtoNumber(3) val images: Map<String, ByteArray> = emptyMap(),
    @ProtoNumber(4) val sprites: List<ProtoSpriteEntity> = emptyList(),
    @ProtoNumber(5) val audios: List<ProtoAudioEntity> = emptyList(),
)

@Serializable
internal data class ProtoMovieParams(
    @ProtoNumber(1) val viewBoxWidth: Float? = null,
    @ProtoNumber(2) val viewBoxHeight: Float? = null,
    @ProtoNumber(3) val fps: Int? = null,
    @ProtoNumber(4) val frames: Int? = null,
)

@Serializable
internal data class ProtoSpriteEntity(
    @ProtoNumber(1) val imageKey: String? = null,
    @ProtoNumber(2) val frames: List<ProtoFrameEntity> = emptyList(),
    @ProtoNumber(3) val matteKey: String? = null,
)

@Serializable
internal data class ProtoFrameEntity(
    @ProtoNumber(1) val alpha: Float? = null,
    @ProtoNumber(2) val layout: ProtoLayout? = null,
    @ProtoNumber(3) val transform: ProtoTransform? = null,
    @ProtoNumber(4) val clipPath: String? = null,
    @ProtoNumber(5) val shapes: List<ProtoShapeEntity> = emptyList(),
)

@Serializable
internal data class ProtoLayout(
    @ProtoNumber(1) val x: Float? = null,
    @ProtoNumber(2) val y: Float? = null,
    @ProtoNumber(3) val width: Float? = null,
    @ProtoNumber(4) val height: Float? = null,
)

@Serializable
internal data class ProtoTransform(
    @ProtoNumber(1) val a: Float? = null,
    @ProtoNumber(2) val b: Float? = null,
    @ProtoNumber(3) val c: Float? = null,
    @ProtoNumber(4) val d: Float? = null,
    @ProtoNumber(5) val tx: Float? = null,
    @ProtoNumber(6) val ty: Float? = null,
)

@Serializable
internal data class ProtoShapeEntity(
    @ProtoNumber(1) val type: ProtoShapeType? = null,
    @ProtoNumber(10) val styles: ProtoShapeStyle? = null,
    @ProtoNumber(11) val transform: ProtoTransform? = null,
    @ProtoNumber(2) val shape: ProtoShapeArgs? = null,
    @ProtoNumber(3) val rect: ProtoRectArgs? = null,
    @ProtoNumber(4) val ellipse: ProtoEllipseArgs? = null,
)

@Serializable
internal enum class ProtoShapeType {
    @SerialName("0") SHAPE,
    @SerialName("1") RECT,
    @SerialName("2") ELLIPSE,
    @SerialName("3") KEEP,
}

@Serializable
internal data class ProtoShapeArgs(
    @ProtoNumber(1) val d: String? = null,
)

@Serializable
internal data class ProtoRectArgs(
    @ProtoNumber(1) val x: Float? = null,
    @ProtoNumber(2) val y: Float? = null,
    @ProtoNumber(3) val width: Float? = null,
    @ProtoNumber(4) val height: Float? = null,
    @ProtoNumber(5) val cornerRadius: Float? = null,
)

@Serializable
internal data class ProtoEllipseArgs(
    @ProtoNumber(1) val x: Float? = null,
    @ProtoNumber(2) val y: Float? = null,
    @ProtoNumber(3) val radiusX: Float? = null,
    @ProtoNumber(4) val radiusY: Float? = null,
)

@Serializable
internal data class ProtoShapeStyle(
    @ProtoNumber(1) val fill: ProtoRgbaColor? = null,
    @ProtoNumber(2) val stroke: ProtoRgbaColor? = null,
    @ProtoNumber(3) val strokeWidth: Float? = null,
    @ProtoNumber(4) val lineCap: ProtoLineCap? = null,
    @ProtoNumber(5) val lineJoin: ProtoLineJoin? = null,
    @ProtoNumber(6) val miterLimit: Float? = null,
    @ProtoNumber(7) val lineDashI: Float? = null,
    @ProtoNumber(8) val lineDashII: Float? = null,
    @ProtoNumber(9) val lineDashIII: Float? = null,
)

@Serializable
internal data class ProtoRgbaColor(
    @ProtoNumber(1) val r: Float? = null,
    @ProtoNumber(2) val g: Float? = null,
    @ProtoNumber(3) val b: Float? = null,
    @ProtoNumber(4) val a: Float? = null,
)

@Serializable
internal enum class ProtoLineCap {
    @SerialName("0") LineCap_BUTT,
    @SerialName("1") LineCap_ROUND,
    @SerialName("2") LineCap_SQUARE,
}

@Serializable
internal enum class ProtoLineJoin {
    @SerialName("0") LineJoin_BEVEL,
    @SerialName("1") LineJoin_MITER,
    @SerialName("2") LineJoin_ROUND,
}

@Serializable
internal data class ProtoAudioEntity(
    @ProtoNumber(1) val audioKey: String? = null,
    @ProtoNumber(2) val startFrame: Int? = null,
    @ProtoNumber(3) val endFrame: Int? = null,
    @ProtoNumber(4) val startTime: Int? = null,
    @ProtoNumber(5) val totalTime: Int? = null,
)
