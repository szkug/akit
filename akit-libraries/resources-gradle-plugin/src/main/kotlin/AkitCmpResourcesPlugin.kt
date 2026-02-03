import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File

/**
 * Gradle plugin that generates Compose resource accessors and syncs iOS resources.
 *
 * This class wires generation tasks, resource publishing, and iOS sync/pruning
 * while keeping configuration and task names centralized.
 */
class AkitCmpResourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        var syncCmpResourcesForXcodeProvider: org.gradle.api.tasks.TaskProvider<SyncCmpResourcesForXcodeTask>? = null
        val extension = extensions.create<AkitCmpResourcesExtension>("cmpResources")
        extension.packageName.convention("")
        extension.androidNamespace.convention("")
        extension.resDir.convention(layout.projectDirectory.dir("src/res"))
        extension.iosPruneUnused.convention(false)
        extension.iosPruneLogEnabled.convention(false)
        val defaultIosPrefix = providers.provider {
            val rawName = project.name
            val parts = rawName.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
            val camel = if (parts.isEmpty()) {
                rawName.replaceFirstChar { it.uppercase() }
            } else {
                parts.joinToString("") { part ->
                    part.replaceFirstChar { it.uppercase() }
                }
            }
            "${camel}Res"
        }
        extension.iosResourcesPrefix.convention(defaultIosPrefix)

        val generatedRootDir = layout.buildDirectory.dir(AkitResourcesGradleConstants.GENERATED_RES_ROOT)
        val emptyResDir = generatedRootDir.map { it.dir(AkitResourcesGradleConstants.GENERATED_EMPTY_RES_DIR) }
        val prepareEmptyResDir = tasks.register(AkitResourcesGradleConstants.TASK_PREPARE_EMPTY) {
            outputs.dir(emptyResDir)
            doLast {
                val dir = emptyResDir.get().asFile
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
        }

        val generateTask = tasks.register<GenerateCmpResourcesTask>(AkitResourcesGradleConstants.TASK_GENERATE) {
            val resolvedResDir = extension.resDir.flatMap { dir ->
                if (dir.asFile.exists()) {
                    providers.provider { dir }
                } else {
                    emptyResDir
                }
            }
            resDir.set(resolvedResDir)
            outputDir.set(generatedRootDir.map { it.dir(AkitResourcesGradleConstants.GENERATED_CODE_DIR) })
            packageName.set(extension.packageName)
            androidNamespace.set(extension.androidNamespace)
            iosResourcesPrefix.set(extension.iosResourcesPrefix)
            dependsOn(prepareEmptyResDir)
        }

        val composeResourcesElements = configurations.create(AkitResourcesGradleConstants.CONFIG_ELEMENTS) {
            isCanBeConsumed = true
            isCanBeResolved = false
        }
        val composeResourcesClasspath = configurations.create(AkitResourcesGradleConstants.CONFIG_CLASSPATH) {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        val composeResourcesArtifacts = composeResourcesClasspath.incoming.artifactView {
            lenient(true)
        }.files
        val composeResourcesDir = generatedRootDir
        val resourcesDirProvider = providers.provider {
            val prefix = extension.iosResourcesPrefix.get()
            val dirName = if (prefix.isBlank()) "resources" else prefix
            File(composeResourcesDir.get().asFile, dirName)
        }
        val prepareComposeResourcesTask = tasks.register(AkitResourcesGradleConstants.TASK_PREPARE_COMPOSE) {
            outputs.dir(resourcesDirProvider)
            dependsOn(generateTask)
            doLast {
                val outputRoot = composeResourcesDir.get().asFile
                if (!outputRoot.exists()) outputRoot.mkdirs()
                val generatedRoot = generateTask.get().outputDir.get().asFile
                    .resolve(AkitResourcesGradleConstants.GENERATED_IOS_RESOURCES_DIR)
                if (!generatedRoot.exists()) return@doLast
                val destDir = resourcesDirProvider.get()
                if (destDir.exists()) {
                    project.delete(destDir)
                }
                destDir.mkdirs()
                project.copy {
                    from(generatedRoot)
                    into(destDir)
                }
            }
        }
        composeResourcesElements.outgoing.artifact(resourcesDirProvider) {
            builtBy(prepareComposeResourcesTask)
        }

        pluginManager.withPlugin("com.android.library") {
            val libraryExt = extensions.getByType<LibraryExtension>()
            extension.androidNamespace.convention(libraryExt.namespace)
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()
            with(kotlinExt) {
                sourceSets.commonMain {
                    kotlin.srcDir(
                        generateTask.map { it.outputDir.get().asFile.resolve(AkitResourcesGradleConstants.SOURCESET_COMMON) }
                    )
                }

                sourceSets.androidMain {
                    kotlin.srcDir(
                        generateTask.map { it.outputDir.get().asFile.resolve(AkitResourcesGradleConstants.SOURCESET_ANDROID) }
                    )
                }

                sourceSets.iosMain {
                    kotlin.srcDir(
                        generateTask.map { it.outputDir.get().asFile.resolve(AkitResourcesGradleConstants.SOURCESET_IOS) }
                    )
                }
            }

            val cmpComposeResourcesFiles = objects.fileCollection().apply {
                from(resourcesDirProvider)
                from(composeResourcesArtifacts)
                builtBy(prepareComposeResourcesTask)
            }

            val syncCmpResourcesForXcode = tasks.register<SyncCmpResourcesForXcodeTask>(
                AkitResourcesGradleConstants.TASK_SYNC_XCODE
            ) {
                resources.from(cmpComposeResourcesFiles)
                outputDirOverride.set(
                    providers.gradleProperty(AkitResourcesGradleConstants.GRADLE_PROP_OUTPUT_DIR).orElse("")
                )
                dependsOn(prepareComposeResourcesTask)
            }
            syncCmpResourcesForXcodeProvider = syncCmpResourcesForXcode

            val composeSyncTasks = tasks.matching {
                (it.name.startsWith(AkitResourcesGradleConstants.SYNC_TASK_PREFIX) ||
                    it.name.startsWith(AkitResourcesGradleConstants.SYNC_POD_TASK_PREFIX)) &&
                    (it.name.contains(AkitResourcesGradleConstants.SYNC_IOS_TOKEN) ||
                        it.name.contains(AkitResourcesGradleConstants.SYNC_XCODE_TOKEN))
            }
            syncCmpResourcesForXcode.configure {
                mustRunAfter(composeSyncTasks)
                pruneUnused.set(extension.iosPruneUnused)
                pruneLogEnabled.set(extension.iosPruneLogEnabled)
            }

            tasks.matching { it.name == AkitResourcesGradleConstants.TASK_EMBED_XCODE }.configureEach {
                finalizedBy(syncCmpResourcesForXcode)
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.native.cocoapods") {
                syncCmpResourcesForXcode.configure {
                    cocoapodsOutputDir.set(
                        layout.buildDirectory.dir(AkitResourcesGradleConstants.COCOAPODS_OUTPUT_DIR)
                            .map { it.asFile.absolutePath }
                    )
                }
                tasks.matching { it.name == AkitResourcesGradleConstants.TASK_SYNC_FRAMEWORK }.configureEach {
                    dependsOn(syncCmpResourcesForXcode)
                }
                tasks.matching {
                    it.name.startsWith(AkitResourcesGradleConstants.TASK_POD_PUBLISH_PREFIX) &&
                        it.name.endsWith(AkitResourcesGradleConstants.TASK_POD_PUBLISH_SUFFIX)
                }.configureEach {
                    dependsOn(syncCmpResourcesForXcode)
                }
            }

            tasks.withType(KotlinCompilationTask::class.java).configureEach {
                dependsOn(generateTask)
            }
        }

        pluginManager.withPlugin("com.android.library") {
            extensions.getByType<LibraryExtension>().sourceSets.getByName("main").apply {
                res.srcDir(extension.resDir)
            }
        }

        afterEvaluate {
            val androidExtra = extension.androidExtraResDir.orNull?.asFile
            if (androidExtra != null && androidExtra.exists()) {
                generateTask.configure { androidExtraResDir.set(extension.androidExtraResDir) }
                pluginManager.withPlugin("com.android.library") {
                    extensions.getByType<LibraryExtension>().sourceSets.getByName("main").apply {
                        res.srcDir(androidExtra)
                    }
                }
            }
            val iosExtra = extension.iosExtraResDir.orNull?.asFile
            if (iosExtra != null && iosExtra.exists()) {
                generateTask.configure { iosExtraResDir.set(extension.iosExtraResDir) }
            }
        }

        gradle.projectsEvaluated {
            val projectDeps = linkedMapOf<String, ProjectDependency>()
            val rootPath = project.path
            fun collectProjectDeps(root: Project) {
                val sourceSetConfigs = root.configurations.matching {
                    it.name.contains(AkitResourcesGradleConstants.CONFIG_MAIN_IMPLEMENTATION) ||
                        it.name.contains(AkitResourcesGradleConstants.CONFIG_MAIN_API)
                }
                for (config in sourceSetConfigs) {
                    for (dep in config.dependencies.withType(ProjectDependency::class.java)) {
                        val path = dep.dependencyProject.path
                        if (path == rootPath) continue
                        if (projectDeps.putIfAbsent(path, dep) == null) {
                            collectProjectDeps(dep.dependencyProject)
                        }
                    }
                }
            }
            collectProjectDeps(project)
            for (dep in projectDeps.values) {
                val depProject = dep.dependencyProject
                if (depProject.configurations.findByName(AkitResourcesGradleConstants.CONFIG_ELEMENTS) != null) {
                    dependencies.add(
                        composeResourcesClasspath.name,
                        dependencies.project(
                            mapOf("path" to depProject.path, "configuration" to AkitResourcesGradleConstants.CONFIG_ELEMENTS)
                        )
                    )
                }
            }

            val allProjects = (listOf(project) + projectDeps.values.map { it.dependencyProject })
                .distinctBy { it.path }
            val resFiles = objects.fileCollection().apply {
                for (depProject in allProjects) {
                    from(
                        depProject.file(
                            "${depProject.buildDir}/${AkitResourcesGradleConstants.GENERATED_RES_ROOT}/" +
                                "${AkitResourcesGradleConstants.GENERATED_CODE_DIR}/" +
                                "${AkitResourcesGradleConstants.SOURCESET_IOS}/" +
                                AkitResourcesConstants.RES_FILE_IOS
                        )
                    )
                }
            }
            val provider = syncCmpResourcesForXcodeProvider
            if (provider != null) {
                val klibToolPathProvider = providers.provider { resolveKlibToolPath(project) ?: "" }
                    val klibDirs = objects.fileCollection().apply {
                        val targets = listOf(
                            AkitResourcesGradleConstants.TARGET_IOS_ARM64,
                            AkitResourcesGradleConstants.TARGET_IOS_X64,
                            AkitResourcesGradleConstants.TARGET_IOS_SIM_ARM64,
                        )
                        for (depProject in allProjects) {
                            for (target in targets) {
                                from(
                                    depProject.layout.buildDirectory.dir(
                                        "${AkitResourcesGradleConstants.KLIB_BASE_DIR}/" +
                                            "$target/" +
                                            "${AkitResourcesGradleConstants.KLIB_DIR_SUFFIX}/" +
                                            "${depProject.name}/" +
                                            AkitResourcesGradleConstants.KLIB_DEFAULT_VARIANT
                                    )
                                )
                            }
                        }
                    }
                provider.configure {
                    resFiles.from(resFiles)
                    klibFiles.from(klibDirs)
                    klibToolPath.set(klibToolPathProvider)
                    for (depProject in allProjects) {
                        val depTask = depProject.tasks.findByName("generateCmpResources")
                        if (depTask != null) {
                            dependsOn(depTask)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resolve a usable `klib` tool path from Gradle properties, env vars, or `~/.konan`.
 */
private fun resolveKlibToolPath(project: Project): String? {
    val propHome = project.findProperty("kotlin.native.home") as? String
    val envHomes = listOfNotNull(
        propHome,
        System.getenv("KONAN_HOME"),
        System.getenv("KOTLIN_NATIVE_HOME"),
        System.getenv("KONAN_DATA_DIR"),
    )
    for (home in envHomes) {
        val homeDir = File(home)
        val tool = findKlibTool(homeDir) ?: findKlibToolInPrebuilt(homeDir)
        if (tool != null) return tool.absolutePath
    }

    val userHome = System.getProperty("user.home") ?: return null
    val konanDir = File(userHome, ".konan")
    val tool = findKlibToolInPrebuilt(konanDir)
    if (tool != null) return tool.absolutePath
    return null
}

/**
 * Locate the `klib` executable inside a Kotlin/Native distribution directory.
 */
private fun findKlibTool(home: File): File? {
    val bin = File(home, "bin")
    val unixTool = File(bin, "klib")
    if (unixTool.exists()) return unixTool
    val windowsTool = File(bin, "klib.bat")
    if (windowsTool.exists()) return windowsTool
    val windowsCmd = File(bin, "klib.cmd")
    if (windowsCmd.exists()) return windowsCmd
    return null
}

/**
 * Locate the `klib` tool by scanning `kotlin-native-prebuilt-*` directories.
 */
private fun findKlibToolInPrebuilt(root: File): File? {
    if (!root.exists()) return null
    val candidates = root.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("kotlin-native-prebuilt-") }
        ?.sortedByDescending { it.name }
        .orEmpty()
    for (candidate in candidates) {
        val tool = findKlibTool(candidate)
        if (tool != null) return tool
    }
    return null
}
