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
    }
}
