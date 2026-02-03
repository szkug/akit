@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.szkug.akit.resources.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import platform.UIKit.UIScreen

abstract class NSResourceId {
    abstract val path: String?
}
class NSURLResourceId(val url: NSURL): NSResourceId() {
    override val path: String?
        get() = url.path
}

actual typealias ResourceId = NSResourceId
actual typealias StringResourceId = NSURLResourceId
actual typealias PluralStringResourceId = NSURLResourceId
actual typealias ColorResourceId = NSURLResourceId
actual typealias RawResourceId = NSURLResourceId
actual typealias PaintableResourceId = NSURLResourceId
actual typealias DimenResourceId = NSURLResourceId

private data class ResourceInfo(
    val prefix: String,
    val value: String,
)

private data class ResourcePath(
    val directory: String,
    val name: String,
    val extension: String,
)

private enum class DimenUnit {
    NONE,
    PX,
    DP,
    SP,
    IN,
    MM,
    PT,
}

private data class DimenValue(
    val value: Float,
    val unit: DimenUnit,
)

private val preferredLocalesCache = mutableMapOf<String, List<String>>()
private val scaleCandidatesCache = mutableMapOf<String, List<String>>()
private val valuesTableCache = mutableMapOf<String, Map<String, String>>()
private val dimenTableCache = mutableMapOf<String, Map<String, DimenValue>>()
private val pluralTableCache = mutableMapOf<String, Map<String, Map<String, String>>>()

@Composable
actual fun stringResource(id: StringResourceId, vararg formatArgs: Any): String {
    val info = decodeResourceId(id)
    val bundle = NSBundle.mainBundle
    val locales = preferredLocales(bundle, userDefaultsLanguage())
    val raw = loadLocalizedString(bundle, info.prefix, info.value, locales)
    return formatString(raw, formatArgs)
}

@Composable
actual fun pluralStringResource(
    id: PluralStringResourceId,
    count: Int,
    vararg formatArgs: Any
): String {
    val info = decodeResourceId(id)
    val bundle = NSBundle.mainBundle
    val locales = preferredLocales(bundle, userDefaultsLanguage())
    val raw = loadLocalizedPlural(bundle, info.prefix, info.value, locales, count)
    return formatString(raw, formatArgs)
}

@Composable
actual fun colorResource(id: ColorResourceId): Color {
    val info = decodeResourceId(id)
    val bundle = NSBundle.mainBundle
    val locales = preferredLocales(bundle, userDefaultsLanguage())
    val raw = loadLocalizedValue(bundle, info.prefix, info.value, locales, valuesColorsFile)
    return parseColor(raw)
}

@Composable
actual fun painterResource(id: PaintableResourceId): Painter {
    val localeOverride = userDefaultsLanguage()
    val density = LocalDensity.current
    val info = decodeResourceId(id)
    val path = parseResourcePath(info.value)
    val bundle = NSBundle.mainBundle
    val locales = preferredLocales(bundle, localeOverride)
    return if (path.extension.equals("xml", ignoreCase = true)) {
        val vector = remember(id, localeOverride, density.density, density.fontScale) {
            val filePath = resolveImagePath(bundle, info.prefix, path, locales)
            val xml = filePath?.let { loadTextAtPath(it) }
            xml?.let { parseVectorXml(it, bundle, info.prefix, locales, density) }
        } ?: error("Failed to resolve vector path: name=${path.name} extension=${path.extension} directory=${path.directory}")
        rememberVectorPainter(vector)
    } else {
        remember(id, localeOverride) {
            val painter = loadPainter(bundle, info.prefix, path, locales)
            if (painter != null) return@remember painter
            error("Failed to resolve image path: name=${path.name} extension=${path.extension} directory=${path.directory}")
        }
    }
}

actual fun resolveResourcePath(id: ResourceId, localeOverride: String?): String? {
    val info = decodeResourceId(id)
    if (info.value.isBlank()) return null
    val path = parseResourcePath(info.value)
    val bundle = NSBundle.mainBundle
    val locales = preferredLocales(bundle, localeOverride ?: userDefaultsLanguage())
    return resolveImagePath(bundle, info.prefix, path, locales)
}

