package cn.szkug.akit.resources.runtime

import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.localizedStringWithFormat

/**
 * Localization table loader for iOS compose-resources.
 *
 * Responsibility: read Localizable.strings and values.strings tables, resolve locales, and
 * format plurals via Foundation. This object centralizes IO and caching.
 *
 * Performance: tables are cached per file path; preferred locales are cached per bundle+override.
 *
 */
internal object LocalizationIos {
    const val valuesFileExtension = "strings"
    const val valuesColorsFile = "Colors"
    const val valuesDimensFile = "Dimens"
    const val valuesPluralsFile = "Localizable"
    const val valuesPluralsExtension = "stringsdict"

    private const val appLanguageKey = "akit.app.language"
    private const val appleLanguagesKey = "AppleLanguages"

    private val preferredLocalesCache = mutableMapOf<String, List<String>>()
    private val stringTableCache = mutableMapOf<String, Map<String, String>>()
    private val valuesTableCache = mutableMapOf<String, Map<String, String>>()
    private val dimenTableCache = mutableMapOf<String, Map<String, DimenIos.Value>>()

    fun userDefaultsLanguage(): String? {
        val defaults = NSUserDefaults.standardUserDefaults
        val direct = defaults.stringForKey(appLanguageKey)?.trim()?.takeIf { it.isNotEmpty() }
        if (direct != null) return direct
        val languages = defaults.objectForKey(appleLanguagesKey) as? List<*>
        val first = languages?.firstOrNull() as? String
        return first?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun preferredLocales(bundle: NSBundle, overrideLocale: String?): List<String> {
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

    fun loadLocalizedStringOrThrow(
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
            attemptedPaths += ResourcePathsIos.fullResourcePath(
                bundle,
                ResourcePathsIos.localizedResourceDirectory(prefix, "", locale),
                valuesPluralsFile,
                valuesFileExtension,
            )
        }
        val message = buildString {
            append("Missing string resource: key=")
            append(key)
            if (prefix.isNotBlank()) {
                append(" prefix=")
                append(prefix)
            }
            append(". Searched paths:\n")
            for (path in attemptedPaths) {
                append(" - ")
                append(path)
                append('\n')
            }
        }
        error(message.trimEnd())
    }

    fun loadLocalizedValue(
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
            attemptedPaths += ResourcePathsIos.fullResourcePath(
                bundle,
                ResourcePathsIos.localizedResourceDirectory(prefix, "", locale),
                fileBase,
                valuesFileExtension,
            )
        }
        if (attemptedPaths.isNotEmpty()) {
            ResourcePathsIos.logMissingResource("$fileBase table", attemptedPaths)
        }
        return ""
    }

    fun loadLocalizedValueOrThrow(
        bundle: NSBundle,
        prefix: String,
        key: String,
        locales: List<String>,
        fileBase: String,
        kind: String,
    ): String {
        val attemptedPaths = mutableListOf<String>()
        for (locale in locales) {
            val table = loadValuesTable(bundle, prefix, locale, fileBase)
            val value = table?.get(key)
            if (value != null) return value
            attemptedPaths += ResourcePathsIos.fullResourcePath(
                bundle,
                ResourcePathsIos.localizedResourceDirectory(prefix, "", locale),
                fileBase,
                valuesFileExtension,
            )
        }
        val message = buildString {
            append("Missing ")
            append(kind)
            append(" resource: key=")
            append(key)
            if (prefix.isNotBlank()) {
                append(" prefix=")
                append(prefix)
            }
            append(". Searched paths:\n")
            for (path in attemptedPaths) {
                append(" - ")
                append(path)
                append('\n')
            }
        }
        error(message.trimEnd())
    }

    fun loadLocalizedDimen(
        bundle: NSBundle,
        prefix: String,
        key: String,
        locales: List<String>,
    ): DimenIos.Value {
        val attemptedPaths = mutableListOf<String>()
        for (locale in locales) {
            val table = loadDimenTable(bundle, prefix, locale)
            val value = table?.get(key)
            if (value != null) return value
            attemptedPaths += ResourcePathsIos.fullResourcePath(
                bundle,
                ResourcePathsIos.localizedResourceDirectory(prefix, "", locale),
                valuesDimensFile,
                valuesFileExtension,
            )
        }
        if (attemptedPaths.isNotEmpty()) {
            ResourcePathsIos.logMissingResource("dimens table", attemptedPaths)
        }
        return DimenIos.Value(0f, DimenIos.Unit.NONE)
    }

