import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal data class StringResource(
    val name: String,
    val value: String,
)

internal data class DrawableResource(
    val id: String,
    val fileNameWithoutExtension: String,
    val extension: String,
    val relativeDir: String,
)

internal data class DrawableSource(
    val id: String,
    val baseName: String,
    val extension: String,
    val relativeDir: String,
    val locale: String?,
    val density: String?,
    val scaleFromName: Int?,
    val file: File,
)

abstract class GenerateCmpResourcesTask : DefaultTask() {

    @get:InputDirectory
    abstract val resDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val androidNamespace: Property<String>

    @get:Input
    abstract val iosResourcesPrefix: Property<String>

    @get:Input
    abstract val iosFrameworkName: Property<String>

    @get:Input
    abstract val whitelistEnabled: Property<Boolean>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringsWhitelistFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val drawablesWhitelistFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val resRoot = resDir.get().asFile
        val whitelistOn = whitelistEnabled.getOrElse(false)
        val stringsWhitelist = if (whitelistOn) {
            loadWhitelist(stringsWhitelistFile, "strings")
        } else {
            emptySet()
        }
        val drawablesWhitelist = if (whitelistOn) {
            loadWhitelist(drawablesWhitelistFile, "drawables")
        } else {
            emptySet()
        }

        val rawStringsByLocale = parseStringsByLocale(resRoot)
        val stringsByLocale = if (whitelistOn) {
            rawStringsByLocale.mapValues { (_, items) ->
                items.filter { it.name in stringsWhitelist }
            }.filterValues { it.isNotEmpty() }
        } else {
            rawStringsByLocale
        }
        val strings = stringsByLocale.values.flatten()
            .distinctBy { it.name }
            .sortedBy { it.name }

        val rawDrawableSources = parseDrawableSources(resRoot)
        val drawableSources = if (whitelistOn) {
            rawDrawableSources.filter { it.id in drawablesWhitelist }
        } else {
            rawDrawableSources
        }
        val drawables = buildDrawableResources(drawableSources)

        val outputRoot = outputDir.get().asFile
        if (outputRoot.exists()) {
            outputRoot.deleteRecursively()
        }
        val commonDir = outputRoot.resolve("commonMain")
        val androidDir = outputRoot.resolve("androidMain")
        val iosDir = outputRoot.resolve("iosMain")
        val iosResourcesDir = outputRoot.resolve("iosResources")
        commonDir.mkdirs()
        androidDir.mkdirs()
        iosDir.mkdirs()
        iosResourcesDir.mkdirs()

        val pkg = packageName.get()
        val androidPkg = androidNamespace.get().ifBlank { pkg }
        val iosPrefix = iosResourcesPrefix.get()
        val iosFrameworkName = iosFrameworkName.get()