@get:Composable
actual val DimenResourceId.toDp: Dp
    get() {
        val info = decodeResourceId(this)
        val bundle = NSBundle.mainBundle
        val locales = preferredLocales(bundle, userDefaultsLanguage())
        val value = loadLocalizedDimen(bundle, info.prefix, info.value, locales)
        return value.toDp(LocalDensity.current)
    }

@get:Composable
actual val DimenResourceId.toSp: TextUnit
    get() {
        val info = decodeResourceId(this)
        val bundle = NSBundle.mainBundle
        val locales = preferredLocales(bundle, userDefaultsLanguage())
        val value = loadLocalizedDimen(bundle, info.prefix, info.value, locales)
        return value.toSp(LocalDensity.current)
    }


fun <T: ResourceId> resourceId(prefix: String, value: String): T {
    val url = if (prefix.isBlank()) NSURL.fileURLWithPath(value)
    else NSURL.fileURLWithPath("$prefix|$value")
    return NSURLResourceId(url) as T
}

private fun decodeResourceId(id: ResourceId): ResourceInfo {
    val rawPath = id.path?.trimStart('/') ?: ""
    val decodedPath = rawPath.replace("%7C", "|").replace("%7c", "|")
    val parts = decodedPath.split('|', limit = 3)
    val (prefix, value) = when (parts.size) {
        3 -> parts[1] to parts[2]
        2 -> parts[0] to parts[1]
        else -> "" to parts.getOrNull(0).orEmpty()
    }
    return ResourceInfo(prefix, value)
}

private fun parseResourcePath(value: String): ResourcePath {
    val normalized = value.trimStart('/')
    val directory = normalized.substringBeforeLast('/', "")
    val fileName = normalized.substringAfterLast('/', normalized)
    val name = fileName.substringBeforeLast('.', fileName)
    val extension = fileName.substringAfterLast('.', "")
    if (extension.isBlank()) {
        error("Missing extension in resource path: $value")
    }
    return ResourcePath(directory = directory, name = name, extension = extension)
}

private fun loadLocalizedString(
    bundle: NSBundle,
    prefix: String,
    key: String,
    locales: List<String>,
): String {
    val attemptedPaths = mutableListOf<String>()
    for (locale in locales) {
        val table = loadStringTable(bundle, prefix, locale)
        val value = table?.get(key)
        if (value != null) return value
        attemptedPaths += fullResourcePath(
            bundle,
            localizedResourceDirectory(prefix, "", locale),
            "Localizable",
            "strings"
        )
    }
    if (attemptedPaths.isNotEmpty()) {
        logMissingResource("string table", attemptedPaths)
    }
    return ""
}

private fun loadLocalizedValue(
    bundle: NSBundle,
    prefix: String,
    key: String,
    locales: List<String>,
    fileBase: String,
): String {
    val attemptedPaths = mutableListOf<String>()
    for (locale in locales) {
        val table = loadValuesTable(bundle, prefix, locale, fileBase)
        val value = table?.get(key)
        if (value != null) return value
        attemptedPaths += fullResourcePath(
            bundle,
            localizedResourceDirectory(prefix, valuesDirectoryName, locale),
            fileBase,
            valuesFileExtension,
        )
    }
    if (attemptedPaths.isNotEmpty()) {
        logMissingResource("$fileBase table", attemptedPaths)
    }
    return ""
}

private fun loadLocalizedDimen(
    bundle: NSBundle,
    prefix: String,
    key: String,
    locales: List<String>,
): DimenValue {
    val attemptedPaths = mutableListOf<String>()
    for (locale in locales) {
        val table = loadDimenTable(bundle, prefix, locale)
        val value = table?.get(key)
        if (value != null) return value
        attemptedPaths += fullResourcePath(
            bundle,
            localizedResourceDirectory(prefix, valuesDirectoryName, locale),
            valuesDimensFile,
            valuesFileExtension,
        )
    }
    if (attemptedPaths.isNotEmpty()) {
        logMissingResource("dimens table", attemptedPaths)
    }
    return DimenValue(0f, DimenUnit.NONE)
}