    fun loadLocalizedPluralOrThrow(
        bundle: NSBundle,
        prefix: String,
        key: String,
        locales: List<String>,
        count: Int,
        formatArgs: Array<out Any>,
    ): String {
        val attemptedPaths = mutableListOf<String>()
        val sentinel = "\u0000__MISSING__\u0000"
        val resourceBundle = ResourcePathsIos.bundleForPrefix(bundle, prefix)
        for (locale in locales) {
            val attemptedPath = if (locale.isBlank()) {
                "${resourceBundle.bundlePath}/${valuesPluralsFile}.${valuesPluralsExtension}"
            } else {
                "${resourceBundle.bundlePath}/${locale}.lproj/${valuesPluralsFile}.${valuesPluralsExtension}"
            }
            attemptedPaths += attemptedPath
            val localizedBundle = ResourcePathsIos.localizedBundleForLocale(resourceBundle, locale) ?: continue
            val candidate = localizedBundle.localizedStringForKey(key, sentinel, valuesPluralsFile)
            if (candidate != sentinel) {
                return formatPluralWithFoundation(candidate, count, formatArgs)
            }
        }
        val message = buildString {
            append("Missing plural resource: key=")
            append(key)
            if (prefix.isNotBlank()) {
                append(" prefix=")
                append(prefix)
            }
            append(". Searched paths:\n")
            for (path in attemptedPaths) {
                append(" - ")
                append(path)
                append('\n')
            }
        }
        error(message.trimEnd())
    }

    fun formatString(template: String, formatArgs: Array<out Any?>): String {
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

    private fun formatPluralWithFoundation(
        format: String,
        count: Int,
        formatArgs: Array<out Any>,
    ): String {
        val normalizedArgs = formatArgs.map(::normalizeFoundationFormatArg)
        return when (normalizedArgs.size) {
            0 -> NSString.localizedStringWithFormat(format, count)
            1 -> NSString.localizedStringWithFormat(format, count, normalizedArgs[0])
            2 -> NSString.localizedStringWithFormat(format, count, normalizedArgs[0], normalizedArgs[1])
            3 -> NSString.localizedStringWithFormat(
                format,
                count,
                normalizedArgs[0],
                normalizedArgs[1],
                normalizedArgs[2],
            )
            4 -> NSString.localizedStringWithFormat(
                format,
                count,
                normalizedArgs[0],
                normalizedArgs[1],
                normalizedArgs[2],
                normalizedArgs[3],
            )
            5 -> NSString.localizedStringWithFormat(
                format,
                count,
                normalizedArgs[0],
                normalizedArgs[1],
                normalizedArgs[2],
                normalizedArgs[3],
                normalizedArgs[4],
            )
            6 -> NSString.localizedStringWithFormat(
                format,
                count,
                normalizedArgs[0],
                normalizedArgs[1],
                normalizedArgs[2],
                normalizedArgs[3],
                normalizedArgs[4],
                normalizedArgs[5],
            )
            7 -> NSString.localizedStringWithFormat(
                format,
                count,
                normalizedArgs[0],
                normalizedArgs[1],
                normalizedArgs[2],
                normalizedArgs[3],
                normalizedArgs[4],
                normalizedArgs[5],
                normalizedArgs[6],
            )
            else -> error("Too many format args for plural resource: count=${formatArgs.size}")
        }
    }

    private fun normalizeFoundationFormatArg(value: Any): Any {
        return when (value) {
            is Byte, is Short, is Int, is Long, is Float, is Double, is Boolean, is Char -> value.toString()
            else -> value
        }
    }

    private fun loadStringTable(
        bundle: NSBundle,
        prefix: String,
        locale: String,
    ): Map<String, String>? {
        val directory = ResourcePathsIos.localizedResourceDirectory(prefix, "", locale)
        val path = bundle.pathForResource(valuesPluralsFile, valuesFileExtension, directory) ?: return null
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
        val directory = ResourcePathsIos.localizedResourceDirectory(prefix, "", locale)
        val path = bundle.pathForResource(fileBase, valuesFileExtension, directory) ?: return null
        return valuesTableCache.getOrPut(path) {
            val data = NSData.dataWithContentsOfFile(path) ?: return@getOrPut emptyMap()
            val bytes = data.toByteArray()
            val text = bytes.decodeToString()
            parseStringsFile(text)
        }
    }

    private fun loadDimenTable(
        bundle: NSBundle,
        prefix: String,
        locale: String,
    ): Map<String, DimenIos.Value>? {
        val directory = ResourcePathsIos.localizedResourceDirectory(prefix, "", locale)
        val path = bundle.pathForResource(valuesDimensFile, valuesFileExtension, directory) ?: return null
        return dimenTableCache.getOrPut(path) {
            val raw = loadValuesTable(bundle, prefix, locale, valuesDimensFile).orEmpty()
            raw.mapNotNull { (key, value) ->
                val parsed = DimenIos.parse(value)
                parsed?.let { key to it }
            }.toMap()
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
}
