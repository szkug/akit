@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package munchkin.svga

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf

object SvgaDecoder {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun decode(bytes: ByteArray): SvgaMovie {
        val container = unpackContainer(bytes)
        val binary = container.find("movie.binary")
        val spec = container.find("movie.spec")
        return when {
            binary != null -> decodeBinary(binary, container.entries)
            spec != null -> decodeSpec(spec, container.entries)
            looksLikeJson(bytes) -> decodeSpec(bytes, emptyMap())
            else -> decodeBinary(bytes, emptyMap())
        }
    }

    private fun decodeBinary(binary: ByteArray, entries: Map<String, ByteArray>): SvgaMovie {
        val movie = ProtoBuf.decodeFromByteArray(ProtoMovieEntity.serializer(), binary)
        val audioKeys = movie.audios.mapNotNull { it.audioKey }.toSet()
        val bitmapAssets = linkedMapOf<String, ByteArray>()
        val audioAssets = linkedMapOf<String, SvgaAudioAsset>()
        movie.images.forEach { (key, value) ->
            val resolved = resolveEmbeddedAsset(value, key, entries)
            if (resolved == null) return@forEach
            if (key in audioKeys || looksLikeAudio(resolved)) {
                val audio = movie.audios.firstOrNull { it.audioKey == key }
                audioAssets[key] = SvgaAudioAsset(
                    key = key,
                    bytes = resolved,
                    startFrame = audio?.startFrame ?: 0,
                    endFrame = audio?.endFrame ?: 0,
                    startTimeMillis = audio?.startTime ?: 0,
                    totalTimeMillis = audio?.totalTime ?: 0,
                )
            } else {
                bitmapAssets[normalizeBitmapAssetKey(key)] = resolved
            }
        }
        return SvgaMovie(
            version = movie.version.orEmpty(),
            width = movie.params?.viewBoxWidth ?: 0f,
            height = movie.params?.viewBoxHeight ?: 0f,
            fps = movie.params?.fps ?: 0,
            frames = movie.params?.frames ?: 0,
            sprites = movie.sprites.map { it.toSprite() },
            bitmapAssets = bitmapAssets,
            audioAssets = audioAssets,
        )
    }

    private fun decodeSpec(spec: ByteArray, entries: Map<String, ByteArray>): SvgaMovie {
        val root = json.parseToJsonElement(spec.decodeToString()).jsonObject
        val movie = root["movie"]?.jsonObject.orEmpty()
        val viewBox = movie["viewBox"]?.jsonObject.orEmpty()
        val audios = root["audios"]?.jsonArray.orEmpty()
        val audioList = audios.map { it.jsonObject }
        val audioAssets = linkedMapOf<String, SvgaAudioAsset>()
        val bitmapAssets = linkedMapOf<String, ByteArray>()
        root["images"]?.jsonObject.orEmpty().forEach { (key, element) ->
            val reference = element.jsonPrimitive.stringContentOrNull().orEmpty()
            val bytes = findZipAsset(reference, key, entries) ?: return@forEach
            val audio = audioList.firstOrNull { it["audioKey"].stringOrNull() == key }
            if (audio != null || looksLikeAudio(bytes)) {
                audioAssets[key] = SvgaAudioAsset(
                    key = key,
                    bytes = bytes,
                    startFrame = audio?.get("startFrame").intOrZero(),
                    endFrame = audio?.get("endFrame").intOrZero(),
                    startTimeMillis = audio?.get("startTime").intOrZero(),
                    totalTimeMillis = audio?.get("totalTime").intOrZero(),
                )
            } else {
                bitmapAssets[normalizeBitmapAssetKey(key)] = bytes
            }
        }
        return SvgaMovie(
            version = root["ver"].stringOrNull().orEmpty(),
            width = viewBox["width"].floatOrZero(),
            height = viewBox["height"].floatOrZero(),
            fps = movie["fps"].intOrZero(),
            frames = movie["frames"].intOrZero(),
            sprites = root["sprites"]?.jsonArray.orEmpty().map { it.jsonObject.toSprite() },
            bitmapAssets = bitmapAssets,
            audioAssets = audioAssets,
        )
    }

