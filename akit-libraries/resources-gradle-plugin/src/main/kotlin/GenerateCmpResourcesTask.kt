import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal data class StringResource(
    val name: String,
    val value: String,
)

internal enum class ValueResourceType {
    STRING,
    COLOR,
    DIMEN,
    PLURAL,
    ARRAY,
}

internal enum class ResourceRefType(val token: String) {
    STRING("string"),
    COLOR("color"),
    DIMEN("dimen"),
    PLURAL("plurals"),
    DRAWABLE("drawable"),
    RAW("raw");

    companion object {
        fun fromToken(token: String): ResourceRefType? =
            values().firstOrNull { it.token == token }
    }
}

internal data class ResourceRef(
    val type: ResourceRefType,
    val name: String,
)

internal data class ValueResource(
    val name: String,
    val type: ValueResourceType,
    val value: String? = null,
    val pluralItems: Map<String, String> = emptyMap(),
    val arrayItems: List<ResourceRef> = emptyList(),
)

internal data class ValuesFile(
    val name: String,
    val resources: List<ValueResource>,
)

internal data class NamedValueResource(
    val name: String,
    val value: String,
)

internal data class PluralResource(
    val name: String,
    val items: Map<String, String>,
)

internal data class ValuesResourceKey(
    val file: String,
    val type: ValueResourceType,
    val name: String,
)

