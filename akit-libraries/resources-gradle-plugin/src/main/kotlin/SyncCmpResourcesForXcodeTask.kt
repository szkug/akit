import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class SyncCmpResourcesForXcodeTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resources: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val outputDirOverride: Property<String>

    @get:Input
    @get:Optional
    abstract val cocoapodsOutputDir: Property<String>

    @get:Input
    abstract val pruneUnused: Property<Boolean>

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val klibFiles: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val klibToolPath: Property<String>

    @TaskAction
    fun sync() {
        val override = outputDirOverride.orNull?.trim().orEmpty()
        val outputDir = if (override.isNotEmpty()) {
            File(override)
        } else {
            val builtProductsDir = System.getenv("BUILT_PRODUCTS_DIR")?.trim().orEmpty()
            val resourcesPath = System.getenv("UNLOCALIZED_RESOURCES_FOLDER_PATH")?.trim().orEmpty()
            if (builtProductsDir.isNotBlank() && resourcesPath.isNotBlank()) {
                File(builtProductsDir, resourcesPath)
            } else {
                val cocoapodsDir = cocoapodsOutputDir.orNull?.trim().orEmpty()
                if (cocoapodsDir.isBlank()) {
                    logger.lifecycle(
                        "Skipping syncCmpResourcesForXcode: missing BUILT_PRODUCTS_DIR/UNLOCALIZED_RESOURCES_FOLDER_PATH."
                    )
                    return
                }
                File(cocoapodsDir)
            }
        }
        val baseDir = outputDir.canonicalFile
        val targetDir = if (baseDir.name == "compose-resources") {
            baseDir
        } else {
            File(baseDir, "compose-resources")
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        for (input in resources.files) {
            if (input.isDirectory) {
                val destination = if (input.name == "compose-resources") {
                    targetDir
                } else {
                    File(targetDir, input.name)
                }
                project.copy {
                    from(input)
                    into(destination)
                    includeEmptyDirs = false
                }
            } else if (input.isFile) {
                project.copy {
                    from(input)
                    into(targetDir)
                    includeEmptyDirs = false
                }
            }
        }

        if (pruneUnused.getOrElse(false)) {
            pruneUnusedResources(targetDir, resFiles.files, klibFiles.files, klibToolPath.orNull, project.buildDir)
        }
    }
}

private data class UsedResources(
    val drawable: Set<String>,
    val raw: Set<String>,
    val strings: Set<String>,
    val drawableCalls: Map<String, Set<String>>,
    val rawCalls: Map<String, Set<String>>,
    val stringCalls: Map<String, Set<String>>,
)

private fun pruneUnusedResources(
    root: File,
    _resFiles: Set<File>,
    klibFiles: Set<File>,
    klibToolPath: String?,
    buildDir: File,
) {
    val usedIds = collectUsedIdsFromKlibs(klibToolPath, klibFiles, buildDir)
    if (usedIds.drawable.isEmpty() && usedIds.raw.isEmpty() && usedIds.strings.isEmpty()) {
        println("AkitResources prune skipped: no usage detected in KLIB IR.")
        return
    }

    val available = collectAvailableIds(root)
    logUsage("drawable", usedIds.drawable, available.drawable, usedIds.drawableCalls)
    logUsage("raw", usedIds.raw, available.raw, usedIds.rawCalls)
    logUsage("strings", usedIds.strings, available.strings, usedIds.stringCalls)

    val prefixDirs = root.listFiles().orEmpty().filter { it.isDirectory }
    for (prefix in prefixDirs) {
        pruneFiles(prefix, usedIds.drawable, usedIds.raw)
        pruneStrings(prefix, usedIds.strings)
    }
}

private fun pruneFiles(prefixDir: File, keepDrawable: Set<String>, keepRaw: Set<String>) {
    if (keepDrawable.isEmpty() && keepRaw.isEmpty()) {
        println("AkitResources prune skipped: no drawable/raw usage detected.")
        return
    }
    val files = prefixDir.walkTopDown().filter { it.isFile }.toList()
    for (file in files) {
        val relative = file.relativeTo(prefixDir).invariantSeparatorsPath
        val normalized = normalizeLocalizedPath(relative)
        if (normalized.startsWith("drawable/")) {
            val id = drawableIdFromFile(file)
            if (id != null && keepDrawable.isNotEmpty() && id !in keepDrawable) {
                println("AkitResources pruned drawable: ${file.absolutePath}")
                file.delete()
            }
        } else if (normalized.startsWith("raw/")) {
            val id = rawIdFromFile(file)
            if (id != null && keepRaw.isNotEmpty() && id !in keepRaw) {
                println("AkitResources pruned raw: ${file.absolutePath}")
                file.delete()
            }
        }
    }
    cleanupEmptyDirs(prefixDir)
}

