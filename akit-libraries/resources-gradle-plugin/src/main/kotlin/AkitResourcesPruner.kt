import java.io.File

/**
 * Prunes unused resources from the output directory based on usage information.
 *
 * This class operates on the final compose-resources output, not on source files,
 * ensuring that the decision matches what will be bundled into the app.
 */
class OutputResourcesPruner(
    private val logger: AkitResourcesLog = AkitResourcesLog
) {
    /**
     * Remove unused files and strings from the output directory.
     */
    fun prune(root: File, used: UsedResources) {
        val prefixDirs = root.listFiles().orEmpty().filter { it.isDirectory }
        for (prefix in prefixDirs) {
            pruneFiles(prefix, used.drawable, used.raw)
            pruneStrings(prefix, used.strings)
        }
    }

    /**
     * Remove unused drawable/raw files under a specific resource prefix.
     */
    private fun pruneFiles(prefixDir: File, keepDrawable: Set<String>, keepRaw: Set<String>) {
        if (keepDrawable.isEmpty() && keepRaw.isEmpty()) {
            logger.info(AkitResourcesMessages.PRUNE_SKIP_NO_DRAWABLE_RAW)
            return
        }
        val files = prefixDir.walkTopDown().filter { it.isFile }.toList()
        for (file in files) {
            val relative = file.relativeTo(prefixDir).invariantSeparatorsPath
            val normalized = normalizeLocalizedPath(relative)
            if (normalized.startsWith("${ResourceKind.DRAWABLE.token}${AkitResourcesConstants.PATH_SEPARATOR}")) {
                val id = ResourceIdNormalizer.drawableIdFromFile(file)
                if (id != null && keepDrawable.isNotEmpty() && id !in keepDrawable) {
                    logger.info(AkitResourcesMessages.prunedDrawable(file.absolutePath))
                    file.delete()
                }
            } else if (normalized.startsWith("${ResourceKind.RAW.token}${AkitResourcesConstants.PATH_SEPARATOR}")) {
                val id = ResourceIdNormalizer.rawIdFromFile(file)
                if (id != null && keepRaw.isNotEmpty() && id !in keepRaw) {
                    logger.info(AkitResourcesMessages.prunedRaw(file.absolutePath))
                    file.delete()
                }
            }
        }
        cleanupEmptyDirs(prefixDir)
    }

    /**
     * Remove unused string entries from Localizable.strings files.
     */
    private fun pruneStrings(prefixDir: File, keepStrings: Set<String>) {
        if (keepStrings.isEmpty()) {
            logger.info(AkitResourcesMessages.PRUNE_SKIP_NO_STRINGS)
            return
        }
        val lprojDirs = prefixDir.listFiles().orEmpty().filter {
            it.isDirectory && it.name.endsWith(AkitResourcesConstants.LPROJ_SUFFIX)
        }
        for (dir in lprojDirs) {
            val file = File(dir, AkitResourcesConstants.STRINGS_FILE)
            if (!file.exists()) continue
            val entries = StringsFileCodec.parse(file.readText())
            if (entries.isEmpty()) continue
            val kept = linkedMapOf<String, String>()
            for ((key, value) in entries) {
                if (key in keepStrings) {
                    kept[key] = value
                } else {
                    logger.info(AkitResourcesMessages.prunedString(file.absolutePath, key))
                }
            }
            if (kept.isEmpty()) {
                file.delete()
            } else {
                file.writeText(StringsFileCodec.build(kept))
            }
        }
    }

    /**
     * Normalize localized paths by removing the `.lproj` folder.
     */
    private fun normalizeLocalizedPath(path: String): String {
        val parts = path.split(AkitResourcesConstants.PATH_SEPARATOR)
        return if (parts.firstOrNull()?.endsWith(AkitResourcesConstants.LPROJ_SUFFIX) == true) {
            parts.drop(1).joinToString(AkitResourcesConstants.PATH_SEPARATOR)
        } else {
            path
        }
    }

    /**
     * Remove empty directories after pruning.
     */
    private fun cleanupEmptyDirs(root: File) {
        val dirs = root.walkBottomUp().filter { it.isDirectory }.toList()
        for (dir in dirs) {
            val children = dir.listFiles().orEmpty()
            if (children.isEmpty()) {
                dir.delete()
            }
        }
    }
}