private fun loadLocalizedPlural(
    bundle: NSBundle,
    prefix: String,
    key: String,
    locales: List<String>,
    count: Int,
): String {
    val attemptedPaths = mutableListOf<String>()
    for (locale in locales) {
        val table = loadPluralTable(bundle, prefix, locale)
        val entries = table?.get(key)
        if (entries != null) {
            return selectPlural(entries, count)
        }
        attemptedPaths += fullResourcePath(
            bundle,
            localizedResourceDirectory(prefix, valuesDirectoryName, locale),
            valuesPluralsFile,
            valuesFileExtension,
        )
    }
    if (attemptedPaths.isNotEmpty()) {
        logMissingResource("plurals table", attemptedPaths)
    }
    return ""
}

private val stringTableCache = mutableMapOf<String, Map<String, String>>()

private const val valuesDirectoryName = "values"
private const val valuesFileExtension = "txt"
private const val valuesColorsFile = "colors"
private const val valuesDimensFile = "dimens"
private const val valuesPluralsFile = "plurals"
private const val dpPerInch = 160f
private const val dpPerMm = dpPerInch / 25.4f
private const val dpPerPt = dpPerInch / 72f

private const val appLanguageKey = "akit.app.language"
private const val appleLanguagesKey = "AppleLanguages"

private fun userDefaultsLanguage(): String? {
    val defaults = NSUserDefaults.standardUserDefaults
    val direct = defaults.stringForKey(appLanguageKey)?.trim()?.takeIf { it.isNotEmpty() }
    if (direct != null) return direct
    val languages = defaults.objectForKey(appleLanguagesKey) as? List<*>
    val first = languages?.firstOrNull() as? String
    val trimmed = first?.trim()?.takeIf { it.isNotEmpty() }
    return trimmed
}


private fun loadStringTable(
    bundle: NSBundle,
    prefix: String,
    locale: String
): Map<String, String>? {
    val directory = localizedResourceDirectory(prefix, "", locale)
    val path = bundle.pathForResource("Localizable", "strings", directory) ?: return null
    return stringTableCache.getOrPut(path) {
        val data = NSData.dataWithContentsOfFile(path) ?: return@getOrPut emptyMap()
        val bytes = data.toByteArray()
        val text = bytes.decodeToString()
        parseStringsFile(text)
    }
}

private fun loadValuesTable(
    bundle: NSBundle,
    prefix: String,
    locale: String,
    fileBase: String,
): Map<String, String>? {
    val directory = localizedResourceDirectory(prefix, valuesDirectoryName, locale)
    val path = bundle.pathForResource(fileBase, valuesFileExtension, directory) ?: return null
    return valuesTableCache.getOrPut(path) {
        val data = NSData.dataWithContentsOfFile(path) ?: return@getOrPut emptyMap()
        val bytes = data.toByteArray()
        val text = bytes.decodeToString()
        parseKeyValueFile(text)
    }
}

private fun loadDimenTable(
    bundle: NSBundle,
    prefix: String,
    locale: String,
): Map<String, DimenValue>? {
    val directory = localizedResourceDirectory(prefix, valuesDirectoryName, locale)
    val path =
        bundle.pathForResource(valuesDimensFile, valuesFileExtension, directory) ?: return null
    return dimenTableCache.getOrPut(path) {
        val raw = loadValuesTable(bundle, prefix, locale, valuesDimensFile).orEmpty()
        raw.mapNotNull { (key, value) ->
            val parsed = parseDimenValue(value)
            parsed?.let { key to it }
        }.toMap()
    }
}

private fun loadPluralTable(
    bundle: NSBundle,
    prefix: String,
    locale: String,
): Map<String, Map<String, String>>? {
    val directory = localizedResourceDirectory(prefix, valuesDirectoryName, locale)
    val path =
        bundle.pathForResource(valuesPluralsFile, valuesFileExtension, directory) ?: return null
    return pluralTableCache.getOrPut(path) {
        val raw = loadValuesTable(bundle, prefix, locale, valuesPluralsFile).orEmpty()
        parsePluralTable(raw)
    }
}