private fun pruneStrings(prefixDir: File, keepStrings: Set<String>) {
    if (keepStrings.isEmpty()) {
        println("AkitResources prune skipped: no string usage detected.")
        return
    }
    val lprojDirs = prefixDir.listFiles().orEmpty().filter { it.isDirectory && it.name.endsWith(".lproj") }
    for (dir in lprojDirs) {
        val file = File(dir, "Localizable.strings")
        if (!file.exists()) continue
        val text = file.readText()
        val entries = parseStringsFile(text)
        if (entries.isEmpty()) continue
        val kept = linkedMapOf<String, String>()
        for ((key, value) in entries) {
            if (key in keepStrings) {
                kept[key] = value
            } else {
                println("AkitResources pruned string: ${file.absolutePath} key=$key")
            }
        }
        if (kept.isEmpty()) {
            file.delete()
        } else {
            file.writeText(buildStringsFile(kept))
        }
    }
}

private fun normalizeLocalizedPath(path: String): String {
    val parts = path.split('/')
    return if (parts.firstOrNull()?.endsWith(".lproj") == true) {
        parts.drop(1).joinToString("/")
    } else {
        path
    }
}

private fun cleanupEmptyDirs(root: File) {
    val dirs = root.walkBottomUp().filter { it.isDirectory }.toList()
    for (dir in dirs) {
        val children = dir.listFiles().orEmpty()
        if (children.isEmpty()) {
            dir.delete()
        }
    }
}

private val stringsEntryRegex = Regex("\"((?:\\\\.|[^\"\\\\])*)\"\\s*=\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*;")

private fun parseStringsFile(content: String): Map<String, String> {
    if (content.isBlank()) return emptyMap()
    val out = LinkedHashMap<String, String>()
    for (match in stringsEntryRegex.findAll(content)) {
        out[match.groupValues[1]] = match.groupValues[2]
    }
    return out
}

private fun buildStringsFile(entries: Map<String, String>): String {
    return buildString {
        for ((key, value) in entries) {
            append('"')
            append(key)
            append('"')
            append(" = ")
            append('"')
            append(value)
            append("\";\n")
        }
    }
}

private data class AvailableResources(
    val drawable: Set<String>,
    val raw: Set<String>,
    val strings: Set<String>,
)

private fun collectAvailableIds(root: File): AvailableResources {
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
                normalized.startsWith("drawable/") -> {
                    drawableIdFromFile(file)?.let { drawable += it }
                }
                normalized.startsWith("raw/") -> {
                    rawIdFromFile(file)?.let { raw += it }
                }
                normalized.endsWith("Localizable.strings") -> {
                    val entries = parseStringsFile(file.readText())
                    strings += entries.keys
                }
            }
        }
    }
    return AvailableResources(drawable, raw, strings)
}