    private fun ProtoSpriteEntity.toSprite(): SvgaSprite {
        val resolvedFrames = ArrayList<SvgaFrame>(frames.size)
        var lastShapes: List<SvgaShape> = emptyList()
        frames.forEach { frame ->
            val decodedShapes = frame.shapes.map { it.toShape() }
            val finalShapes = if (decodedShapes.firstOrNull()?.type == SvgaShapeType.Keep && lastShapes.isNotEmpty()) {
                lastShapes
            } else {
                decodedShapes
            }
            resolvedFrames += SvgaFrame(
                alpha = frame.alpha ?: 0f,
                layout = frame.layout.toLayout(),
                transform = frame.transform.toTransform(),
                clipPathData = frame.clipPath?.takeIf { it.isNotBlank() },
                shapes = finalShapes,
            )
            lastShapes = finalShapes
        }
        return SvgaSprite(
            imageKey = imageKey.orEmpty(),
            matteKey = matteKey?.takeIf { it.isNotBlank() },
            frames = resolvedFrames,
        )
    }

    private fun JsonObject.toSprite(): SvgaSprite {
        val resolvedFrames = ArrayList<SvgaFrame>()
        var lastShapes: List<SvgaShape> = emptyList()
        this["frames"]?.jsonArray.orEmpty().forEach { frameElement ->
            val frame = frameElement.jsonObject
            val decodedShapes = frame["shapes"]?.jsonArray.orEmpty().map { it.jsonObject.toShape() }
            val finalShapes = if (decodedShapes.firstOrNull()?.type == SvgaShapeType.Keep && lastShapes.isNotEmpty()) {
                lastShapes
            } else {
                decodedShapes
            }
            resolvedFrames += SvgaFrame(
                alpha = frame["alpha"].floatOrZero(),
                layout = frame["layout"]?.jsonObject.toLayout(),
                transform = frame["transform"]?.jsonObject.toTransform(),
                clipPathData = frame["clipPath"].stringOrNull(),
                shapes = finalShapes,
            )
            lastShapes = finalShapes
        }
        return SvgaSprite(
            imageKey = this["imageKey"].stringOrNull().orEmpty(),
            matteKey = this["matteKey"].stringOrNull()?.takeIf { it.isNotBlank() },
            frames = resolvedFrames,
        )
    }

    private fun ProtoFrameEntity?.toLayout(): SvgaLayout = this?.layout.toLayout()

    private fun ProtoLayout?.toLayout(): SvgaLayout = SvgaLayout(
        x = this?.x ?: 0f,
        y = this?.y ?: 0f,
        width = this?.width ?: 0f,
        height = this?.height ?: 0f,
    )

    private fun JsonObject?.toLayout(): SvgaLayout = SvgaLayout(
        x = this?.get("x").floatOrZero(),
        y = this?.get("y").floatOrZero(),
        width = this?.get("width").floatOrZero(),
        height = this?.get("height").floatOrZero(),
    )

    private fun ProtoTransform?.toTransform(): SvgaTransform = SvgaTransform(
        a = this?.a ?: 1f,
        b = this?.b ?: 0f,
        c = this?.c ?: 0f,
        d = this?.d ?: 1f,
        tx = this?.tx ?: 0f,
        ty = this?.ty ?: 0f,
    )

    private fun JsonObject?.toTransform(): SvgaTransform = SvgaTransform(
        a = this?.get("a").floatOrDefault(1f),
        b = this?.get("b").floatOrZero(),
        c = this?.get("c").floatOrZero(),
        d = this?.get("d").floatOrDefault(1f),
        tx = this?.get("tx").floatOrZero(),
        ty = this?.get("ty").floatOrZero(),
    )

