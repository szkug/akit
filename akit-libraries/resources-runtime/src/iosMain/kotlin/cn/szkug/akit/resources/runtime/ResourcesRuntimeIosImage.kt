@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.szkug.akit.resources.runtime

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cn.szkug.akit.graph.ninepatch.ImageBitmapNinePatchSource
import cn.szkug.akit.graph.ninepatch.NinePatchChunk
import cn.szkug.akit.graph.ninepatch.NinePatchPainter
import cn.szkug.akit.graph.ninepatch.NinePatchType
import cn.szkug.akit.graph.ninepatch.parseNinePatch
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import platform.UIKit.UIScreen

/**
 * Image loading and XML vector parsing for iOS resources.
 *
 * Responsibility: resolve localized image paths, decode bitmaps, parse XML vectors, and
 * apply nine-patch metadata when needed.
 *
 * Performance: scale candidates are cached; vector parsing is expected to be wrapped by
 * `remember` at the call site to avoid repeat XML parsing.
 */
internal object ImageIos {
    private val scaleCandidatesCache = mutableMapOf<String, List<String>>()
    private val vectorTagRegex = Regex("<vector\\b[^>]*>")
    private val pathTagRegex = Regex("<path\\b[^>]*>")
    private val attrRegex = Regex("""([A-Za-z_][A-Za-z0-9_:\-.]+)\s*=\s*["']([^"']*)["']""")

    fun resolveImagePath(
        bundle: NSBundle,
        prefix: String,
        path: ResourcePath,
        locales: List<String>,
    ): String? {
        val nameCandidates = buildScaleCandidates(path.name)
        for (locale in locales) {
            val directory = ResourcePathsIos.localizedResourceDirectory(prefix, path.directory, locale)
            for (name in nameCandidates) {
                val resolved = ResourcePathsIos.pathForResource(bundle, name, path.extension, directory)
                if (resolved != null) return resolved
            }
        }
        val attemptedPaths = mutableListOf<String>()
        for (locale in locales) {
            val directory = ResourcePathsIos.localizedResourceDirectory(prefix, path.directory, locale)
            for (name in nameCandidates) {
                attemptedPaths += ResourcePathsIos.fullResourcePath(bundle, directory, name, path.extension)
            }
        }
        if (attemptedPaths.isNotEmpty()) {
            ResourcePathsIos.logMissingResource("image", attemptedPaths)
        }
        return null
    }

    fun loadPainter(
        bundle: NSBundle,
        prefix: String,
        path: ResourcePath,
        locales: List<String>,
    ): Painter? {
        val filePath = resolveImagePath(bundle, prefix, path, locales) ?: return null
        val bitmap = loadImageBitmapAtPath(filePath)
        val ninePatchSource = ImageBitmapNinePatchSource(bitmap)
        val parsed = parseNinePatch(ninePatchSource, null)
        return if (parsed.type == NinePatchType.Raw) {
            val chunk = parsed.chunk ?: NinePatchChunk.createEmptyChunk()
            val cropped = cropNinePatch(bitmap)
            NinePatchPainter(cropped, chunk)
        } else {
            BitmapPainter(bitmap)
        }
    }

    fun parseVectorXml(
        xml: String,
        bundle: NSBundle,
        prefix: String,
        locales: List<String>,
        density: Density,
    ): ImageVector? {
        val vectorTag = vectorTagRegex.find(xml)?.value ?: return null
        val vectorAttrs = parseXmlAttributes(vectorTag)
        val viewportWidth = parseFloatAttr(vectorAttrs, "android:viewportWidth", "viewportWidth") ?: return null
        val viewportHeight = parseFloatAttr(vectorAttrs, "android:viewportHeight", "viewportHeight") ?: return null
        val defaultWidth = parseVectorDimen(vectorAttrs["android:width"] ?: vectorAttrs["width"], density)
            ?: viewportWidth.dp
        val defaultHeight = parseVectorDimen(vectorAttrs["android:height"] ?: vectorAttrs["height"], density)
            ?: viewportHeight.dp
        val builder = ImageVector.Builder(
            defaultWidth = defaultWidth,
            defaultHeight = defaultHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
        )
        for (match in pathTagRegex.findAll(xml)) {
            val attrs = parseXmlAttributes(match.value)
            val pathData = attrs["android:pathData"] ?: attrs["pathData"] ?: continue
            val nodes = runCatching { PathParser().parsePathString(pathData).toNodes() }.getOrNull() ?: continue
            val fillColor = resolveVectorColor(attrs["android:fillColor"] ?: attrs["fillColor"], bundle, prefix, locales)
            val strokeColor = resolveVectorColor(attrs["android:strokeColor"] ?: attrs["strokeColor"], bundle, prefix, locales)
            val fill = fillColor?.let { SolidColor(it) }
            val stroke = strokeColor?.let { SolidColor(it) }
            val fillAlpha = parseFloat(attrs["android:fillAlpha"] ?: attrs["fillAlpha"]) ?: 1f
            val strokeAlpha = parseFloat(attrs["android:strokeAlpha"] ?: attrs["strokeAlpha"]) ?: 1f
            val strokeWidth = parseFloat(attrs["android:strokeWidth"] ?: attrs["strokeWidth"]) ?: 0f
            val strokeCap = parseStrokeCap(attrs["android:strokeLineCap"] ?: attrs["strokeLineCap"])
            val strokeJoin = parseStrokeJoin(attrs["android:strokeLineJoin"] ?: attrs["strokeLineJoin"])
            val strokeMiter = parseFloat(attrs["android:strokeMiterLimit"] ?: attrs["strokeMiterLimit"]) ?: 4f
            val fillType = parseFillType(attrs["android:fillType"] ?: attrs["fillType"])
            builder.addPath(
                pathData = nodes,
                pathFillType = fillType,
                fill = fill,
                fillAlpha = fillAlpha,
                stroke = stroke,
                strokeAlpha = strokeAlpha,
                strokeLineWidth = strokeWidth,
                strokeLineCap = strokeCap,
                strokeLineJoin = strokeJoin,
                strokeLineMiter = strokeMiter,
            )
        }
        return builder.build()
    }

