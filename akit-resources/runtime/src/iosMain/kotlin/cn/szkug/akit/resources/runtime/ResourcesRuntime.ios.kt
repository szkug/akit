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
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import platform.UIKit.UIScreen

actual typealias ResourceId = NSURL

private data class ResourceInfo(
    val frameworkName: String,
    val prefix: String,
    val value: String,
)

private data class ResourcePath(
    val directory: String,
    val name: String,
    val extension: String,
)

@Composable
actual fun stringResource(id: ResourceId, vararg formatArgs: Any?): String {
    val info = decodeResourceId(id)
    val bundle = resourceBundle(info.frameworkName)
    val localeOverride = LocalResourceLocale.current.languageCode ?: ResourceLocaleManager.locale.languageCode
    val locales = preferredLocales(bundle, localeOverride)
    val raw = loadLocalizedString(bundle, info.prefix, info.value, locales)
    return formatString(raw, formatArgs)
}

@Composable
actual fun painterResource(id: ResourceId): Painter {
    val localeOverride = LocalResourceLocale.current.languageCode ?: ResourceLocaleManager.locale.languageCode
    return remember(id, localeOverride) {
        val info = decodeResourceId(id)
        val path = parseResourcePath(info.value)
        val bundle = resourceBundle(info.frameworkName)
        val locales = preferredLocales(bundle, localeOverride)
        loadPainter(bundle, info.prefix, path, locales)
    }
}

private fun decodeResourceId(id: ResourceId): ResourceInfo {
    val rawPath = id.path?.trimStart('/') ?: ""
    val decodedPath = rawPath.replace("%7C", "|").replace("%7c", "|")
    val parts = decodedPath.split('|', limit = 3)
    val frameworkName = parts.getOrNull(0).orEmpty()
    val prefix = parts.getOrNull(1).orEmpty()
    val value = parts.getOrNull(2).orEmpty()
    return ResourceInfo(frameworkName, prefix, value)
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

private fun resourceBundle(frameworkName: String): NSBundle {
    if (frameworkName.isBlank()) return NSBundle.mainBundle
    val frameworksPath = NSBundle.mainBundle.privateFrameworksPath ?: return NSBundle.mainBundle
    val frameworkPath = "$frameworksPath/$frameworkName.framework"
    return NSBundle.bundleWithPath(frameworkPath) ?: NSBundle.mainBundle
}

private fun loadLocalizedString(
    bundle: NSBundle,
    prefix: String,
    key: String,
    locales: List<String>,
): String {
    for (locale in locales) {
        val table = loadStringTable(bundle, prefix, locale) ?: continue
        val value = table[key]
        if (value != null) return value
    }
    return ""
}

private val stringTableCache = mutableMapOf<String, Map<String, String>>()

private fun loadStringTable(bundle: NSBundle, prefix: String, locale: String): Map<String, String>? {
    val directory = when {
        locale.isBlank() -> prefix.takeIf { it.isNotBlank() }
        else -> localizationDirectory(prefix, locale)
    }
    val path = if (directory.isNullOrBlank()) {
        bundle.pathForResource("Localizable", "strings")
    } else {
        bundle.pathForResource("Localizable", "strings", directory)
    } ?: return null
    return stringTableCache.getOrPut(path) {
        val data = NSData.dataWithContentsOfFile(path) ?: return@getOrPut emptyMap()
        val bytes = data.toByteArray()
        val text = bytes.decodeToString()
        parseStringsFile(text)
    }
}

private fun localizationDirectory(prefix: String, locale: String): String {
    val lproj = "$locale.lproj"
    return if (prefix.isBlank()) lproj else "$prefix/$lproj"
}

private fun preferredLocales(bundle: NSBundle, overrideLocale: String?): List<String> {
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
    return out.distinct()
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
): Painter {
    val bitmap = loadImageBitmap(bundle, prefix, path, locales)
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

private fun loadImageBitmap(
    bundle: NSBundle,
    prefix: String,
    path: ResourcePath,
    locales: List<String>,
): ImageBitmap {
    val filePath = resolveImagePath(bundle, prefix, path, locales)
        ?: error("Failed to resolve image path: name=${path.name} extension=${path.extension} directory=${path.directory}")
    val data = NSData.dataWithContentsOfFile(filePath)
        ?: error("Failed to load image data: path=$filePath")
    val bytes = data.toByteArray()
    if (bytes.isEmpty()) {
        error("Image data is empty: path=$filePath")
    }
    val image = Image.makeFromEncoded(bytes)
    return image.toComposeImageBitmap()
}

private fun resourceDirectory(prefix: String, directory: String): String {
    if (prefix.isBlank()) return directory
    if (directory.isBlank()) return prefix
    return "$prefix/$directory"
}

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
    return null
}

private fun localizedResourceDirectory(prefix: String, directory: String, locale: String): String {
    if (locale.isBlank()) return resourceDirectory(prefix, directory)
    val lproj = "$locale.lproj"
    return if (prefix.isBlank()) {
        if (directory.isBlank()) lproj else "$lproj/$directory"
    } else {
        if (directory.isBlank()) "$prefix/$lproj" else "$prefix/$lproj/$directory"
    }
}

private fun pathForResource(bundle: NSBundle, name: String, extension: String, directory: String): String? {
    return if (directory.isBlank()) {
        bundle.pathForResource(name, extension)
    } else {
        bundle.pathForResource(name, extension, directory)
            ?: bundle.pathForResource(name, extension)
    }
}

private fun buildScaleCandidates(name: String): List<String> {
    val (baseName, hasScaleSuffix) = splitScaleSuffix(name)
    if (hasScaleSuffix) {
        return listOf(name, baseName).distinct()
    }
    val candidates = mutableListOf<String>()
    for (scale in preferredScales()) {
        candidates += applyScaleSuffix(baseName, scale)
    }
    candidates += baseName
    return candidates.distinct()
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