    private fun ProtoShapeEntity.toShape(): SvgaShape = SvgaShape(
        type = when (type) {
            ProtoShapeType.RECT -> SvgaShapeType.Rect
            ProtoShapeType.ELLIPSE -> SvgaShapeType.Ellipse
            ProtoShapeType.KEEP -> SvgaShapeType.Keep
            else -> SvgaShapeType.Shape
        },
        data = when {
            shape?.d != null -> SvgaShapeData.PathData(shape.d)
            rect != null -> SvgaShapeData.RectData(
                x = rect.x ?: 0f,
                y = rect.y ?: 0f,
                width = rect.width ?: 0f,
                height = rect.height ?: 0f,
                cornerRadius = rect.cornerRadius ?: 0f,
            )
            ellipse != null -> SvgaShapeData.EllipseData(
                x = ellipse.x ?: 0f,
                y = ellipse.y ?: 0f,
                radiusX = ellipse.radiusX ?: 0f,
                radiusY = ellipse.radiusY ?: 0f,
            )
            else -> null
        },
        style = styles?.toShapeStyle(),
        transform = transform.toTransform(),
    )

    private fun JsonObject.toShape(): SvgaShape {
        val args = this["args"]?.jsonObject.orEmpty()
        return SvgaShape(
            type = when (this["type"].stringOrNull()?.lowercase()) {
                "rect" -> SvgaShapeType.Rect
                "ellipse" -> SvgaShapeType.Ellipse
                "keep" -> SvgaShapeType.Keep
                else -> SvgaShapeType.Shape
            },
            data = when (this["type"].stringOrNull()?.lowercase()) {
                "rect" -> SvgaShapeData.RectData(
                    x = args["x"].floatOrZero(),
                    y = args["y"].floatOrZero(),
                    width = args["width"].floatOrZero(),
                    height = args["height"].floatOrZero(),
                    cornerRadius = args["cornerRadius"].floatOrZero(),
                )
                "ellipse" -> SvgaShapeData.EllipseData(
                    x = args["x"].floatOrZero(),
                    y = args["y"].floatOrZero(),
                    radiusX = args["radiusX"].floatOrZero(),
                    radiusY = args["radiusY"].floatOrZero(),
                )
                "keep" -> null
                else -> SvgaShapeData.PathData(args["d"].stringOrNull().orEmpty())
            },
            style = this["styles"]?.jsonObject.toShapeStyle(),
            transform = this["transform"]?.jsonObject.toTransform(),
        )
    }

    private fun ProtoShapeStyle.toShapeStyle(): SvgaShapeStyle = SvgaShapeStyle(
        fill = fill.toColor(),
        stroke = stroke.toColor(),
        strokeWidth = strokeWidth ?: 0f,
        lineCap = when (lineCap) {
            ProtoLineCap.LineCap_ROUND -> SvgaLineCap.Round
            ProtoLineCap.LineCap_SQUARE -> SvgaLineCap.Square
            else -> SvgaLineCap.Butt
        },
        lineJoin = when (lineJoin) {
            ProtoLineJoin.LineJoin_BEVEL -> SvgaLineJoin.Bevel
            ProtoLineJoin.LineJoin_ROUND -> SvgaLineJoin.Round
            else -> SvgaLineJoin.Miter
        },
        miterLimit = miterLimit ?: 0f,
        lineDash = floatArrayOf(
            lineDashI ?: 0f,
            lineDashII ?: 0f,
            lineDashIII ?: 0f,
        ).filter { it > 0f }.toFloatArray(),
    )