private fun collectUsedIdsFromKlibs(
    klibToolPath: String?,
    klibFiles: Set<File>,
    buildDir: File,
): UsedResources {
    if (klibToolPath.isNullOrBlank()) {
        println("AkitResources prune skipped: klib tool not found.")
        return UsedResources(emptySet(), emptySet(), emptySet(), emptyMap(), emptyMap(), emptyMap())
    }
    val tool = File(klibToolPath)
    if (!tool.exists()) {
        println("AkitResources prune skipped: klib tool not found at ${tool.absolutePath}.")
        return UsedResources(emptySet(), emptySet(), emptySet(), emptyMap(), emptyMap(), emptyMap())
    }
    val existingKlibs = klibFiles.filter { it.exists() }.toSet()
    if (existingKlibs.isEmpty()) {
        println("AkitResources prune skipped: no klib files found.")
        return UsedResources(emptySet(), emptySet(), emptySet(), emptyMap(), emptyMap(), emptyMap())
    }
    println("AkitResources prune: scanning KLIB IR from ${existingKlibs.size} libraries.")
    val tempRoot = File(buildDir, "compose-resources/klib-prune")
    if (tempRoot.exists()) {
        tempRoot.deleteRecursively()
    }
    tempRoot.mkdirs()

    val drawable = linkedSetOf<String>()
    val raw = linkedSetOf<String>()
    val strings = linkedSetOf<String>()
    val drawableCalls = linkedMapOf<String, MutableSet<String>>()
    val rawCalls = linkedMapOf<String, MutableSet<String>>()
    val stringCalls = linkedMapOf<String, MutableSet<String>>()
    val signatureRegex = Regex("""Res\.(strings|drawable|raw)[.#]<get-([A-Za-z0-9_]+)>""")
    val propertyRegex = Regex("""Res\.(strings|drawable|raw)\.([A-Za-z0-9_]+)\.<get-([A-Za-z0-9_]+)>""")
    val directRegex = Regex("""Res\.(strings|drawable|raw)\.<get-([A-Za-z0-9_]+)>""")
    val funRegex = Regex("""^\s*(FUN|CONSTRUCTOR)\b""")
    val fileRegex = Regex("""^\s*FILE fqName:.*fileName:(.+)$""")
    val signatureBlockRegex = Regex("""signature:\[[^\]]*<-\s*([^\]]+)\]""")
    val nameRegex = Regex("""\bname:([^\s]+)""")

    for (input in existingKlibs) {
        val klibPath = when {
            input.isFile && input.extension == "klib" -> input
            input.isDirectory -> {
                val hash = Integer.toHexString(input.absolutePath.hashCode())
                val output = File(tempRoot, "${input.name}-$hash.klib")
                zipKlibDir(input, output)
                output
            }
            else -> null
        } ?: continue

        val process = ProcessBuilder(tool.absolutePath, "dump-ir", klibPath.absolutePath)
            .redirectErrorStream(true)
            .start()
        val funStack = ArrayDeque<Pair<Int, String>>()
        var currentFile: String? = null
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val fileMatch = fileRegex.find(line)
                if (fileMatch != null) {
                    currentFile = fileMatch.groupValues[1].trim()
                }
                if (funRegex.containsMatchIn(line)) {
                    val indent = line.indexOfFirst { it != ' ' }.let { if (it < 0) 0 else it }
                    val trimmed = line.trim()
                    val signatureMatch = signatureBlockRegex.find(trimmed)
                    val label = signatureMatch?.groupValues?.get(1)
                        ?: nameRegex.find(trimmed)?.groupValues?.get(1)
                        ?: trimmed
                    while (funStack.isNotEmpty() && indent <= funStack.last().first) {
                        funStack.removeLast()
                    }
                    funStack.addLast(indent to label)
                }
                val signatureMatch = signatureRegex.find(line)
                if (signatureMatch != null) {
                    if (isResFile(currentFile)) return@forEach
                    val type = signatureMatch.groupValues[1]
                    val id = signatureMatch.groupValues[2]
                    recordUsage(
                        type,
                        id,
                        buildCallChain(currentFile, funStack, line),
                        drawable,
                        raw,
                        strings,
                        drawableCalls,
                        rawCalls,
                        stringCalls
                    )
                    return@forEach
                }
                val propertyMatch = propertyRegex.find(line)
                if (propertyMatch != null) {
                    if (isResFile(currentFile)) return@forEach
                    val type = propertyMatch.groupValues[1]
                    val id = propertyMatch.groupValues[2]
                    recordUsage(
                        type,
                        id,
                        buildCallChain(currentFile, funStack, line),
                        drawable,
                        raw,
                        strings,
                        drawableCalls,
                        rawCalls,
                        stringCalls
                    )
                    return@forEach
                }
                val directMatch = directRegex.find(line)
                if (directMatch != null) {
                    if (isResFile(currentFile)) return@forEach
                    val type = directMatch.groupValues[1]
                    val id = directMatch.groupValues[2]
                    recordUsage(
                        type,
                        id,
                        buildCallChain(currentFile, funStack, line),
                        drawable,
                        raw,
                        strings,
                        drawableCalls,
                        rawCalls,
                        stringCalls
                    )
                }
            }
        }
        val exit = process.waitFor()
        if (exit != 0) {
            println("AkitResources prune warning: klib dump-ir failed for ${input.absolutePath} (exit=$exit).")
        }
    }
    return UsedResources(
        drawable,
        raw,
        strings,
        drawableCalls.mapValues { it.value.toSet() },
        rawCalls.mapValues { it.value.toSet() },
        stringCalls.mapValues { it.value.toSet() },
    )
}

