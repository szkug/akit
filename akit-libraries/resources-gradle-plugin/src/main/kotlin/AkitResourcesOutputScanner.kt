import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Scans the generated compose-resources output directory to find available resource ids.
 *
 * This scanner uses the same naming rules as the generator to ensure ids match those
 * referenced in code (and therefore by KLIB IR).
 */
class OutputResourcesScanner {
    /**
     * Collect all resource ids found under the given root.
     */
    fun scan(root: File): AvailableResources {
        val drawable = linkedSetOf<String>()
        val raw = linkedSetOf<String>()
        val strings = linkedSetOf<String>()

        val prefixDirs = root.listFiles().orEmpty().filter { it.isDirectory }
        for (prefix in prefixDirs) {
            val files = prefix.walkTopDown().filter { it.isFile }.toList()
            for (file in files) {
                val relative = file.relativeTo(prefix).invariantSeparatorsPath
                val normalized = normalizeLocalizedPath(relative)
                when {
                    normalized.startsWith("${ResourceKind.DRAWABLE.token}${AkitResourcesConstants.PATH_SEPARATOR}") -> {
                        ResourceIdNormalizer.drawableIdFromFile(file)?.let { drawable += it }
                    }
                    normalized.startsWith("${ResourceKind.RAW.token}${AkitResourcesConstants.PATH_SEPARATOR}") -> {
                        ResourceIdNormalizer.rawIdFromFile(file)?.let { raw += it }
                    }
                    normalized.endsWith(AkitResourcesConstants.STRINGS_FILE) -> {
                        val entries = StringsFileCodec.parse(file.readText())
                        strings += entries.keys
                    }
                    normalized.endsWith(AkitResourcesConstants.STRINGS_DICT_FILE) -> {
                        strings += StringsDictCodec.parseKeys(file)
                    }
                }
            }
        }
        return AvailableResources(drawable, raw, strings)
    }

    /**
     * Drop localization directory prefixes (e.g., `en.lproj`) for path-based checks.
     */
    private fun normalizeLocalizedPath(path: String): String {
        val parts = path.split(AkitResourcesConstants.PATH_SEPARATOR)
        return if (parts.firstOrNull()?.endsWith(AkitResourcesConstants.LPROJ_SUFFIX) == true) {
            parts.drop(1).joinToString(AkitResourcesConstants.PATH_SEPARATOR)
        } else {
            path
        }
    }
}

/**
 * Shared resource id normalization logic.
 *
 * This mirrors the generator's naming rules so ids match the produced Res.* APIs.
 */
object ResourceIdNormalizer {
    /**
     * Derive a drawable id from the output file name (including nine-patch handling).
     */
    fun drawableIdFromFile(file: File): String? {
        val rawName = file.nameWithoutExtension
        if (rawName.isBlank()) return null
        val (_, idBase) = normalizeDrawableName(rawName)
        return sanitizeIdentifier(idBase)
    }

    /**
     * Derive a raw resource id from the output file name.
     */
    fun rawIdFromFile(file: File): String? {
        val baseName = file.nameWithoutExtension
        if (baseName.isBlank()) return null
        return sanitizeIdentifier(baseName)
    }

    /**
     * Normalize drawable names by removing density suffix and preserving nine-patch markers.
     */
    private fun normalizeDrawableName(raw: String): Pair<String, String> {
        val ninePatch = raw.endsWith(".9")
        val withoutNine = if (ninePatch) raw.removeSuffix(".9") else raw
        val withoutScale = withoutNine.replace(AkitResourcesRegex.DRAWABLE_SCALE_SUFFIX, "")
        val baseName = if (ninePatch) "$withoutScale.9" else withoutScale
        return baseName to withoutScale
    }

    /**
     * Match the generator's identifier rules (lowercase and safe characters).
     */
    private fun sanitizeIdentifier(raw: String): String {
        val cleaned = raw.lowercase().map { ch ->
            if (ch == '_' || ch.isLetterOrDigit()) ch else '_'
        }.joinToString("")
        if (cleaned.isEmpty()) return AkitResourcesConstants.GENERATED_ID_EMPTY
        val first = cleaned.first()
        return if (first == '_' || first.isLetter()) cleaned else "${AkitResourcesConstants.GENERATED_ID_PREFIX}$cleaned"
    }
}

/**
 * Simple parser/writer for iOS Localizable.strings files.
 */
object StringsFileCodec {
    /**
     * Parse a Localizable.strings file into a map.
     */
    fun parse(content: String): Map<String, String> {
        if (content.isBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (match in AkitResourcesRegex.STRINGS_ENTRY.findAll(content)) {
            out[match.groupValues[1]] = match.groupValues[2]
        }
        return out
    }

    /**
     * Build a Localizable.strings file from entries.
     */
    fun build(entries: Map<String, String>): String {
        return buildString {
            for ((key, value) in entries) {
                append('"')
                append(key)
                append('"')
                append(AkitResourcesConstants.STRINGS_ASSIGN)
                append('"')
                append(value)
                append(AkitResourcesConstants.STRINGS_LINE_END)
            }
        }
    }
}

/**
 * Parser for Localizable.stringsdict files to extract plural keys.
 */
object StringsDictCodec {
    fun parseKeys(file: File): Set<String> {
        if (!file.exists()) return emptySet()
        return runCatching { parseKeysFromXml(file.readText()) }.getOrDefault(emptySet())
    }

    private fun parseKeysFromXml(content: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }.newDocumentBuilder().parse(content.byteInputStream())
        val root = doc.documentElement ?: return emptySet()
        val dict = root.getElementsByTagName("dict")?.item(0) ?: return emptySet()
        val out = linkedSetOf<String>()
        val children = dict.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            if (node.nodeName == "key") {
                val key = node.textContent?.trim().orEmpty()
                if (key.isNotBlank()) out += key
            }
        }
        return out
    }
}

/**
 * Extracts string resource ids from generated Res.*.kt files.
 */
object ResFileStringIdScanner {
    private val stringIdRegex = Regex("""\bval\s+([A-Za-z0-9_]+)\s*:\s*(StringResourceId|PluralStringResourceId)\b""")

    fun scan(files: Set<File>): Set<String> {
        if (files.isEmpty()) return emptySet()
        val out = linkedSetOf<String>()
        for (file in files) {
            if (!file.exists()) continue
            val content = file.readText()
            for (match in stringIdRegex.findAll(content)) {
                val id = match.groupValues[1]
                if (id.isNotBlank()) out += id
            }
        }
        return out
    }
}