    private fun JsonObject?.toShapeStyle(): SvgaShapeStyle? {
        val style = this ?: return null
        return SvgaShapeStyle(
            fill = style["fill"]?.jsonArray.toColor(),
            stroke = style["stroke"]?.jsonArray.toColor(),
            strokeWidth = style["strokeWidth"].floatOrZero(),
            lineCap = when (style["lineCap"].stringOrNull()?.lowercase()) {
                "round" -> SvgaLineCap.Round
                "square" -> SvgaLineCap.Square
                else -> SvgaLineCap.Butt
            },
            lineJoin = when (style["lineJoin"].stringOrNull()?.lowercase()) {
                "bevel" -> SvgaLineJoin.Bevel
                "round" -> SvgaLineJoin.Round
                else -> SvgaLineJoin.Miter
            },
            miterLimit = style["miterLimit"].floatOrZero(),
            lineDash = style["lineDash"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.floatOrNull }.toFloatArray(),
        )
    }

    private fun ProtoRgbaColor?.toColor(): Color? {
        if (this == null) return null
        return rgbaToColor(r, g, b, a)
    }

    private fun JsonArray?.toColor(): Color? {
        if (this == null || size < 4) return null
        return rgbaToColor(
            this[0].jsonPrimitive.floatOrNull,
            this[1].jsonPrimitive.floatOrNull,
            this[2].jsonPrimitive.floatOrNull,
            this[3].jsonPrimitive.floatOrNull,
        )
    }

    private fun rgbaToColor(r: Float?, g: Float?, b: Float?, a: Float?): Color {
        val rgbScale = if ((r ?: 0f) <= 1f && (g ?: 0f) <= 1f && (b ?: 0f) <= 1f) 1f else 255f
        val alphaScale = if ((a ?: 0f) <= 1f) 1f else 255f
        return Color(
            red = ((r ?: 0f) / rgbScale).coerceIn(0f, 1f),
            green = ((g ?: 0f) / rgbScale).coerceIn(0f, 1f),
            blue = ((b ?: 0f) / rgbScale).coerceIn(0f, 1f),
            alpha = ((a ?: 0f) / alphaScale).coerceIn(0f, 1f),
        )
    }

    private fun resolveEmbeddedAsset(value: ByteArray, key: String, entries: Map<String, ByteArray>): ByteArray? {
        if (value.size < 4) return null
        return if (looksLikeReferencedFile(value)) {
            val reference = value.decodeToString().trim('\u0000')
            findZipAsset(reference, key, entries)
        } else {
            value
        }
    }

    private fun findZipAsset(reference: String, key: String, entries: Map<String, ByteArray>): ByteArray? {
        val candidates = buildList {
            if (reference.isNotBlank()) {
                add(reference)
                add("$reference.png")
            }
            add("$key.png")
            add(key)
            add(normalizeBitmapAssetKey(key))
            add("${normalizeBitmapAssetKey(key)}.png")
        }
        return candidates.firstNotNullOfOrNull { candidate ->
            entries.entries.firstOrNull { it.key.substringAfterLast('/') == candidate }?.value
        }
    }

    private fun unpackContainer(bytes: ByteArray): SvgaContainer {
        return when {
            bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() -> {
                SvgaContainer(parseZipEntries(bytes))
            }
            looksLikeZlib(bytes) -> SvgaContainer(mapOf("movie.binary" to inflateZlib(bytes)))
            else -> SvgaContainer(mapOf("movie.binary" to bytes))
        }
    }

