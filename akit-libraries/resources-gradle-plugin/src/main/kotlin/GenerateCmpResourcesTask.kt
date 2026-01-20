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

internal data class RawResource(
    val id: String,
    val fileNameWithoutExtension: String,
    val extension: String,
    val relativeDir: String,
)

internal data class RawSource(
    val id: String,
    val baseName: String,
    val extension: String,
    val relativeDir: String,
    val locale: String?,
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

    @get:InputDirectory
    @get:Optional
    abstract val androidExtraResDir: DirectoryProperty

    @get:Input
    abstract val iosResourcesPrefix: Property<String>

    @get:Input
    abstract val iosFrameworkName: Property<String>

    @get:InputDirectory
    @get:Optional
    abstract val iosExtraResDir: DirectoryProperty

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

        val commonStringsByLocale = filterStringsByWhitelist(
            parseStringsByLocale(resRoot),
            stringsWhitelist,
            whitelistOn,
        )
        val commonStrings = commonStringsByLocale.values.flatten()
            .distinctBy { it.name }
            .sortedBy { it.name }

        val commonDrawableSources = filterDrawableSourcesByWhitelist(
            parseDrawableSources(resRoot),
            drawablesWhitelist,
            whitelistOn,
        )
        val commonDrawables = buildDrawableResources(commonDrawableSources)
        val commonRawSources = filterRawSourcesByWhitelist(
            parseRawSources(resRoot),
            drawablesWhitelist,
            whitelistOn,
        )
        val commonRaws = buildRawResources(commonRawSources)

        val androidExtraRoot = androidExtraResDir.orNull?.asFile?.takeIf { it.exists() }
        val androidExtraStringsByLocale = if (androidExtraRoot == null) {
            emptyMap()
        } else {
            filterStringsByWhitelist(
                parseStringsByLocale(androidExtraRoot),
                stringsWhitelist,
                whitelistOn,
            )
        }
        val androidExtraDrawableSources = if (androidExtraRoot == null) {
            emptyList()
        } else {
            filterDrawableSourcesByWhitelist(
                parseDrawableSources(androidExtraRoot),
                drawablesWhitelist,
                whitelistOn,
            )
        }
        val androidExtraRawSources = if (androidExtraRoot == null) {
            emptyList()
        } else {
            filterRawSourcesByWhitelist(
                parseRawSources(androidExtraRoot),
                drawablesWhitelist,
                whitelistOn,
            )
        }
        val androidStringsByLocale = mergeStringsByLocale(commonStringsByLocale, androidExtraStringsByLocale)
        val androidStrings = androidStringsByLocale.values.flatten()
            .distinctBy { it.name }
            .sortedBy { it.name }
        val androidDrawableSources = commonDrawableSources + androidExtraDrawableSources
        val androidDrawables = buildDrawableResources(androidDrawableSources)
        val androidRawSources = commonRawSources + androidExtraRawSources
        val androidRaws = buildRawResources(androidRawSources)

        val iosExtraRoot = iosExtraResDir.orNull?.asFile?.takeIf { it.exists() }
        val iosExtraStringsByLocale = if (iosExtraRoot == null) {
            emptyMap()
        } else {
            filterStringsByWhitelist(
                parseStringsByLocale(iosExtraRoot),
                stringsWhitelist,
                whitelistOn,
            )
        }
        val iosExtraDrawableSources = if (iosExtraRoot == null) {
            emptyList()
        } else {
            filterDrawableSourcesByWhitelist(
                parseDrawableSources(iosExtraRoot),
                drawablesWhitelist,
                whitelistOn,
            )
        }
        val iosExtraRawSources = if (iosExtraRoot == null) {
            emptyList()
        } else {
            filterRawSourcesByWhitelist(
                parseRawSources(iosExtraRoot),
                drawablesWhitelist,
                whitelistOn,
            )
        }
        val iosStringsByLocale = mergeStringsByLocale(commonStringsByLocale, iosExtraStringsByLocale)
        val iosStrings = iosStringsByLocale.values.flatten()
            .distinctBy { it.name }
            .sortedBy { it.name }
        val iosDrawableSources = commonDrawableSources + iosExtraDrawableSources
        val iosDrawables = buildDrawableResources(iosDrawableSources)
        val iosRawSources = commonRawSources + iosExtraRawSources
        val iosRaws = buildRawResources(iosRawSources)

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
        val commonStringIds = commonStrings.map { it.name }.toSet()
        val commonDrawableIds = commonDrawables.map { it.id }.toSet()
        val commonRawIds = commonRaws.map { it.id }.toSet()

        writeCommonRes(commonDir, pkg, commonStrings, commonDrawables, commonRaws)
        writeAndroidRes(
            androidDir,
            pkg,
            androidPkg,
            androidStrings,
            androidDrawables,
            androidRaws,
            commonStringIds,
            commonDrawableIds,
            commonRawIds,
        )
        writeIosRes(
            iosDir,
            pkg,
            iosPrefix,
            iosFrameworkName,
            iosStrings,
            iosDrawables,
            iosRaws,
            commonStringIds,
            commonDrawableIds,
            commonRawIds,
        )
        writeIosStringResources(iosResourcesDir, iosStringsByLocale)
        writeIosDrawableResources(iosResourcesDir, iosDrawableSources)
        writeIosRawResources(iosResourcesDir, iosRawSources)
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

    private fun filterStringsByWhitelist(
        stringsByLocale: Map<String, List<StringResource>>,
        whitelist: Set<String>,
        whitelistOn: Boolean,
    ): Map<String, List<StringResource>> {
        if (!whitelistOn) return stringsByLocale
        return stringsByLocale.mapValues { (_, items) ->
            items.filter { it.name in whitelist }
        }.filterValues { it.isNotEmpty() }
    }

    private fun filterDrawableSourcesByWhitelist(
        sources: List<DrawableSource>,
        whitelist: Set<String>,
        whitelistOn: Boolean,
    ): List<DrawableSource> {
        if (!whitelistOn) return sources
        return sources.filter { it.id in whitelist }
    }

    private fun filterRawSourcesByWhitelist(
        sources: List<RawSource>,
        whitelist: Set<String>,
        whitelistOn: Boolean,
    ): List<RawSource> {
        if (!whitelistOn) return sources
        return sources.filter { it.id in whitelist }
    }

    private fun mergeStringsByLocale(
        base: Map<String, List<StringResource>>,
        extra: Map<String, List<StringResource>>,
    ): Map<String, List<StringResource>> {
        if (extra.isEmpty()) return base
        val locales = (base.keys + extra.keys).distinct()
        val out = linkedMapOf<String, List<StringResource>>()
        for (locale in locales) {
            val merged = LinkedHashMap<String, StringResource>()
            for (string in base[locale].orEmpty()) {
                merged[string.name] = string
            }
            for (string in extra[locale].orEmpty()) {
                merged[string.name] = string
            }
            if (merged.isNotEmpty()) {
                out[locale] = merged.values.sortedBy { it.name }
            }
        }
        return out
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

    private fun parseRawSources(resRoot: File): List<RawSource> {
        if (!resRoot.exists()) return emptyList()
        val rawDirs = resRoot.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("raw") }

        val out = mutableListOf<RawSource>()
        for (dir in rawDirs) {
            val localeInfo = rawDirLocaleInfo(dir.name)
            val locale = localeInfo?.locale
            val relativeDir = resRoot.toPath().relativize(dir.toPath()).toString()
                .replace(File.separatorChar, '/')
            val normalizedDir = if (localeInfo != null) {
                normalizeRawDir(relativeDir, localeInfo.tokensToRemove)
            } else {
                relativeDir
            }
            val files = dir.listFiles().orEmpty().filter { it.isFile }
            for (file in files) {
                val extension = file.extension.lowercase()
                if (extension.isBlank()) continue
                val baseName = file.nameWithoutExtension
                val id = sanitizeIdentifier(baseName)
                out += RawSource(
                    id = id,
                    baseName = baseName,
                    extension = extension,
                    relativeDir = normalizedDir,
                    locale = locale,
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

    private fun buildRawResources(sources: List<RawSource>): List<RawResource> {
        if (sources.isEmpty()) return emptyList()
        val grouped = sources.groupBy { it.id }
        val out = mutableListOf<RawResource>()
        for ((id, candidates) in grouped) {
            val baseCandidates = candidates.filter { it.locale == null }
            val pickFrom = if (baseCandidates.isNotEmpty()) baseCandidates else candidates
            val chosen = pickFrom.minWith(compareBy<RawSource> { it.relativeDir }.thenBy { it.file.name })
            if (chosen != null) {
                out += RawResource(
                    id = id,
                    fileNameWithoutExtension = chosen.baseName,
                    extension = chosen.extension,
                    relativeDir = chosen.relativeDir,
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

    private fun rawDirLocaleInfo(dirName: String): LocaleQualifier? {
        if (!dirName.startsWith("raw-")) return null
        val qualifiers = dirName.removePrefix("raw-")
        return parseLocaleQualifier(qualifiers)
    }

    private fun normalizeDrawableDir(dirName: String, tokensToRemove: Set<String>): String {
        if (!dirName.startsWith("drawable-")) return dirName
        val parts = dirName.split('-')
        val kept = parts.drop(1).filter { it !in tokensToRemove }
        return if (kept.isEmpty()) "drawable" else "drawable-" + kept.joinToString("-")
    }

    private fun normalizeRawDir(dirName: String, tokensToRemove: Set<String>): String {
        if (!dirName.startsWith("raw-")) return dirName
        val parts = dirName.split('-')
        val kept = parts.drop(1).filter { it !in tokensToRemove }
        return if (kept.isEmpty()) "raw" else "raw-" + kept.joinToString("-")
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
        raws: List<RawResource>,
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
            appendLine()
            appendLine("    object raw {")
            if (raws.isEmpty()) {
                appendLine("    }")
            } else {
                for (raw in raws) {
                    appendLine("        val ${raw.id}: ResourceId")
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
        raws: List<RawResource>,
        commonStrings: Set<String>,
        commonDrawables: Set<String>,
        commonRaws: Set<String>,
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
                    val keyword = if (string.name in commonStrings) "actual val" else "val"
                    appendLine("        $keyword ${string.name}: ResourceId")
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
                    val keyword = if (drawable.id in commonDrawables) "actual val" else "val"
                    appendLine("        $keyword ${drawable.id}: ResourceId")
                    appendLine("            get() = R.drawable.${drawable.id}")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    actual object raw {")
            if (raws.isEmpty()) {
                appendLine("    }")
            } else {
                for (raw in raws) {
                    val keyword = if (raw.id in commonRaws) "actual val" else "val"
                    appendLine("        $keyword ${raw.id}: ResourceId")
                    appendLine("            get() = R.raw.${raw.id}")
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
        raws: List<RawResource>,
        commonStrings: Set<String>,
        commonDrawables: Set<String>,
        commonRaws: Set<String>,
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
                    val keyword = if (string.name in commonStrings) "actual val" else "val"
                    appendLine("        $keyword ${string.name}: ResourceId")
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
                    val keyword = if (drawable.id in commonDrawables) "actual val" else "val"
                    appendLine("        $keyword ${drawable.id}: ResourceId")
                    appendLine("            get() = resourceId(\"$path\")")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    actual object raw {")
            if (raws.isEmpty()) {
                appendLine("    }")
            } else {
                for (raw in raws) {
                    val path = buildString {
                        if (raw.relativeDir.isNotBlank()) {
                            append(raw.relativeDir)
                            append('/')
                        }
                        append(raw.fileNameWithoutExtension)
                        append('.')
                        append(raw.extension)
                    }
                    val keyword = if (raw.id in commonRaws) "actual val" else "val"
                    appendLine("        $keyword ${raw.id}: ResourceId")
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

    private fun writeIosRawResources(
        outputDir: File,
        sources: List<RawSource>,
    ) {
        if (sources.isEmpty()) return
        data class TargetKey(val locale: String?, val relativeDir: String, val fileName: String)

        val chosen = mutableMapOf<TargetKey, RawSource>()
        for (source in sources.sortedBy { it.file.name }) {
            val fileName = "${source.baseName}.${source.extension}"
            val key = TargetKey(source.locale, source.relativeDir, fileName)
            if (key !in chosen) {
                chosen[key] = source
            }
        }

        for ((key, source) in chosen) {
            val baseDir = if (key.locale.isNullOrBlank()) {
                outputDir
            } else {
                outputDir.resolve("${key.locale}.lproj")
            }
            val targetDir = if (key.relativeDir.isBlank()) baseDir else baseDir.resolve(key.relativeDir)
            targetDir.mkdirs()
            source.file.copyTo(targetDir.resolve(key.fileName), overwrite = true)
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