    fun loadTextAtPath(filePath: String): String? {
        val data = NSData.dataWithContentsOfFile(filePath) ?: return null
        val bytes = data.toByteArray()
        if (bytes.isEmpty()) return null
        return bytes.decodeToString()
    }

    fun loadImageBitmapAtPath(filePath: String): ImageBitmap {
        val data = NSData.dataWithContentsOfFile(filePath)
            ?: error("Failed to load image data: path=$filePath")
        val bytes = data.toByteArray()
        if (bytes.isEmpty()) {
            error("Image data is empty: path=$filePath")
        }
        val image = Image.makeFromEncoded(bytes)
        return image.toComposeImageBitmap()
    }

    private fun parseXmlAttributes(tag: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (match in attrRegex.findAll(tag)) {
            out[match.groupValues[1]] = match.groupValues[2]
        }
        return out
    }

    private fun parseFloatAttr(attrs: Map<String, String>, vararg keys: String): Float? {
        for (key in keys) {
            val value = parseFloat(attrs[key])
            if (value != null) return value
        }
        return null
    }

    private fun parseFloat(raw: String?): Float? = raw?.trim()?.toFloatOrNull()

    private fun parseVectorDimen(raw: String?, density: Density): Dp? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("""^([+-]?\d+(?:\.\d+)?)([a-zA-Z]+)?$""").find(raw.trim()) ?: return null
        val value = match.groupValues[1].toFloatOrNull() ?: return null
        val unit = match.groupValues.getOrNull(2).orEmpty().lowercase()
        return when (unit) {
            "", "dp", "dip" -> value.dp
            "px" -> (value / density.density).dp
            else -> value.dp
        }
    }

    private fun resolveVectorColor(
        raw: String?,
        bundle: NSBundle,
        prefix: String,
        locales: List<String>,
    ): Color? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val color = if (trimmed.startsWith("@color/")) {
            val key = trimmed.removePrefix("@color/")
            val value = LocalizationIos.loadLocalizedValueOrThrow(
                bundle,
                prefix,
                key,
                locales,
                LocalizationIos.valuesColorsFile,
                "color",
            )
            ColorIos.parseColor(value)
        } else {
            ColorIos.parseColor(trimmed)
        }
        return if (color == Color.Unspecified) null else color
    }

    private fun parseStrokeCap(raw: String?): StrokeCap {
        return when (raw?.trim()?.lowercase()) {
            "round" -> StrokeCap.Round
            "square" -> StrokeCap.Square
            else -> StrokeCap.Butt
        }
    }

    private fun parseStrokeJoin(raw: String?): StrokeJoin {
        return when (raw?.trim()?.lowercase()) {
            "round" -> StrokeJoin.Round
            "bevel" -> StrokeJoin.Bevel
            else -> StrokeJoin.Miter
        }
    }

    private fun parseFillType(raw: String?): PathFillType {
        return when (raw?.trim()?.lowercase()) {
            "evenodd" -> PathFillType.EvenOdd
            else -> PathFillType.NonZero
        }
    }

    private fun buildScaleCandidates(name: String): List<String> {
        return scaleCandidatesCache.getOrPut(name) {
            val (baseName, hasScaleSuffix) = splitScaleSuffix(name)
            if (hasScaleSuffix) {
                return@getOrPut listOf(name, baseName).distinct()
            }
            val candidates = mutableListOf<String>()
            for (scale in preferredScales()) {
                candidates += applyScaleSuffix(baseName, scale)
            }
            candidates += baseName
            candidates.distinct()
        }
    }

    private fun splitScaleSuffix(name: String): Pair<String, Boolean> {
        val ninePatch = name.endsWith(".9")
        val base = if (ninePatch) name.removeSuffix(".9") else name
        val match = Regex("@[23]x$").find(base)
        if (match == null) return name to false
        val stripped = base.removeSuffix(match.value)
        val restored = if (ninePatch) "$stripped.9" else stripped
        return restored to true
    }

    private fun applyScaleSuffix(name: String, scale: Int): String {
        val suffix = "@${scale}x"
        return if (name.endsWith(".9")) {
            val base = name.removeSuffix(".9")
            "$base$suffix.9"
        } else {
            "$name$suffix"
        }
    }

    private fun preferredScales(): List<Int> {
        val scale = UIScreen.mainScreen.scale
        return when {
            scale >= 3.0 -> listOf(3, 2)
            scale >= 2.0 -> listOf(2)
            else -> emptyList()
        }
    }

    private fun cropNinePatch(image: ImageBitmap): ImageBitmap {
        val contentWidth = (image.width - 2).coerceAtLeast(1)
        val contentHeight = (image.height - 2).coerceAtLeast(1)
        val out = ImageBitmap(width = contentWidth, height = contentHeight)
        val canvas = Canvas(out)
        val paint = Paint()
        val srcOffset = IntOffset(1, 1)
        val srcSize = IntSize(contentWidth, contentHeight)
        val dstOffset = IntOffset(0, 0)
        val dstSize = IntSize(contentWidth, contentHeight)
        canvas.drawImageRect(image, srcOffset, srcSize, dstOffset, dstSize, paint)
        return out
    }
}

/**
 * Shared NSData -> ByteArray conversion for localized table loading.
 */
internal fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val source = this.bytes ?: return ByteArray(0)
    val buffer = ByteArray(length)
    buffer.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, this.length)
    }
    return buffer
}