        writeCommonRes(commonDir, pkg, strings, drawables)
        writeAndroidRes(androidDir, pkg, androidPkg, strings, drawables)
        writeIosRes(iosDir, pkg, iosPrefix, iosFrameworkName, strings, drawables)
        writeIosStringResources(iosResourcesDir, stringsByLocale)
        writeIosDrawableResources(iosResourcesDir, drawableSources)
    }

    private fun loadWhitelist(property: RegularFileProperty, label: String): Set<String> {
        if (!property.isPresent) {
            throw GradleException("cmpResources whitelist is enabled but $label whitelist file is not set.")
        }
        val file = property.get().asFile
        if (!file.exists()) {
            throw GradleException("cmpResources whitelist file does not exist: ${file.absolutePath}")
        }
        return file.readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotBlank() && !it.startsWith("//") }
            .toSet()
    }

    private fun parseStringsByLocale(resRoot: File): Map<String, List<StringResource>> {
        if (!resRoot.exists()) return emptyMap()
        val valuesDirs = resRoot.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
        val out = linkedMapOf<String, List<StringResource>>()
        for (dir in valuesDirs) {
            val locale = valuesDirToLocale(dir.name) ?: continue
            val strings = parseStrings(dir.resolve("strings.xml"))
            if (strings.isNotEmpty()) {
                out[locale] = strings
            }
        }
        return out
    }

    private fun parseStrings(stringsFile: File): List<StringResource> {
        if (!stringsFile.exists()) return emptyList()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        val nodes = doc.getElementsByTagName("string")
        val out = mutableListOf<StringResource>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            val value = node.textContent ?: ""
            out += StringResource(name, value)
        }
        return out.sortedBy { it.name }
    }

    private data class LocaleQualifier(
        val locale: String,
        val tokensToRemove: Set<String>,
    )

    private fun valuesDirToLocale(dirName: String): String? {
        if (dirName == "values") return "Base"
        if (!dirName.startsWith("values-")) return null
        val qualifiers = dirName.removePrefix("values-")
        return parseLocaleQualifier(qualifiers)?.locale
    }

    private fun parseLocaleQualifier(qualifiers: String): LocaleQualifier? {
        if (qualifiers.isBlank()) return null
        if (qualifiers.startsWith("b+")) {
            val bcp47 = qualifiers.removePrefix("b+").replace('+', '-')
            if (bcp47.isBlank()) return null
            return LocaleQualifier(bcp47, setOf(qualifiers))
        }
        val parts = qualifiers.split('-')
        val language = parts.firstOrNull()?.lowercase()
            ?.takeIf { it.length in 2..3 } ?: return null
        var regionToken: String? = null
        for (part in parts.drop(1)) {
            if (part.startsWith("r") && part.length == 3) {
                regionToken = part
                break
            }
        }
        val locale = if (regionToken != null) {
            "$language-${regionToken.substring(1).uppercase()}"
        } else {
            language
        }
        val tokens = buildSet {
            add(language)
            if (regionToken != null) add(regionToken)
        }
        return LocaleQualifier(locale, tokens)
    }

    private val densityQualifiers = setOf(
        "ldpi", "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi",
        "tvdpi", "anydpi", "nodpi",
    )

    private fun parseDensityQualifier(relativeDir: String): String? {
        val head = relativeDir.substringBefore('/')
        if (!head.startsWith("drawable-")) return null
        val tokens = head.removePrefix("drawable-").split('-')
        return tokens.firstOrNull { it in densityQualifiers }
    }

    private fun stripDensityQualifier(relativeDir: String): String {
        val parts = relativeDir.split('/')
        val head = parts.first()
        if (!head.startsWith("drawable-")) return relativeDir
        val tokens = head.removePrefix("drawable-").split('-')
        val kept = tokens.filterNot { it in densityQualifiers }
        val newHead = if (kept.isEmpty()) "drawable" else "drawable-" + kept.joinToString("-")
        return (listOf(newHead) + parts.drop(1)).joinToString("/")
    }

    private fun parseDrawableSources(resRoot: File): List<DrawableSource> {
        if (!resRoot.exists()) return emptyList()
        val drawableDirs = resRoot.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("drawable") }

        val allowedExtensions = setOf("png", "jpg", "jpeg", "webp", "gif")
        val out = mutableListOf<DrawableSource>()
        for (dir in drawableDirs) {
            val localeInfo = drawableDirLocaleInfo(dir.name)
            val locale = localeInfo?.locale
            val relativeDir = resRoot.toPath().relativize(dir.toPath()).toString()
                .replace(File.separatorChar, '/')
            val normalizedDir = if (localeInfo != null) {
                normalizeDrawableDir(relativeDir, localeInfo.tokensToRemove)
            } else {
                relativeDir
            }
            val density = parseDensityQualifier(normalizedDir)
            val files = dir.listFiles().orEmpty().filter { it.isFile }
            for (file in files) {
                val extension = file.extension.lowercase()
                if (extension !in allowedExtensions) continue

                val rawName = file.nameWithoutExtension
                val scaleFromName = parseScaleSuffix(rawName)
                val (baseName, idBase) = normalizeDrawableName(rawName)
                val id = sanitizeIdentifier(idBase)

                out += DrawableSource(
                    id = id,
                    baseName = baseName,
                    extension = extension,
                    relativeDir = normalizedDir,
                    locale = locale,
                    density = density,
                    scaleFromName = scaleFromName,
                    file = file,
                )
            }
        }
        return out
    }

    private fun buildDrawableResources(sources: List<DrawableSource>): List<DrawableResource> {
        if (sources.isEmpty()) return emptyList()
        val grouped = sources.groupBy { it.id }
        val out = mutableListOf<DrawableResource>()
        for ((id, candidates) in grouped) {
            val baseCandidates = candidates.filter { it.locale == null }
            val pickFrom = if (baseCandidates.isNotEmpty()) baseCandidates else candidates
            val chosen = pickFrom.minWith(
                compareBy<DrawableSource> { drawableDirScore(it.relativeDir) }
                    .thenBy { it.file.name }
            )
            if (chosen != null) {
                out += DrawableResource(
                    id = id,
                    fileNameWithoutExtension = chosen.baseName,
                    extension = chosen.extension,
                    relativeDir = stripDensityQualifier(chosen.relativeDir),
                )
            }
        }
        return out.sortedBy { it.id }
    }

    private fun drawableDirScore(relativeDir: String): Int {
        val suffix = relativeDir.removePrefix("drawable").trimStart('-')
        if (suffix.isBlank()) return 0
        var score = 0
        for (part in suffix.split('-')) {
            score += if (part in densityQualifiers) 2 else 1
        }
        return score
    }

    private fun drawableDirLocaleInfo(dirName: String): LocaleQualifier? {
        if (!dirName.startsWith("drawable-")) return null
        val qualifiers = dirName.removePrefix("drawable-")
        return parseLocaleQualifier(qualifiers)
    }

    private fun normalizeDrawableDir(dirName: String, tokensToRemove: Set<String>): String {
        if (!dirName.startsWith("drawable-")) return dirName
        val parts = dirName.split('-')
        val kept = parts.drop(1).filter { it !in tokensToRemove }
        return if (kept.isEmpty()) "drawable" else "drawable-" + kept.joinToString("-")
    }

    private fun normalizeDrawableName(raw: String): Pair<String, String> {
        val ninePatch = raw.endsWith(".9")
        val withoutNine = if (ninePatch) raw.removeSuffix(".9") else raw
        val withoutScale = withoutNine.replace(Regex("@[23]x$"), "")
        val baseName = if (ninePatch) "$withoutScale.9" else withoutScale
        return baseName to withoutScale
    }

    private fun parseScaleSuffix(raw: String): Int? {
        val ninePatch = raw.endsWith(".9")
        val withoutNine = if (ninePatch) raw.removeSuffix(".9") else raw
        return when {
            withoutNine.endsWith("@2x") -> 2
            withoutNine.endsWith("@3x") -> 3
            else -> null
        }
    }

    private fun sanitizeIdentifier(raw: String): String {
        val cleaned = raw.lowercase().map { ch ->
            if (ch == '_' || ch.isLetterOrDigit()) ch else '_'
        }.joinToString("")
        if (cleaned.isEmpty()) return "res_unnamed"
        val first = cleaned.first()
        return if (first == '_' || first.isLetter()) cleaned else "res_$cleaned"
    }

    private fun writeCommonRes(
        outputDir: File,
        packageName: String,
        strings: List<StringResource>,
        drawables: List<DrawableResource>,
    ) {
        val file = outputDir.resolve("Res.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import cn.szkug.akit.resources.runtime.ResourceId")
            appendLine()
            appendLine("expect object Res {")
            appendLine("    object strings {")
            if (strings.isEmpty()) {
                appendLine("    }")
            } else {
                for (string in strings) {
                    appendLine("        val ${string.name}: ResourceId")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    appendLine("        val ${drawable.id}: ResourceId")
                }
                appendLine("    }")
            }
            appendLine("}")
        })
    }

    private fun writeAndroidRes(
        outputDir: File,
        packageName: String,
        androidNamespace: String,
        strings: List<StringResource>,
        drawables: List<DrawableResource>,
    ) {
        val file = outputDir.resolve("Res.android.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import cn.szkug.akit.resources.runtime.ResourceId")
            appendLine("import $androidNamespace.R")
            appendLine()
            appendLine("actual object Res {")
            appendLine("    actual object strings {")
            if (strings.isEmpty()) {
                appendLine("    }")
            } else {
                for (string in strings) {
                    appendLine("        actual val ${string.name}: ResourceId")
                    appendLine("            get() = R.string.${string.name}")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    actual object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    appendLine("        actual val ${drawable.id}: ResourceId")
                    appendLine("            get() = R.drawable.${drawable.id}")
                }
                appendLine("    }")
            }
            appendLine("}")
        })
    }

    private fun writeIosRes(
        outputDir: File,
        packageName: String,
        iosPrefix: String,
        iosFrameworkName: String,
        strings: List<StringResource>,
        drawables: List<DrawableResource>,
    ) {
        val file = outputDir.resolve("Res.ios.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import cn.szkug.akit.resources.runtime.ResourceId")
            appendLine("import platform.Foundation.NSURL")
            appendLine()
            appendLine("private const val frameworkName = \"$iosFrameworkName\"")
            appendLine("private const val resourcesPrefix = \"$iosPrefix\"")
            appendLine()
            appendLine("private fun resourceId(value: String): ResourceId =")
            appendLine("    NSURL.fileURLWithPath(\"${'$'}frameworkName|${'$'}resourcesPrefix|${'$'}value\")")
            appendLine()
            appendLine("actual object Res {")
            appendLine("    actual object strings {")
            if (strings.isEmpty()) {
                appendLine("    }")
            } else {
                for (string in strings) {
                    appendLine("        actual val ${string.name}: ResourceId")
                    appendLine("            get() = resourceId(\"${string.name}\")")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    actual object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    val path = buildString {
                        if (drawable.relativeDir.isNotBlank()) {
                            append(drawable.relativeDir)
                            append('/')
                        }
                        append(drawable.fileNameWithoutExtension)
                        append('.')
                        append(drawable.extension)
                    }
                    appendLine("        actual val ${drawable.id}: ResourceId")
                    appendLine("            get() = resourceId(\"$path\")")
                }
                appendLine("    }")
            }
            appendLine("}")
        })
    }

    private fun writeIosStringResources(
        outputDir: File,
        stringsByLocale: Map<String, List<StringResource>>,
    ) {
        for ((locale, strings) in stringsByLocale) {
            val lprojDir = outputDir.resolve("$locale.lproj")
            lprojDir.mkdirs()
            val file = lprojDir.resolve("Localizable.strings")
            file.writeText(buildString {
                val sorted = strings.sortedBy { it.name }
                for (string in sorted) {
                    val escaped = escapeIosString(string.value)
                    append('"')
                    append(string.name)
                    append('"')
                    append(" = ")
                    append('"')
                    append(escaped)
                    append("\";\n")
                }
            })
        }
    }

    private fun writeIosDrawableResources(
        outputDir: File,
        sources: List<DrawableSource>,
    ) {
        if (sources.isEmpty()) return
        data class TargetKey(val locale: String?, val relativeDir: String, val fileName: String)
        data class Candidate(val source: DrawableSource, val priority: Int)

        val chosen = mutableMapOf<TargetKey, Candidate>()
        for (source in sources) {
            val scale = source.scaleFromName ?: densityToScale(source.density)
            val fileName = buildIosDrawableFileName(source.baseName, source.extension, scale)
            val relativeDir = stripDensityQualifier(source.relativeDir)
            val key = TargetKey(source.locale, relativeDir, fileName)
            val priority = iosCandidatePriority(scale, source)
            val existing = chosen[key]
            if (existing == null || priority < existing.priority) {
                chosen[key] = Candidate(source, priority)
            }
        }

        for ((key, candidate) in chosen) {
            val baseDir = if (key.locale.isNullOrBlank()) {
                outputDir
            } else {
                outputDir.resolve("${key.locale}.lproj")
            }
            val targetDir = if (key.relativeDir.isBlank()) baseDir else baseDir.resolve(key.relativeDir)
            targetDir.mkdirs()
            candidate.source.file.copyTo(targetDir.resolve(key.fileName), overwrite = true)
        }
    }

    private fun iosCandidatePriority(scale: Int?, source: DrawableSource): Int {
        if (source.scaleFromName != null) return 0
        val normalizedScale = if (scale == null || scale <= 1) 1 else scale
        return 1 + densityPriorityForScale(normalizedScale, source.density)
    }

    private fun densityPriorityForScale(scale: Int, density: String?): Int {
        val order = when (scale) {
            2 -> listOf("xxhdpi", "xhdpi", "hdpi", "mdpi", null, "xxxhdpi", "ldpi", "tvdpi", "anydpi", "nodpi")
            3 -> listOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi", null, "ldpi", "tvdpi", "anydpi", "nodpi")
            else -> listOf(null, "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "ldpi", "tvdpi", "anydpi", "nodpi")
        }
        val index = order.indexOf(density)
        return if (index == -1) order.size else index
    }

    private fun densityToScale(density: String?): Int? {
        return when (density) {
            "xxhdpi", "xhdpi" -> 2
            "xxxhdpi" -> 3
            "hdpi", "mdpi", "ldpi", "tvdpi", "anydpi", "nodpi", null -> 1
            else -> 1
        }
    }

    private fun buildIosDrawableFileName(baseName: String, extension: String, scale: Int?): String {
        val normalizedScale = if (scale == null || scale <= 1) null else scale
        val name = if (normalizedScale == null) {
            baseName
        } else {
            applyScaleSuffix(baseName, normalizedScale)
        }
        return "$name.$extension"
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

    private fun escapeIosString(value: String): String {
        return buildString {
            for (ch in value) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }
}