private fun zipKlibDir(sourceDir: File, outputFile: File) {
    outputFile.parentFile?.mkdirs()
    if (outputFile.exists()) {
        outputFile.delete()
    }
    val base = sourceDir.parentFile ?: sourceDir
    java.util.zip.ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
        sourceDir.walkTopDown().forEach { file ->
            val relative = file.relativeTo(base).invariantSeparatorsPath
            if (relative.isEmpty()) return@forEach
            val entryName = if (file.isDirectory) "$relative/" else relative
            val entry = java.util.zip.ZipEntry(entryName)
            entry.time = file.lastModified()
            zip.putNextEntry(entry)
            if (file.isFile) {
                file.inputStream().use { it.copyTo(zip) }
            }
            zip.closeEntry()
        }
    }
}

private fun recordUsage(
    type: String,
    id: String,
    callChain: String?,
    drawable: MutableSet<String>,
    raw: MutableSet<String>,
    strings: MutableSet<String>,
    drawableCalls: MutableMap<String, MutableSet<String>>,
    rawCalls: MutableMap<String, MutableSet<String>>,
    stringCalls: MutableMap<String, MutableSet<String>>,
) {
    when (type) {
        "drawable" -> {
            drawable += id
            if (callChain != null) {
                drawableCalls.getOrPut(id) { linkedSetOf() }.add(callChain)
            }
        }
        "raw" -> {
            raw += id
            if (callChain != null) {
                rawCalls.getOrPut(id) { linkedSetOf() }.add(callChain)
            }
        }
        "strings" -> {
            strings += id
            if (callChain != null) {
                stringCalls.getOrPut(id) { linkedSetOf() }.add(callChain)
            }
        }
    }
}

private fun buildCallChain(
    currentFile: String?,
    funStack: ArrayDeque<Pair<Int, String>>,
    line: String,
): String? {
    if (funStack.isEmpty() && currentFile == null) return null
    val chain = funStack.map { it.second }.takeLast(3)
    val fileLabel = currentFile?.let { File(it).name }
    val callLine = line.trim()
    return buildString {
        if (fileLabel != null) {
            append(fileLabel)
            append(" | ")
        }
        if (chain.isNotEmpty()) {
            append(chain.joinToString(" -> "))
        } else {
            append("<top-level>")
        }
        append(" :: ")
        append(callLine)
    }
}

private fun isResFile(currentFile: String?): Boolean {
    val file = currentFile ?: return false
    return file.endsWith("Res.ios.kt") || file.endsWith("Res.kt")
}

private fun logUsage(
    type: String,
    usedIds: Set<String>,
    availableIds: Set<String>,
    calls: Map<String, Set<String>>,
) {
    if (usedIds.isEmpty()) return
    val missing = usedIds.filterNot { availableIds.contains(it) }
    if (missing.isNotEmpty()) {
        println("AkitResources prune warning: $type ids not found in output resources: ${missing.joinToString(", ")}")
        if (availableIds.isEmpty()) {
            println("AkitResources prune warning: $type output resources set is empty.")
        }
    }
    for (id in usedIds) {
        println("AkitResources used $type: $id")
        val callSites = calls[id].orEmpty()
        for (call in callSites) {
            println("AkitResources call: $call")
        }
    }
}

private fun drawableIdFromFile(file: File): String? {
    val rawName = file.nameWithoutExtension
    if (rawName.isBlank()) return null
    val (_, idBase) = normalizeDrawableName(rawName)
    return sanitizeIdentifier(idBase)
}

private fun rawIdFromFile(file: File): String? {
    val baseName = file.nameWithoutExtension
    if (baseName.isBlank()) return null
    return sanitizeIdentifier(baseName)
}

private fun normalizeDrawableName(raw: String): Pair<String, String> {
    val ninePatch = raw.endsWith(".9")
    val withoutNine = if (ninePatch) raw.removeSuffix(".9") else raw
    val withoutScale = withoutNine.replace(Regex("@[23]x$"), "")
    val baseName = if (ninePatch) "$withoutScale.9" else withoutScale
    return baseName to withoutScale
}

private fun sanitizeIdentifier(raw: String): String {
    val cleaned = raw.lowercase().map { ch ->
        if (ch == '_' || ch.isLetterOrDigit()) ch else '_'
    }.joinToString("")
    if (cleaned.isEmpty()) return "res_unnamed"
    val first = cleaned.first()
    return if (first == '_' || first.isLetter()) cleaned else "res_$cleaned"
}
