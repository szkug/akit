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

class AkitCmpResourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        var syncCmpResourcesForXcodeProvider: org.gradle.api.tasks.TaskProvider<SyncCmpResourcesForXcodeTask>? = null
        val extension = extensions.create<AkitCmpResourcesExtension>("cmpResources")
        extension.packageName.convention("")
        extension.androidNamespace.convention("")
        extension.resDir.convention(layout.projectDirectory.dir("src/res"))
        extension.iosPruneUnused.convention(false)
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

        val generatedRootDir = layout.buildDirectory.dir("generated/compose-resources")
        val emptyResDir = generatedRootDir.map { it.dir("empty-res") }
        val prepareEmptyResDir = tasks.register("prepareCmpEmptyResDir") {
            outputs.dir(emptyResDir)
            doLast {
                val dir = emptyResDir.get().asFile
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
        }

        val generateTask = tasks.register<GenerateCmpResourcesTask>("generateCmpResources") {
            val resolvedResDir = extension.resDir.flatMap { dir ->
                if (dir.asFile.exists()) {
                    providers.provider { dir }
                } else {
                    emptyResDir
                }
            }
            resDir.set(resolvedResDir)
            outputDir.set(generatedRootDir.map { it.dir("code") })
            packageName.set(extension.packageName)
            androidNamespace.set(extension.androidNamespace)
            iosResourcesPrefix.set(extension.iosResourcesPrefix)
            dependsOn(prepareEmptyResDir)
        }

        val composeResourcesElements = configurations.create("cmpComposeResourcesElements") {
            isCanBeConsumed = true
            isCanBeResolved = false
        }
        val composeResourcesClasspath = configurations.create("cmpComposeResourcesClasspath") {
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
        val prepareComposeResourcesTask = tasks.register("prepareCmpComposeResources") {
            outputs.dir(resourcesDirProvider)
            dependsOn(generateTask)
            doLast {
                val outputRoot = composeResourcesDir.get().asFile
                if (!outputRoot.exists()) outputRoot.mkdirs()
                val generatedRoot = generateTask.get().outputDir.get().asFile.resolve("iosResources")
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

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()
            with(kotlinExt) {
                sourceSets.commonMain {
                    kotlin.srcDir(generateTask.map { it.outputDir.get().asFile.resolve("commonMain") })
                }

                sourceSets.androidMain {
                    kotlin.srcDir(generateTask.map { it.outputDir.get().asFile.resolve("androidMain") })
                }

                sourceSets.iosMain {
                    kotlin.srcDir(generateTask.map { it.outputDir.get().asFile.resolve("iosMain") })
                }
            }

            val cmpComposeResourcesFiles = objects.fileCollection().apply {
                from(resourcesDirProvider)
                from(composeResourcesArtifacts)
                builtBy(prepareComposeResourcesTask)
            }

            val syncCmpResourcesForXcode = tasks.register<SyncCmpResourcesForXcodeTask>(
                "syncCmpResourcesForXcode"
            ) {
                resources.from(cmpComposeResourcesFiles)
                outputDirOverride.set(providers.gradleProperty("cmp.ios.resources.outputDir").orElse(""))
                dependsOn(prepareComposeResourcesTask)
            }
            syncCmpResourcesForXcodeProvider = syncCmpResourcesForXcode

            val composeSyncTasks = tasks.matching {
                (it.name.startsWith("syncComposeResourcesFor") || it.name.startsWith("syncPodComposeResourcesFor")) &&
                    (it.name.contains("Ios") || it.name.contains("Xcode"))
            }
            syncCmpResourcesForXcode.configure {
                mustRunAfter(composeSyncTasks)
                pruneUnused.set(extension.iosPruneUnused)
            }

            tasks.matching { it.name == "embedAndSignAppleFrameworkForXcode" }.configureEach {
                finalizedBy(syncCmpResourcesForXcode)
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.native.cocoapods") {
                syncCmpResourcesForXcode.configure {
                    cocoapodsOutputDir.set(
                        layout.buildDirectory.dir("compose/cocoapods/compose-resources").map { it.asFile.absolutePath }
                    )
                }
                tasks.matching { it.name == "syncFramework" }.configureEach {
                    dependsOn(syncCmpResourcesForXcode)
                }
                tasks.matching {
                    it.name.startsWith("podPublish") && it.name.endsWith("XCFramework")
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
                    it.name.contains("MainImplementation") || it.name.contains("MainApi")
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
                if (depProject.configurations.findByName("cmpComposeResourcesElements") != null) {
                    dependencies.add(
                        composeResourcesClasspath.name,
                        dependencies.project(
                            mapOf("path" to depProject.path, "configuration" to "cmpComposeResourcesElements")
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
                            "${depProject.buildDir}/generated/compose-resources/code/iosMain/Res.ios.kt"
                        )
                    )
                }
            }
            val provider = syncCmpResourcesForXcodeProvider
            if (provider != null) {
                val klibToolPathProvider = providers.provider { resolveKlibToolPath(project) ?: "" }
                val klibDirs = objects.fileCollection().apply {
                    val targets = listOf("iosArm64", "iosX64", "iosSimulatorArm64")
                    for (depProject in allProjects) {
                        for (target in targets) {
                            from(
                                depProject.layout.buildDirectory.dir(
                                    "classes/kotlin/$target/main/klib/${depProject.name}/default"
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