private fun preferredLocales(bundle: NSBundle, overrideLocale: String?): List<String> {
    val key = buildString {
        append(bundle.bundlePath)
        append('|')
        append(overrideLocale.orEmpty())
    }
    return preferredLocalesCache.getOrPut(key) {
        val locales = if (overrideLocale.isNullOrBlank()) {
            bundle.preferredLocalizations.mapNotNull { it as? String }
        } else {
            listOf(overrideLocale)
        }
        val out = mutableListOf<String>()
        for (locale in locales) {
            val normalized = locale.replace('_', '-').trim()
            if (normalized.isBlank()) continue
            out += normalized
            val base = normalized.substringBefore('-')
            if (base.isNotBlank() && base != normalized) {
                out += base
            }
        }
        out += "Base"
        out += ""
        out.distinct()
    }
}

private val formatRegex = Regex("%(\\d+\\$)?[@sdif]")

private val stringsEntryRegex =
    Regex("\"((?:\\\\.|[^\"\\\\])*)\"\\s*=\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*;")

private fun parseStringsFile(content: String): Map<String, String> {
    if (content.isBlank()) return emptyMap()
    val out = LinkedHashMap<String, String>()
    for (match in stringsEntryRegex.findAll(content)) {
        val key = unescapeIosString(match.groupValues[1])
        val value = unescapeIosString(match.groupValues[2])
        out[key] = value
    }
    return out
}

private fun unescapeIosString(value: String): String {
    val out = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val ch = value[index]
        if (ch == '\\' && index + 1 < value.length) {
            val next = value[index + 1]
            when (next) {
                'n' -> {
                    out.append('\n')
                    index += 2
                    continue
                }

                'r' -> {
                    out.append('\r')
                    index += 2
                    continue
                }

                't' -> {
                    out.append('\t')
                    index += 2
                    continue
                }

                '"' -> {
                    out.append('"')
                    index += 2
                    continue
                }

                '\\' -> {
                    out.append('\\')
                    index += 2
                    continue
                }

                'u' -> {
                    if (index + 5 < value.length) {
                        val hex = value.substring(index + 2, index + 6)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            out.append(code.toChar())
                            index += 6
                            continue
                        }
                    }
                }
            }
        }
        out.append(ch)
        index += 1
    }
    return out.toString()
}

private fun parseKeyValueFile(content: String): Map<String, String> {
    if (content.isBlank()) return emptyMap()
    val out = LinkedHashMap<String, String>()
    val lines = content.split('\n')
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) continue
        if (trimmed.startsWith("#") || trimmed.startsWith("//")) continue
        val index = line.indexOf('=')
        if (index <= 0) continue
        val key = line.substring(0, index).trim()
        if (key.isBlank()) continue
        val rawValue = line.substring(index + 1).trimStart().trimEnd()
        out[key] = unescapeValuesFileValue(rawValue)
    }
    return out
}

private fun unescapeValuesFileValue(value: String): String {
    val out = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val ch = value[index]
        if (ch == '\\' && index + 1 < value.length) {
            val next = value[index + 1]
            when (next) {
                'n' -> {
                    out.append('\n')
                    index += 2
                    continue
                }

                'r' -> {
                    out.append('\r')
                    index += 2
                    continue
                }

                't' -> {
                    out.append('\t')
                    index += 2
                    continue
                }

                '\\' -> {
                    out.append('\\')
                    index += 2
                    continue
                }
            }
        }
        out.append(ch)
        index += 1
    }
    return out.toString()
}

private fun parseDimenValue(raw: String): DimenValue? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val match = Regex("""^([+-]?\d+(?:\.\d+)?)([a-zA-Z]+)?$""").find(trimmed) ?: return null
    val value = match.groupValues[1].toFloatOrNull() ?: return null
    val unit = match.groupValues.getOrNull(2).orEmpty().lowercase()
    val dimenUnit = when (unit) {
        "dp", "dip" -> DimenUnit.DP
        "sp" -> DimenUnit.SP
        "px" -> DimenUnit.PX
        "in" -> DimenUnit.IN
        "mm" -> DimenUnit.MM
        "pt" -> DimenUnit.PT
        else -> return null
    }
    return DimenValue(value, dimenUnit)
}

private fun parsePluralTable(raw: Map<String, String>): Map<String, Map<String, String>> {
    if (raw.isEmpty()) return emptyMap()
    val out = LinkedHashMap<String, LinkedHashMap<String, String>>()
    for ((key, value) in raw) {
        val name = key.substringBeforeLast('.', "")
        val quantity = key.substringAfterLast('.', "")
        if (name.isBlank() || quantity.isBlank()) continue
        out.getOrPut(name) { LinkedHashMap() }[quantity] = value
    }
    return out.mapValues { it.value.toMap() }
}

