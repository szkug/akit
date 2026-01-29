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

/**
 * Copies compose resources into the Xcode build output and optionally prunes unused files.
 *
 * This task runs after Kotlin/Native frameworks are produced so the KLIB IR
 * scanner can determine which resources are actually referenced.
 */
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

    @get:Input
    @get:Optional
    abstract val pruneLogEnabled: Property<Boolean>

    /**
     * Resolve the output directory, copy resources, and trigger pruning.
     */
    @TaskAction
    fun sync() {
        val override = outputDirOverride.orNull?.trim().orEmpty()
        val outputDir = if (override.isNotEmpty()) {
            File(override)
        } else {
            val builtProductsDir = System.getenv(AkitResourcesConstants.ENV_BUILT_PRODUCTS_DIR)?.trim().orEmpty()
            val resourcesPath = System.getenv(AkitResourcesConstants.ENV_UNLOCALIZED_RESOURCES_FOLDER_PATH)?.trim().orEmpty()
            if (builtProductsDir.isNotBlank() && resourcesPath.isNotBlank()) {
                File(builtProductsDir, resourcesPath)
            } else {
                val cocoapodsDir = cocoapodsOutputDir.orNull?.trim().orEmpty()
                if (cocoapodsDir.isBlank()) {
                    logger.lifecycle(AkitResourcesMessages.syncSkipMissingEnv())
                    return
                }
                File(cocoapodsDir)
            }
        }
        val baseDir = outputDir.canonicalFile
        val targetDir = if (baseDir.name == AkitResourcesConstants.RESOURCE_ROOT_DIR) {
            baseDir
        } else {
            File(baseDir, AkitResourcesConstants.RESOURCE_ROOT_DIR)
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        for (input in resources.files) {
            if (input.isDirectory) {
                val destination = if (input.name == AkitResourcesConstants.RESOURCE_ROOT_DIR) {
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
            pruneUnusedResources(
                targetDir,
                resFiles.files,
                klibFiles.files,
                klibToolPath.orNull,
                project.buildDir,
                pruneLogEnabled.getOrElse(false)
            )
        }
    }
}
/**
 * Orchestrates the pruning pipeline by scanning KLIB IR, scanning output resources,
 * logging usage, and removing unused files.
 */
private fun pruneUnusedResources(
    root: File,
    _resFiles: Set<File>,
    klibFiles: Set<File>,
    klibToolPath: String?,
    buildDir: File,
    logEnabled: Boolean,
) {
    val usageScanner = KlibIrUsageScanner()
    val outputScanner = OutputResourcesScanner()
    val pruner = OutputResourcesPruner()
    val usageLogger = ResourceUsageLogger(logEnabled)

    val used = usageScanner.scan(klibToolPath, klibFiles, buildDir)
    if (used.isEmpty()) {
        AkitResourcesLog.info(AkitResourcesMessages.PRUNE_SKIP_NO_USAGE)
        return
    }
    val available = outputScanner.scan(root)
    usageLogger.logUsage(used, available)
    pruner.prune(root, used)
}