internal data class ResourceLookupKey(
    val type: ResourceRefType,
    val name: String,
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
    @get:Optional
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

    @get:InputDirectory
    @get:Optional
    abstract val iosExtraResDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val resRoot = resDir.get().asFile
        val commonValuesByLocale = parseValuesByLocale(resRoot)
        val commonValuesByFile = buildValuesByFile(commonValuesByLocale)
        val commonValueKeys = buildValuesKeys(commonValuesByFile)
        val commonStringsByLocale = collectStringsByLocale(commonValuesByLocale)
        val commonColorsByLocale = collectNamedValuesByLocale(commonValuesByLocale, ValueResourceType.COLOR)
        val commonDimensByLocale = collectNamedValuesByLocale(commonValuesByLocale, ValueResourceType.DIMEN)
        val commonPluralsByLocale = collectPluralsByLocale(commonValuesByLocale)

        val commonDrawableSources = parseDrawableSources(resRoot)
        val commonDrawables = buildDrawableResources(commonDrawableSources)
        val commonRawSources = parseRawSources(resRoot)
        val commonRaws = buildRawResources(commonRawSources)

        val androidExtraRoot = androidExtraResDir.orNull?.asFile?.takeIf { it.exists() }
        val androidExtraValuesByLocale = if (androidExtraRoot == null) {
            emptyMap()
        } else {
            parseValuesByLocale(androidExtraRoot)
        }
        val androidExtraValuesByFile = buildValuesByFile(androidExtraValuesByLocale)
        val androidValuesByFile = mergeValuesByFile(commonValuesByFile, androidExtraValuesByFile)
        val androidValueLookup = buildValuesLookup(androidValuesByFile)
        val androidExtraDrawableSources = if (androidExtraRoot == null) {
            emptyList()
        } else {
            parseDrawableSources(androidExtraRoot)
        }
        val androidExtraRawSources = if (androidExtraRoot == null) {
            emptyList()
        } else {
            parseRawSources(androidExtraRoot)
        }
        val androidDrawableSources = commonDrawableSources + androidExtraDrawableSources
        val androidDrawables = buildDrawableResources(androidDrawableSources)
        val androidRawSources = commonRawSources + androidExtraRawSources
        val androidRaws = buildRawResources(androidRawSources)

        val iosExtraRoot = iosExtraResDir.orNull?.asFile?.takeIf { it.exists() }
        val iosExtraValuesByLocale = if (iosExtraRoot == null) {
            emptyMap()
        } else {
            parseValuesByLocale(iosExtraRoot)
        }
        val iosExtraValuesByFile = buildValuesByFile(iosExtraValuesByLocale)
        val iosValuesByFile = mergeValuesByFile(commonValuesByFile, iosExtraValuesByFile)
        val iosValueLookup = buildValuesLookup(iosValuesByFile)
        val iosExtraStringsByLocale = collectStringsByLocale(iosExtraValuesByLocale)
        val iosExtraColorsByLocale = collectNamedValuesByLocale(iosExtraValuesByLocale, ValueResourceType.COLOR)
        val iosExtraDimensByLocale = collectNamedValuesByLocale(iosExtraValuesByLocale, ValueResourceType.DIMEN)
        val iosExtraPluralsByLocale = collectPluralsByLocale(iosExtraValuesByLocale)
        val iosExtraDrawableSources = if (iosExtraRoot == null) {
            emptyList()
        } else {
            parseDrawableSources(iosExtraRoot)
        }
        val iosExtraRawSources = if (iosExtraRoot == null) {
            emptyList()
        } else {
            parseRawSources(iosExtraRoot)
        }
        val iosStringsByLocale = mergeByLocale(commonStringsByLocale, iosExtraStringsByLocale) { it.name }
        val iosColorsByLocale = mergeByLocale(commonColorsByLocale, iosExtraColorsByLocale) { it.name }
        val iosDimensByLocale = mergeByLocale(commonDimensByLocale, iosExtraDimensByLocale) { it.name }
        val iosPluralsByLocale = mergeByLocale(commonPluralsByLocale, iosExtraPluralsByLocale) { it.name }
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
        val commonDrawableIds = commonDrawables.map { it.id }.toSet()
        val commonRawIds = commonRaws.map { it.id }.toSet()

        writeCommonRes(commonDir, pkg, commonValuesByFile, commonDrawables, commonRaws)
        writeAndroidRes(
            androidDir,
            pkg,
            androidPkg,
            androidValuesByFile,
            androidValueLookup,
            androidDrawables,
            androidRaws,
            commonValueKeys,
            commonDrawableIds,
            commonRawIds,
        )
        writeIosRes(
            iosDir,
            pkg,
            iosPrefix,
            iosValuesByFile,
            iosValueLookup,
            iosDrawables,
            iosRaws,
            commonValueKeys,
            commonDrawableIds,
            commonRawIds,
        )
        writeIosStringResources(iosResourcesDir, iosStringsByLocale)
        writeIosValueResources(iosResourcesDir, iosColorsByLocale, iosDimensByLocale, iosPluralsByLocale)
        writeIosDrawableResources(iosResourcesDir, iosDrawableSources)
        writeIosRawResources(iosResourcesDir, iosRawSources)
        writeIosBundleInfo(iosResourcesDir, iosPrefix, pkg)
    }

    private fun <T> mergeByLocale(
        base: Map<String, List<T>>,
        extra: Map<String, List<T>>,
        keySelector: (T) -> String,
    ): Map<String, List<T>> {
        if (extra.isEmpty()) return base
        val locales = (base.keys + extra.keys).distinct()
            .sortedWith(compareBy<String> { it != "Base" }.thenBy { it })
        val out = linkedMapOf<String, List<T>>()
        for (locale in locales) {
            val merged = LinkedHashMap<String, T>()
            for (item in base[locale].orEmpty()) {
                merged[keySelector(item)] = item
            }
            for (item in extra[locale].orEmpty()) {
                merged[keySelector(item)] = item
            }
            if (merged.isNotEmpty()) {
                out[locale] = merged.values.sortedBy { keySelector(it) }
            }
        }
        return out
    }

    private fun parseValuesByLocale(resRoot: File): Map<String, List<ValuesFile>> {
        if (!resRoot.exists()) return emptyMap()
        val valuesDirs = resRoot.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .sortedBy { it.name }
        val out = linkedMapOf<String, List<ValuesFile>>()
        for (dir in valuesDirs) {
            val locale = valuesDirToLocale(dir.name) ?: continue
            val files = dir.listFiles().orEmpty()
                .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
                .sortedBy { it.name }
            val parsed = mutableListOf<ValuesFile>()
            for (file in files) {
                val fileName = sanitizeIdentifier(file.nameWithoutExtension)
                val resources = parseValuesFile(file)
                if (resources.isNotEmpty()) {
                    parsed += ValuesFile(fileName, resources.sortedBy { it.name })
                }
            }
            if (parsed.isNotEmpty()) {
                out[locale] = parsed.sortedBy { it.name }
            }
        }
        return out
    }

    private fun parseValuesFile(file: File): List<ValueResource> {
        if (!file.exists()) return emptyList()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement ?: return emptyList()
        val nodes = root.childNodes
        val out = mutableListOf<ValueResource>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            val tag = node.nodeName
            when (tag) {
                "string" -> {
                    val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val value = node.textContent ?: ""
                    out += ValueResource(name = name, type = ValueResourceType.STRING, value = value)
                }
                "color" -> {
                    val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val value = node.textContent?.trim().orEmpty()
                    out += ValueResource(name = name, type = ValueResourceType.COLOR, value = value)
                }
                "dimen" -> {
                    val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val value = node.textContent?.trim().orEmpty()
                    if (!isSupportedDimenValue(value)) {
                        logger.warn("Unsupported dimen value for $name: $value")
                        continue
                    }
                    out += ValueResource(name = name, type = ValueResourceType.DIMEN, value = value)
                }
                "plurals" -> {
                    val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val items = parsePluralItems(node)
                    out += ValueResource(name = name, type = ValueResourceType.PLURAL, pluralItems = items)
                }
                "array", "string-array" -> {
                    val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val items = parseArrayItems(node)
                    out += ValueResource(name = name, type = ValueResourceType.ARRAY, arrayItems = items)
                }
            }
        }
        return out
    }

    private fun parsePluralItems(node: org.w3c.dom.Node): Map<String, String> {
        val items = mutableMapOf<String, String>()
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            if (child.nodeName != "item") continue
            val quantity = child.attributes?.getNamedItem("quantity")?.nodeValue ?: continue
            val value = child.textContent ?: ""
            items[quantity] = value
        }
        return items
    }

    private fun parseArrayItems(node: org.w3c.dom.Node): List<ResourceRef> {
        val out = mutableListOf<ResourceRef>()
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            if (child.nodeName != "item") continue
            val raw = child.textContent?.trim().orEmpty()
            if (!raw.startsWith("@")) continue
            val cleaned = raw.removePrefix("@").removePrefix("+")
            if (cleaned.contains(':')) continue
            val typeToken = cleaned.substringBefore('/').lowercase()
            val name = cleaned.substringAfter('/', "")
            if (name.isBlank()) continue
            val refType = ResourceRefType.fromToken(typeToken) ?: continue
            out += ResourceRef(refType, name)
        }
        return out
    }

    private fun isSupportedDimenValue(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return false
        val match = Regex("""^([+-]?\d+(?:\.\d+)?)([a-zA-Z]+)$""").find(trimmed) ?: return false
        val unit = match.groupValues.getOrNull(2).orEmpty().lowercase()
        return unit in setOf("dp", "dip", "sp", "px", "in", "mm", "pt")
    }

    private fun buildValuesByFile(valuesByLocale: Map<String, List<ValuesFile>>): Map<String, List<ValueResource>> {
        if (valuesByLocale.isEmpty()) return emptyMap()
        val out = linkedMapOf<String, LinkedHashMap<ValuesResourceKey, ValueResource>>()
        val localeOrder = valuesByLocale.keys.sortedWith(compareBy<String> { it != "Base" }.thenBy { it })
        for (locale in localeOrder) {
            for (file in valuesByLocale[locale].orEmpty()) {
                val bucket = out.getOrPut(file.name) { LinkedHashMap() }
                for (resource in file.resources) {
                    val key = ValuesResourceKey(file = file.name, type = resource.type, name = resource.name)
                    if (key !in bucket) {
                        bucket[key] = resource
                    }
                }
            }
        }
        return out.mapValues { it.value.values.sortedBy { res -> res.name } }
    }

    private fun mergeValuesByFile(
        base: Map<String, List<ValueResource>>,
        extra: Map<String, List<ValueResource>>,
    ): Map<String, List<ValueResource>> {
        if (extra.isEmpty()) return base
        val fileNames = (base.keys + extra.keys).distinct().sorted()
        val out = linkedMapOf<String, LinkedHashMap<ValuesResourceKey, ValueResource>>()
        for (fileName in fileNames) {
            val bucket = LinkedHashMap<ValuesResourceKey, ValueResource>()
            for (resource in base[fileName].orEmpty()) {
                val key = ValuesResourceKey(file = fileName, type = resource.type, name = resource.name)
                bucket[key] = resource
            }
            for (resource in extra[fileName].orEmpty()) {
                val key = ValuesResourceKey(file = fileName, type = resource.type, name = resource.name)
                bucket[key] = resource
            }
            if (bucket.isNotEmpty()) {
                out[fileName] = bucket
            }
        }
        return out.mapValues { it.value.values.sortedBy { res -> res.name } }
    }

    private fun buildValuesKeys(valuesByFile: Map<String, List<ValueResource>>): Set<ValuesResourceKey> {
        if (valuesByFile.isEmpty()) return emptySet()
        val out = linkedSetOf<ValuesResourceKey>()
        for ((file, resources) in valuesByFile) {
            for (resource in resources) {
                out += ValuesResourceKey(file = file, type = resource.type, name = resource.name)
            }
        }
        return out
    }

    private fun buildValuesLookup(valuesByFile: Map<String, List<ValueResource>>): Map<ResourceLookupKey, String> {
        if (valuesByFile.isEmpty()) return emptyMap()
        val out = linkedMapOf<ResourceLookupKey, String>()
        for (file in valuesByFile.keys.sorted()) {
            val resources = valuesByFile[file].orEmpty()
            for (resource in resources) {
                val refType = when (resource.type) {
                    ValueResourceType.STRING -> ResourceRefType.STRING
                    ValueResourceType.COLOR -> ResourceRefType.COLOR
                    ValueResourceType.DIMEN -> ResourceRefType.DIMEN
                    ValueResourceType.PLURAL -> ResourceRefType.PLURAL
                    ValueResourceType.ARRAY -> null
                }
                if (refType != null) {
                    val key = ResourceLookupKey(refType, resource.name)
                    if (key !in out) {
                        out[key] = file
                    }
                }
            }
        }
        return out
    }

    private fun collectStringsByLocale(valuesByLocale: Map<String, List<ValuesFile>>): Map<String, List<StringResource>> {
        if (valuesByLocale.isEmpty()) return emptyMap()
        val out = linkedMapOf<String, List<StringResource>>()
        for ((locale, files) in valuesByLocale) {
            val strings = LinkedHashMap<String, StringResource>()
            for (file in files) {
                for (resource in file.resources) {
                    if (resource.type != ValueResourceType.STRING) continue
                    strings[resource.name] = StringResource(resource.name, resource.value.orEmpty())
                }
            }
            if (strings.isNotEmpty()) {
                out[locale] = strings.values.sortedBy { it.name }
            }
        }
        return out
    }

    private fun collectNamedValuesByLocale(
        valuesByLocale: Map<String, List<ValuesFile>>,
        type: ValueResourceType,
    ): Map<String, List<NamedValueResource>> {
        if (valuesByLocale.isEmpty()) return emptyMap()
        val out = linkedMapOf<String, List<NamedValueResource>>()
        for ((locale, files) in valuesByLocale) {
            val entries = LinkedHashMap<String, NamedValueResource>()
            for (file in files) {
                for (resource in file.resources) {
                    if (resource.type != type) continue
                    entries[resource.name] = NamedValueResource(resource.name, resource.value.orEmpty())
                }
            }
            if (entries.isNotEmpty()) {
                out[locale] = entries.values.sortedBy { it.name }
            }
        }
        return out
    }

    private fun collectPluralsByLocale(valuesByLocale: Map<String, List<ValuesFile>>): Map<String, List<PluralResource>> {
        if (valuesByLocale.isEmpty()) return emptyMap()
        val out = linkedMapOf<String, List<PluralResource>>()
        for ((locale, files) in valuesByLocale) {
            val entries = LinkedHashMap<String, PluralResource>()
            for (file in files) {
                for (resource in file.resources) {
                    if (resource.type != ValueResourceType.PLURAL) continue
                    entries[resource.name] = PluralResource(resource.name, resource.pluralItems)
                }
            }
            if (entries.isNotEmpty()) {
                out[locale] = entries.values.sortedBy { it.name }
            }
        }
        return out
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

        val allowedExtensions = setOf("png", "jpg", "jpeg", "webp", "gif", "xml")
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

    private fun androidResourceGetter(
        resource: ValueResource,
        valuesLookup: Map<ResourceLookupKey, String>,
    ): String {
        return if (resource.type == ValueResourceType.ARRAY) {
            buildArrayExpression(resource, valuesLookup)
        } else {
            val token = androidResourceToken(resource.type)
            "R.$token.${resource.name}"
        }
    }

    private fun iosResourceGetter(
        resource: ValueResource,
        valuesLookup: Map<ResourceLookupKey, String>,
    ): String {
        return if (resource.type == ValueResourceType.ARRAY) {
            buildArrayExpression(resource, valuesLookup)
        } else {
            "resourceId(resourcesPrefix, \"${resource.name}\")"
        }
    }

    private fun androidResourceToken(type: ValueResourceType): String = when (type) {
        ValueResourceType.STRING -> "string"
        ValueResourceType.COLOR -> "color"
        ValueResourceType.DIMEN -> "dimen"
        ValueResourceType.PLURAL -> "plurals"
        ValueResourceType.ARRAY -> "array"
    }

    private fun valueResourceTypeName(resource: ValueResource): String = when (resource.type) {
        ValueResourceType.STRING -> "StringResourceId"
        ValueResourceType.COLOR -> "ColorResourceId"
        ValueResourceType.DIMEN -> "DimenResourceId"
        ValueResourceType.PLURAL -> "PluralStringResourceId"
        ValueResourceType.ARRAY -> "Array<${arrayElementTypeName(resource)}>"
    }

    private fun arrayElementTypeName(resource: ValueResource): String {
        val first = resource.arrayItems.firstOrNull()?.type ?: return "ResourceId"
        if (resource.arrayItems.any { it.type != first }) return "ResourceId"
        return resourceRefTypeName(first) ?: "ResourceId"
    }

    private fun resourceRefTypeName(type: ResourceRefType): String? = when (type) {
        ResourceRefType.STRING -> "StringResourceId"
        ResourceRefType.COLOR -> "ColorResourceId"
        ResourceRefType.DIMEN -> "DimenResourceId"
        ResourceRefType.PLURAL -> "PluralStringResourceId"
        ResourceRefType.DRAWABLE -> "PaintableResourceId"
        ResourceRefType.RAW -> "RawResourceId"
    }

    private fun buildArrayExpression(
        resource: ValueResource,
        valuesLookup: Map<ResourceLookupKey, String>,
    ): String {
        val elementType = arrayElementTypeName(resource)
        if (resource.arrayItems.isEmpty()) return "emptyArray<$elementType>()"
        val resolved = resource.arrayItems.mapNotNull { item ->
            resolveArrayItem(resource.name, item, valuesLookup)
        }
        if (resolved.isEmpty()) return "emptyArray<$elementType>()"
        return "arrayOf<$elementType>(${resolved.joinToString(", ")})"
    }

    private fun resolveArrayItem(
        arrayName: String,
        item: ResourceRef,
        valuesLookup: Map<ResourceLookupKey, String>,
    ): String? {
        return when (item.type) {
            ResourceRefType.DRAWABLE -> "Res.drawable.${item.name}"
            ResourceRefType.RAW -> "Res.raw.${item.name}"
            else -> {
                val key = ResourceLookupKey(item.type, item.name)
                val file = valuesLookup[key]
                if (file == null) {
                    logger.warn("Array $arrayName references missing ${item.type.token}/${item.name}")
                    null
                } else {
                    "Res.$file.${item.name}"
                }
            }
        }
    }

    private fun writeCommonRes(
        outputDir: File,
        packageName: String,
        valuesByFile: Map<String, List<ValueResource>>,
        drawables: List<DrawableResource>,
        raws: List<RawResource>,
    ) {
        val file = outputDir.resolve("Res.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import cn.szkug.akit.resources.runtime.ResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.StringResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.PluralStringResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.ColorResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.DimenResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.PaintableResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.RawResourceId")
            appendLine()
            appendLine("expect object Res {")
            if (valuesByFile.isNotEmpty()) {
                for (fileName in valuesByFile.keys.sorted()) {
                    val resources = valuesByFile[fileName].orEmpty()
                    appendLine("    object $fileName {")
                    if (resources.isEmpty()) {
                        appendLine("    }")
                    } else {
                        for (resource in resources) {
                            val typeLabel = valueResourceTypeName(resource)
                            appendLine("        val ${resource.name}: $typeLabel")
                        }
                        appendLine("    }")
                    }
                    appendLine()
                }
            }
            appendLine("    object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    appendLine("        val ${drawable.id}: PaintableResourceId")
                }
                appendLine("    }")
            }
            appendLine()
            appendLine("    object raw {")
            if (raws.isEmpty()) {
                appendLine("    }")
            } else {
                for (raw in raws) {
                    appendLine("        val ${raw.id}: RawResourceId")
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
        valuesByFile: Map<String, List<ValueResource>>,
        valuesLookup: Map<ResourceLookupKey, String>,
        drawables: List<DrawableResource>,
        raws: List<RawResource>,
        commonValues: Set<ValuesResourceKey>,
        commonDrawables: Set<String>,
        commonRaws: Set<String>,
    ) {
        val file = outputDir.resolve("Res.android.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import cn.szkug.akit.resources.runtime.ResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.StringResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.PluralStringResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.ColorResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.DimenResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.PaintableResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.RawResourceId")
            appendLine("import $androidNamespace.R")
            appendLine()
            appendLine("actual object Res {")
            if (valuesByFile.isNotEmpty()) {
                val commonFiles = commonValues.map { it.file }.toSet()
                for (fileName in valuesByFile.keys.sorted()) {
                    val resources = valuesByFile[fileName].orEmpty()
                    val objectKeyword = if (fileName in commonFiles) "actual object" else "object"
                    appendLine("    $objectKeyword $fileName {")
                    if (resources.isEmpty()) {
                        appendLine("    }")
                    } else {
                        for (resource in resources) {
                            val key = ValuesResourceKey(file = fileName, type = resource.type, name = resource.name)
                            val keyword = if (key in commonValues) "actual val" else "val"
                            val typeLabel = valueResourceTypeName(resource)
                            appendLine("        $keyword ${resource.name}: $typeLabel")
                            appendLine(
                                "            get() = ${androidResourceGetter(resource, valuesLookup)}"
                            )
                        }
                        appendLine("    }")
                    }
                    appendLine()
                }
            }
            appendLine("    actual object drawable {")
            if (drawables.isEmpty()) {
                appendLine("    }")
            } else {
                for (drawable in drawables) {
                    val keyword = if (drawable.id in commonDrawables) "actual val" else "val"
                    appendLine("        $keyword ${drawable.id}: PaintableResourceId")
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
                    appendLine("        $keyword ${raw.id}: RawResourceId")
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
        valuesByFile: Map<String, List<ValueResource>>,
        valuesLookup: Map<ResourceLookupKey, String>,
        drawables: List<DrawableResource>,
        raws: List<RawResource>,
        commonValues: Set<ValuesResourceKey>,
        commonDrawables: Set<String>,
        commonRaws: Set<String>,
    ) {
        val file = outputDir.resolve("Res.ios.kt")
        file.writeText(buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import cn.szkug.akit.resources.runtime.ResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.StringResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.PluralStringResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.ColorResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.DimenResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.PaintableResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.RawResourceId")
            appendLine("import cn.szkug.akit.resources.runtime.resourceId")
            appendLine()
            appendLine("private const val resourcesPrefix = \"$iosPrefix\"")
            appendLine()
            appendLine("actual object Res {")
            if (valuesByFile.isNotEmpty()) {
                val commonFiles = commonValues.map { it.file }.toSet()
                for (fileName in valuesByFile.keys.sorted()) {
                    val resources = valuesByFile[fileName].orEmpty()
                    val objectKeyword = if (fileName in commonFiles) "actual object" else "object"
                    appendLine("    $objectKeyword $fileName {")
                    if (resources.isEmpty()) {
                        appendLine("    }")
                    } else {
                        for (resource in resources) {
                            val key = ValuesResourceKey(file = fileName, type = resource.type, name = resource.name)
                            val keyword = if (key in commonValues) "actual val" else "val"
                            val typeLabel = valueResourceTypeName(resource)
                            appendLine("        $keyword ${resource.name}: $typeLabel")
                            appendLine(
                                "            get() = ${iosResourceGetter(resource, valuesLookup)}"
                            )
                        }
                        appendLine("    }")
                    }
                    appendLine()
                }
            }
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
                    appendLine("        $keyword ${drawable.id}: PaintableResourceId")
                    appendLine("            get() = resourceId(resourcesPrefix, \"$path\")")
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
                    appendLine("        $keyword ${raw.id}: RawResourceId")
                    appendLine("            get() = resourceId(resourcesPrefix, \"$path\")")
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
                    val escaped = escapeIosString(convertAndroidFormatToIos(string.value))
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

    private fun writeIosValueResources(
        outputDir: File,
        colorsByLocale: Map<String, List<NamedValueResource>>,
        dimensByLocale: Map<String, List<NamedValueResource>>,
        pluralsByLocale: Map<String, List<PluralResource>>,
    ) {
        if (colorsByLocale.isEmpty() && dimensByLocale.isEmpty() && pluralsByLocale.isEmpty()) return
        val locales = (colorsByLocale.keys + dimensByLocale.keys + pluralsByLocale.keys).distinct()
            .sortedWith(compareBy<String> { it != "Base" }.thenBy { it })
        for (locale in locales) {
            val lprojDir = iosLprojDir(outputDir, locale)
            val colors = colorsByLocale[locale].orEmpty()
            if (colors.isNotEmpty()) {
                val entries = linkedMapOf<String, String>()
                for (item in colors.sortedBy { it.name }) {
                    entries[item.name] = item.value
                }
                writeIosStringsFile(lprojDir.resolve("Colors.strings"), entries)
            }
            val dimens = dimensByLocale[locale].orEmpty()
            if (dimens.isNotEmpty()) {
                val entries = linkedMapOf<String, String>()
                for (item in dimens.sortedBy { it.name }) {
                    entries[item.name] = item.value
                }
                writeIosStringsFile(lprojDir.resolve("Dimens.strings"), entries)
            }
            val plurals = pluralsByLocale[locale].orEmpty()
            if (plurals.isNotEmpty()) {
                writeIosStringsDict(lprojDir.resolve("Localizable.stringsdict"), plurals.sortedBy { it.name })
            }
        }
    }

    private fun iosLprojDir(outputDir: File, locale: String): File {
        return if (locale.isBlank()) {
            outputDir
        } else {
            outputDir.resolve("$locale.lproj")
        }
    }

    private fun writeIosStringsFile(file: File, entries: Map<String, String>) {
        if (entries.isEmpty()) return
        file.parentFile?.mkdirs()
        file.writeText(buildString {
            for ((key, value) in entries.toSortedMap()) {
                append('"')
                append(escapeIosString(key))
                append("\" = \"")
                append(escapeIosString(value))
                append("\";\n")
            }
        })
    }

    private fun writeIosStringsDict(file: File, plurals: List<PluralResource>) {
        if (plurals.isEmpty()) return
        file.parentFile?.mkdirs()
        file.writeText(buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine(
                """<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">"""
            )
            appendLine("""<plist version="1.0">""")
            appendLine("<dict>")
            for (plural in plurals) {
                append("  <key>")
                append(xmlEscape(plural.name))
                appendLine("</key>")
                appendLine("  <dict>")
                appendLine("    <key>NSStringLocalizedFormatKey</key>")
                appendLine("    <string>%#@value@</string>")
                appendLine("    <key>value</key>")
                appendLine("    <dict>")
                appendLine("      <key>NSStringFormatSpecTypeKey</key>")
                appendLine("      <string>NSStringPluralRuleType</string>")
                appendLine("      <key>NSStringFormatValueTypeKey</key>")
                appendLine("      <string>d</string>")
                val quantities = plural.items.toSortedMap()
                for ((quantity, value) in quantities) {
                    append("      <key>")
                    append(xmlEscape(quantity))
                    appendLine("</key>")
                    append("      <string>")
                    append(xmlEscape(convertAndroidPluralFormatToIos(value)))
                    appendLine("</string>")
                }
                appendLine("    </dict>")
                appendLine("  </dict>")
            }
            appendLine("</dict>")
            appendLine("</plist>")
        })
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

    private fun writeIosBundleInfo(
        outputDir: File,
        prefix: String,
        packageName: String,
    ) {
        val bundleName = if (prefix.isBlank()) "resources" else prefix
        val file = outputDir.resolve("Info.plist")
        if (file.exists()) return
        file.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
              <key>CFBundleIdentifier</key>
              <string>$packageName.$bundleName</string>
              <key>CFBundleName</key>
              <string>$bundleName</string>
              <key>CFBundlePackageType</key>
              <string>BNDL</string>
              <key>CFBundleShortVersionString</key>
              <string>1.0</string>
              <key>CFBundleVersion</key>
              <string>1</string>
            </dict>
            </plist>
            """.trimIndent()
        )
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
        if (extension.equals("xml", ignoreCase = true)) {
            return "$baseName.$extension"
        }
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

    private fun convertAndroidFormatToIos(value: String): String {
        if (!value.contains('%')) return value
        val placeholder = "\u0000"
        val escaped = value.replace("%%", placeholder)
        val converted = Regex("%(\\d+\\$)?[@sdif]").replace(escaped) { match ->
            val index = match.groups[1]?.value.orEmpty()
            val spec = match.value.last()
            val mapped = when (spec) {
                's', 'd', 'i', 'f' -> '@'
                else -> spec
            }
            "%$index$mapped"
        }
        return converted.replace(placeholder, "%%")
    }

    private fun convertAndroidPluralFormatToIos(value: String): String {
        if (!value.contains('%')) return value
        val placeholder = "\u0000"
        val escaped = value.replace("%%", placeholder)
        var nextIndex = 2
        val converted = Regex("%(\\d+\\$)?[@sdif]").replace(escaped) { match ->
            val rawIndex = match.groups[1]?.value
            val index = if (rawIndex.isNullOrBlank()) {
                "${nextIndex++}\$"
            } else {
                val parsed = rawIndex.dropLast(1).toIntOrNull() ?: return@replace match.value
                "${parsed + 1}\$"
            }
            val spec = match.value.last()
            val mapped = when (spec) {
                's', 'd', 'i', 'f' -> '@'
                else -> spec
            }
            "%$index$mapped"
        }
        return converted.replace(placeholder, "%%")
    }

    private fun xmlEscape(value: String): String {
        return buildString {
            for (ch in value) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(ch)
                }
            }
        }
    }

}