private fun selectPlural(entries: Map<String, String>, count: Int): String {
    val zero = entries["zero"]
    if (count == 0 && zero != null) return zero
    val one = entries["one"]
    if (count == 1 && one != null) return one
    val two = entries["two"]
    if (count == 2 && two != null) return two
    val few = entries["few"]
    if (count in 3..4 && few != null) return few
    val many = entries["many"]
    if (count >= 5 && many != null) return many
    return entries["other"] ?: entries.values.firstOrNull().orEmpty()
}

private fun parseColor(raw: String): Color {
    val trimmed = raw.trim()
    if (trimmed.isBlank() || !trimmed.startsWith("#")) return Color.Unspecified
    val hex = trimmed.removePrefix("#")
    fun hexByte(value: String): Int? = value.toIntOrNull(16)
    val argb = when (hex.length) {
        3 -> {
            val r = hexByte("${hex[0]}${hex[0]}") ?: return Color.Unspecified
            val g = hexByte("${hex[1]}${hex[1]}") ?: return Color.Unspecified
            val b = hexByte("${hex[2]}${hex[2]}") ?: return Color.Unspecified
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        4 -> {
            val a = hexByte("${hex[0]}${hex[0]}") ?: return Color.Unspecified
            val r = hexByte("${hex[1]}${hex[1]}") ?: return Color.Unspecified
            val g = hexByte("${hex[2]}${hex[2]}") ?: return Color.Unspecified
            val b = hexByte("${hex[3]}${hex[3]}") ?: return Color.Unspecified
            (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        6 -> {
            val r = hexByte(hex.substring(0, 2)) ?: return Color.Unspecified
            val g = hexByte(hex.substring(2, 4)) ?: return Color.Unspecified
            val b = hexByte(hex.substring(4, 6)) ?: return Color.Unspecified
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        8 -> {
            val a = hexByte(hex.substring(0, 2)) ?: return Color.Unspecified
            val r = hexByte(hex.substring(2, 4)) ?: return Color.Unspecified
            val g = hexByte(hex.substring(4, 6)) ?: return Color.Unspecified
            val b = hexByte(hex.substring(6, 8)) ?: return Color.Unspecified
            (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        else -> return Color.Unspecified
    }
    val packed = (argb.toLong() and 0xFFFFFFFFL).toULong()
    return Color(packed)
}

private fun formatString(template: String, formatArgs: Array<out Any?>): String {
    if (formatArgs.isEmpty()) return template
    val placeholder = "\u0000"
    val escaped = template.replace("%%", placeholder)
    var nextIndex = 0
    val replaced = formatRegex.replace(escaped) { match ->
        val rawIndex = match.groups[1]?.value
        val index = rawIndex?.dropLast(1)?.toIntOrNull()?.minus(1) ?: nextIndex++
        formatArgs.getOrNull(index)?.toString() ?: ""
    }
    return replaced.replace(placeholder, "%")
}

private fun DimenValue.toDp(density: Density): Dp = when (unit) {
    DimenUnit.NONE -> 0.dp
    DimenUnit.DP -> value.dp
    DimenUnit.PX -> (value / density.density).dp
    DimenUnit.SP -> (value * density.fontScale).dp
    DimenUnit.IN -> (value * dpPerInch).dp
    DimenUnit.MM -> (value * dpPerMm).dp
    DimenUnit.PT -> (value * dpPerPt).dp
}

private fun DimenValue.toSp(density: Density): TextUnit {
    val spValue = when (unit) {
        DimenUnit.NONE -> 0f
        DimenUnit.SP -> value
        DimenUnit.DP -> value / density.fontScale
        DimenUnit.PX -> value / (density.density * density.fontScale)
        DimenUnit.IN -> (value * dpPerInch) / density.fontScale
        DimenUnit.MM -> (value * dpPerMm) / density.fontScale
        DimenUnit.PT -> (value * dpPerPt) / density.fontScale
    }
    return spValue.sp
}

private fun loadPainter(
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

private fun loadTextAtPath(filePath: String): String? {
    val data = NSData.dataWithContentsOfFile(filePath) ?: return null
    val bytes = data.toByteArray()
    if (bytes.isEmpty()) return null
    return bytes.decodeToString()
}

private fun loadImageBitmapAtPath(filePath: String): ImageBitmap {
    val data = NSData.dataWithContentsOfFile(filePath)
        ?: error("Failed to load image data: path=$filePath")
    val bytes = data.toByteArray()
    if (bytes.isEmpty()) {
        error("Image data is empty: path=$filePath")
    }
    val image = Image.makeFromEncoded(bytes)
    return image.toComposeImageBitmap()
}

private const val composeResourcesRoot = "compose-resources"

private fun resolveImagePath(
    bundle: NSBundle,
    prefix: String,
    path: ResourcePath,
    locales: List<String>,
): String? {
    val nameCandidates = buildScaleCandidates(path.name)
    for (locale in locales) {
        val directory = localizedResourceDirectory(prefix, path.directory, locale)
        for (name in nameCandidates) {
            val resolved = pathForResource(bundle, name, path.extension, directory)
            if (resolved != null) return resolved
        }
    }
    val attemptedPaths = mutableListOf<String>()
    for (locale in locales) {
        val directory = localizedResourceDirectory(prefix, path.directory, locale)
        for (name in nameCandidates) {
            attemptedPaths += fullResourcePath(bundle, directory, name, path.extension)
        }
    }
    if (attemptedPaths.isNotEmpty()) {
        logMissingResource("image", attemptedPaths)
    }
    return null
}

private fun parseVectorXml(
    xml: String,
    bundle: NSBundle,
    prefix: String,
    locales: List<String>,
    density: Density,
): ImageVector? {
    val vectorTag = Regex("<vector\\b[^>]*>").find(xml)?.value ?: return null
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
    val pathRegex = Regex("<path\\b[^>]*>")
    for (match in pathRegex.findAll(xml)) {
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

private fun parseXmlAttributes(tag: String): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    val regex = Regex("""([A-Za-z_][A-Za-z0-9_:\-.]+)\s*=\s*["']([^"']*)["']""")
    for (match in regex.findAll(tag)) {
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
        parseColor(loadLocalizedValue(bundle, prefix, key, locales, valuesColorsFile))
    } else {
        parseColor(trimmed)
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

private fun resourceDirectory1(prefix: String, directory: String): String {
    val parts = mutableListOf(composeResourcesRoot)
    if (prefix.isNotBlank()) parts += prefix
    if (directory.isNotBlank()) parts += directory
    return parts.joinToString("/")
}

private fun localizedResourceDirectory(prefix: String, directory: String, locale: String): String {
    if (locale.isBlank()) return resourceDirectory1(prefix, directory)
    val lproj = "$locale.lproj"
    val parts = mutableListOf(composeResourcesRoot)
    if (prefix.isNotBlank()) parts += prefix
    parts += lproj
    if (directory.isNotBlank()) parts += directory
    return parts.joinToString("/")
}

private fun pathForResource(
    bundle: NSBundle,
    name: String,
    extension: String,
    directory: String
): String? {
    return if (directory.isBlank()) {
        bundle.pathForResource(name, extension)
    } else {
        bundle.pathForResource(name, extension, directory)
            ?: bundle.pathForResource(name, extension)
    }
}

private fun fullResourcePath(
    bundle: NSBundle,
    directory: String,
    name: String,
    extension: String
): String {
    val file = if (extension.isBlank()) name else "$name.$extension"
    return if (directory.isBlank()) {
        "${bundle.bundlePath}/$file"
    } else {
        "${bundle.bundlePath}/$directory/$file"
    }
}

private fun logMissingResource(kind: String, paths: List<String>) {
    if (paths.isEmpty()) return
    val message = buildString {
        append("AkitResources missing ")
        append(kind)
        append(" path(s):\n")
        for (path in paths) {
            append(" - ")
            append(path)
            append('\n')
        }
    }
    println(message.trimEnd())
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

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val source = this.bytes ?: return ByteArray(0)
    val buffer = ByteArray(length)
    buffer.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, this.length)
    }
    return buffer
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
