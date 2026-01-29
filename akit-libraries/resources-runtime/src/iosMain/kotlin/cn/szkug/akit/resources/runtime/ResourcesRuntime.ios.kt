@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.szkug.akit.resources.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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

actual typealias ResourceId = NSURL

private data class ResourceInfo(
    val prefix: String,
    val value: String,
)

private data class ResourcePath(
    val directory: String,
    val name: String,
    val extension: String,
)

private val preferredLocalesCache = mutableMapOf<String, List<String>>()
private val scaleCandidatesCache = mutableMapOf<String, List<String>>()

@Composable
actual fun stringResource(id: ResourceId, vararg formatArgs: Any): String {
    val info = decodeResourceId(id)
    val bundle = NSBundle.mainBundle
    val locales = preferredLocales(bundle, userDefaultsLanguage())
    val raw = loadLocalizedString(bundle, info.prefix, info.value, locales)
    return formatString(raw, formatArgs)
}

@Composable
actual fun painterResource(id: ResourceId): Painter {
    val localeOverride = userDefaultsLanguage()
    return remember(id, localeOverride) {
        val info = decodeResourceId(id)
        val path = parseResourcePath(info.value)
        val bundle = NSBundle.mainBundle
        val locales = preferredLocales(bundle, localeOverride)
        val painter = loadPainter(bundle, info.prefix, path, locales)
        if (painter != null) return@remember painter
        error("Failed to resolve image path: name=${path.name} extension=${path.extension} directory=${path.directory}")
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

private fun decodeResourceId(id: ResourceId): ResourceInfo {
    val rawPath = id.path?.trimStart('/') ?: ""
    val decodedPath = rawPath.replace("%7C", "|").replace("%7c", "|")
    val parts = decodedPath.split('|', limit = 3)
    val (prefix, value) = when (parts.size) {
        3 -> parts[1].orEmpty() to parts[2].orEmpty()
        2 -> parts[0].orEmpty() to parts[1].orEmpty()
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

private val stringTableCache = mutableMapOf<String, Map<String, String>>()

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


private fun loadStringTable(bundle: NSBundle, prefix: String, locale: String): Map<String, String>? {
    val directory = localizedResourceDirectory(prefix, "", locale)
    val path = bundle.pathForResource("Localizable", "strings", directory) ?: return null
    return stringTableCache.getOrPut(path) {
        val data = NSData.dataWithContentsOfFile(path) ?: return@getOrPut emptyMap()
        val bytes = data.toByteArray()
        val text = bytes.decodeToString()
        parseStringsFile(text)
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

private val stringsEntryRegex = Regex("\"((?:\\\\.|[^\"\\\\])*)\"\\s*=\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*;")

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

private fun pathForResource(bundle: NSBundle, name: String, extension: String, directory: String): String? {
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
