import java.io.File
import java.util.ArrayDeque
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Scans KLIB IR output to discover which resources are referenced by compiled code.
 *
 * The scanner runs `klib dump-ir` and extracts getter calls like:
 *   Res.drawable.foo.<get-foo>
 *
 * It also captures a short call chain based on the IR structure to aid debugging.
 */
class KlibIrUsageScanner(
    private val logger: AkitResourcesLog = AkitResourcesLog
) {
    /**
     * Collect resource ids and call chains from KLIBs.
     *
     * @param klibToolPath path to the `klib` tool from Kotlin/Native distribution
     * @param klibFiles candidate KLIB files or unpacked KLIB directories
     * @param buildDir project build directory (used for temporary zip output)
     */
    fun scan(klibToolPath: String?, klibFiles: Set<File>, buildDir: File): UsedResources {
        if (klibToolPath.isNullOrBlank()) {
            logger.info(AkitResourcesMessages.PRUNE_SKIP_KLIB_TOOL_MISSING)
            return emptyUsage()
        }
        val tool = File(klibToolPath)
        if (!tool.exists()) {
            logger.info(AkitResourcesMessages.pruneSkipKlibToolAt(tool.absolutePath))
            return emptyUsage()
        }
        val existingKlibs = klibFiles.filter { it.exists() }.toSet()
        if (existingKlibs.isEmpty()) {
            logger.info(AkitResourcesMessages.PRUNE_SKIP_NO_KLIB_FILES)
            return emptyUsage()
        }
        logger.info(AkitResourcesMessages.pruneScanStart(existingKlibs.size))

        val tempRoot = File(buildDir, AkitResourcesConstants.KLIB_TEMP_DIR)
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

        for (input in existingKlibs) {
            val klibPath = toKlibArchive(input, tempRoot) ?: continue
            scanKlib(
                tool,
                klibPath,
                drawable,
                raw,
                strings,
                drawableCalls,
                rawCalls,
                stringCalls
            )
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

    private fun scanKlib(
        tool: File,
        klibPath: File,
        drawable: MutableSet<String>,
        raw: MutableSet<String>,
        strings: MutableSet<String>,
        drawableCalls: MutableMap<String, MutableSet<String>>,
        rawCalls: MutableMap<String, MutableSet<String>>,
        stringCalls: MutableMap<String, MutableSet<String>>,
    ) {
        val process = ProcessBuilder(
            tool.absolutePath,
            AkitResourcesConstants.KLIB_COMMAND_DUMP_IR,
            klibPath.absolutePath
        ).redirectErrorStream(true).start()

        val funStack = ArrayDeque<Pair<Int, String>>()
        var currentFile: String? = null

        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val fileMatch = AkitResourcesRegex.IR_FILE.find(line)
                if (fileMatch != null) {
                    currentFile = fileMatch.groupValues[1].trim()
                }
                if (AkitResourcesRegex.IR_FUN.containsMatchIn(line)) {
                    updateFunctionStack(line, funStack)
                }

                val signatureMatch = AkitResourcesRegex.IR_SIGNATURE.find(line)
                if (signatureMatch != null) {
                    if (isResFile(currentFile)) return@forEach
                    val kind = ResourceKind.fromToken(signatureMatch.groupValues[1]) ?: return@forEach
                    val id = signatureMatch.groupValues[2]
                    recordUsage(
                        kind,
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

                val propertyMatch = AkitResourcesRegex.IR_PROPERTY.find(line)
                if (propertyMatch != null) {
                    if (isResFile(currentFile)) return@forEach
                    val kind = ResourceKind.fromToken(propertyMatch.groupValues[1]) ?: return@forEach
                    val id = propertyMatch.groupValues[2]
                    recordUsage(
                        kind,
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

                val directMatch = AkitResourcesRegex.IR_DIRECT.find(line)
                if (directMatch != null) {
                    if (isResFile(currentFile)) return@forEach
                    val kind = ResourceKind.fromToken(directMatch.groupValues[1]) ?: return@forEach
                    val id = directMatch.groupValues[2]
                    recordUsage(
                        kind,
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
            logger.info(AkitResourcesMessages.pruneDumpIrFailed(klibPath.absolutePath, exit))
        }
    }

    /**
     * Convert unpacked KLIB directories to `.klib` archive for the tool.
     */
    private fun toKlibArchive(input: File, tempRoot: File): File? = when {
        input.isFile && input.extension == AkitResourcesConstants.KLIB_EXTENSION -> input
        input.isDirectory -> {
            val hash = Integer.toHexString(input.absolutePath.hashCode())
            val output = File(tempRoot, "${input.name}-$hash.${AkitResourcesConstants.KLIB_EXTENSION}")
            zipKlibDir(input, output)
            output
        }
        else -> null
    }

    /**
     * Update a simple stack that approximates the current IR function context.
     */
    private fun updateFunctionStack(line: String, funStack: ArrayDeque<Pair<Int, String>>) {
        val indent = line.indexOfFirst { it != ' ' }.let { if (it < 0) 0 else it }
        val trimmed = line.trim()
        val signatureMatch = AkitResourcesRegex.IR_SIGNATURE_BLOCK.find(trimmed)
        val label = signatureMatch?.groupValues?.get(1)
            ?: AkitResourcesRegex.IR_NAME.find(trimmed)?.groupValues?.get(1)
            ?: trimmed
        while (funStack.isNotEmpty() && indent <= funStack.last().first) {
            funStack.removeLast()
        }
        funStack.addLast(indent to label)
    }

    /**
     * Build a short call chain label for debugging.
     */
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
                append(AkitResourcesConstants.CALL_CHAIN_FILE_SEPARATOR)
            }
            if (chain.isNotEmpty()) {
                append(chain.joinToString(AkitResourcesConstants.CALL_CHAIN_STEP_SEPARATOR))
            } else {
                append(AkitResourcesConstants.CALL_CHAIN_TOP_LEVEL)
            }
            append(AkitResourcesConstants.CALL_CHAIN_LINE_SEPARATOR)
            append(callLine)
        }
    }

    /**
     * Exclude Res.* source files to avoid counting declarations as usage.
     */
    private fun isResFile(currentFile: String?): Boolean {
        val file = currentFile ?: return false
        return file.endsWith(AkitResourcesConstants.RES_FILE_IOS) ||
            file.endsWith(AkitResourcesConstants.RES_FILE_COMMON)
    }

    /**
     * Record the usage of a resource id and associate it with the call chain.
     */
    private fun recordUsage(
        kind: ResourceKind,
        id: String,
        callChain: String?,
        drawable: MutableSet<String>,
        raw: MutableSet<String>,
        strings: MutableSet<String>,
        drawableCalls: MutableMap<String, MutableSet<String>>,
        rawCalls: MutableMap<String, MutableSet<String>>,
        stringCalls: MutableMap<String, MutableSet<String>>,
    ) {
        when (kind) {
            ResourceKind.DRAWABLE -> {
                drawable += id
                if (callChain != null) {
                    drawableCalls.getOrPut(id) { linkedSetOf() }.add(callChain)
                }
            }
            ResourceKind.RAW -> {
                raw += id
                if (callChain != null) {
                    rawCalls.getOrPut(id) { linkedSetOf() }.add(callChain)
                }
            }
            ResourceKind.STRINGS -> {
                strings += id
                if (callChain != null) {
                    stringCalls.getOrPut(id) { linkedSetOf() }.add(callChain)
                }
            }
        }
    }

    /**
     * Zip an unpacked KLIB directory so the `klib` tool can read it.
     */
    private fun zipKlibDir(sourceDir: File, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) {
            outputFile.delete()
        }
        val base = sourceDir.parentFile ?: sourceDir
        ZipOutputStream(outputFile.outputStream().buffered()).use { zip ->
            sourceDir.walkTopDown().forEach { file ->
                val relative = file.relativeTo(base).invariantSeparatorsPath
                if (relative.isEmpty()) return@forEach
                val entryName = if (file.isDirectory) {
                    "$relative${AkitResourcesConstants.PATH_SEPARATOR}"
                } else {
                    relative
                }
                val entry = ZipEntry(entryName)
                entry.time = file.lastModified()
                zip.putNextEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { it.copyTo(zip) }
                }
                zip.closeEntry()
            }
        }
    }

    /**
     * Create an empty usage result for early exits.
     */
    private fun emptyUsage(): UsedResources =
        UsedResources(emptySet(), emptySet(), emptySet(), emptyMap(), emptyMap(), emptyMap())
}