    private fun parseZipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val endOffset = findEndOfCentralDirectory(bytes)
        val centralDirectoryOffset = bytes.readIntLe(endOffset + 16)
        val entryCount = bytes.readShortLe(endOffset + 10)
        var offset = centralDirectoryOffset
        val entries = linkedMapOf<String, ByteArray>()
        repeat(entryCount) {
            require(bytes.readIntLe(offset) == ZIP_CENTRAL_HEADER) { "Invalid zip central directory." }
            val compressionMethod = bytes.readShortLe(offset + 10)
            val compressedSize = bytes.readIntLe(offset + 20)
            val uncompressedSize = bytes.readIntLe(offset + 24)
            val fileNameLength = bytes.readShortLe(offset + 28)
            val extraLength = bytes.readShortLe(offset + 30)
            val commentLength = bytes.readShortLe(offset + 32)
            val localHeaderOffset = bytes.readIntLe(offset + 42)
            val fileName = bytes.readString(offset + 46, fileNameLength)
            offset += 46 + fileNameLength + extraLength + commentLength
            if (fileName.endsWith('/')) return@repeat
            val localNameLength = bytes.readShortLe(localHeaderOffset + 26)
            val localExtraLength = bytes.readShortLe(localHeaderOffset + 28)
            val dataStart = localHeaderOffset + 30 + localNameLength + localExtraLength
            val compressed = bytes.copyOfRange(dataStart, dataStart + compressedSize)
            val data = when (compressionMethod) {
                0 -> compressed
                8 -> inflateRawDeflate(compressed, uncompressedSize)
                else -> error("Unsupported zip compression method: $compressionMethod")
            }
            entries[fileName] = data
        }
        return entries
    }

    private fun findEndOfCentralDirectory(bytes: ByteArray): Int {
        val minOffset = (bytes.size - 0xFFFF - 22).coerceAtLeast(0)
        var index = bytes.size - 22
        while (index >= minOffset) {
            if (bytes.readIntLe(index) == ZIP_END_OF_CENTRAL_DIRECTORY) return index
            index -= 1
        }
        error("Invalid zip archive: end of central directory not found.")
    }

    private fun looksLikeReferencedFile(bytes: ByteArray): Boolean {
        if (looksLikeAudio(bytes) || looksLikePng(bytes) || looksLikeJpeg(bytes) || looksLikeWebp(bytes)) return false
        return bytes.all { it == 0.toByte() || it in 32..126 }
    }

    private fun looksLikeJson(bytes: ByteArray): Boolean = bytes.firstOrNull()?.let { it == '{'.code.toByte() } == true

    private fun looksLikeZlib(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        val header = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
        return header % 31 == 0 && (bytes[0].toInt() and 0x0F) == 8
    }

    private fun looksLikeAudio(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) ||
            (bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0)
    }

    private fun looksLikePng(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        return png.indices.all { bytes[it] == png[it] }
    }

    private fun looksLikeJpeg(bytes: ByteArray): Boolean {
        return bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
    }

    private fun looksLikeWebp(bytes: ByteArray): Boolean {
        return bytes.size >= 12 && bytes.readString(0, 4) == "RIFF" && bytes.readString(8, 4) == "WEBP"
    }

    private fun ByteArray.readIntLe(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun ByteArray.readShortLe(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun ByteArray.readString(offset: Int, length: Int): String {
        return copyOfRange(offset, offset + length).decodeToString()
    }

    private fun JsonObject?.orEmpty(): JsonObject = this ?: JsonObject(emptyMap())

    private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())

    private fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.stringContentOrNull()

    private fun JsonPrimitive.stringContentOrNull(): String? = runCatching { content }.getOrNull()

    private fun JsonElement?.floatOrZero(): Float = (this as? JsonPrimitive)?.floatOrNull ?: 0f

    private fun JsonElement?.floatOrDefault(default: Float): Float = (this as? JsonPrimitive)?.floatOrNull ?: default

    private fun JsonElement?.intOrZero(): Int = (this as? JsonPrimitive)?.intOrNull ?: 0
}

private data class SvgaContainer(
    val entries: Map<String, ByteArray>,
) {
    fun find(name: String): ByteArray? {
        return entries.entries.firstOrNull { it.key.substringAfterLast('/') == name }?.value
    }
}

private const val ZIP_CENTRAL_HEADER = 0x02014B50
private const val ZIP_END_OF_CENTRAL_DIRECTORY = 0x06054B50

internal expect fun inflateZlib(bytes: ByteArray): ByteArray

internal expect fun inflateRawDeflate(bytes: ByteArray, expectedSize: Int): ByteArray
