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
        val extension = extensions.create<AkitCmpResourcesExtension>("cmpResources")
        extension.packageName.convention("")
        extension.androidNamespace.convention("")
        extension.resDir.convention(layout.projectDirectory.dir("src/res"))
        extension.iosResourcesPrefix.convention("cmp-res")
        extension.whitelistEnabled.convention(false)

        val emptyResDir = layout.buildDirectory.dir("generated/cmp-resources/empty-res")
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
            outputDir.set(layout.buildDirectory.dir("generated/cmp-resources"))
            packageName.set(extension.packageName)
            androidNamespace.set(extension.androidNamespace)
            iosResourcesPrefix.set(extension.iosResourcesPrefix)
            whitelistEnabled.set(extension.whitelistEnabled)
            stringsWhitelistFile.set(extension.stringsWhitelistFile)
            drawablesWhitelistFile.set(extension.drawablesWhitelistFile)
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
        val composeResourcesDir = layout.buildDirectory.dir("generated/cmp-resources/compose-resources")
        val prepareComposeResourcesTask = tasks.register("prepareCmpComposeResources") {
            outputs.dir(composeResourcesDir)
            dependsOn(generateTask)
            doLast {
                val outputRoot = composeResourcesDir.get().asFile
                if (outputRoot.exists()) {
                    project.delete(outputRoot)
                }
                outputRoot.mkdirs()
                val generatedRoot = generateTask.get().outputDir.get().asFile.resolve("iosResources")
                if (!generatedRoot.exists()) return@doLast
                val prefix = extension.iosResourcesPrefix.get()
                val destDir = if (prefix.isBlank()) {
                    File(outputRoot, "compose-resources")
                } else {
                    File(outputRoot, "compose-resources/$prefix")
                }
                destDir.mkdirs()
                project.copy {
                    from(generatedRoot)
                    into(destDir)
                }
            }
        }
        composeResourcesElements.outgoing.artifact(composeResourcesDir) {
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
                from(composeResourcesDir)
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

            val composeSyncTasks = tasks.matching {
                it.name.startsWith("syncComposeResourcesFor") &&
                    (it.name.contains("Ios") || it.name.contains("Xcode"))
            }
            syncCmpResourcesForXcode.configure {
                mustRunAfter(composeSyncTasks)
            }

            tasks.matching { it.name == "embedAndSignAppleFrameworkForXcode" }.configureEach {
                dependsOn(syncCmpResourcesForXcode)
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
        }
    }
}
