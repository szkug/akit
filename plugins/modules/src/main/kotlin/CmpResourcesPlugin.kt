import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File

class CmpResourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<CmpResourcesExtension>("cmpResources")
        extension.packageName.convention("")
        extension.androidNamespace.convention("")
        extension.resDir.convention(layout.projectDirectory.dir("src/res"))
        extension.iosResourcesPrefix.convention("cmp-res")
        extension.iosFrameworkName.convention("")
        extension.iosFrameworkBundleId.convention("")

        val generateTask = tasks.register<GenerateCmpResourcesTask>("generateCmpResources") {
            resDir.set(extension.resDir)
            outputDir.set(layout.buildDirectory.dir("generated/cmp-resources"))
            packageName.set(extension.packageName)
            androidNamespace.set(extension.androidNamespace)
            iosResourcesPrefix.set(extension.iosResourcesPrefix)
            iosFrameworkName.set(extension.iosFrameworkName)
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()
            val inferredFrameworkName = kotlinExt.targets
                .withType(KotlinNativeTarget::class.java)
                .flatMap { it.binaries.withType(Framework::class.java) }
                .firstOrNull()
                ?.baseName
                .orEmpty()
            if (inferredFrameworkName.isNotBlank()) {
                extension.iosFrameworkName.convention(inferredFrameworkName)
            }

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

            kotlinExt.targets.withType(KotlinNativeTarget::class.java).configureEach {
                binaries.withType(Framework::class.java).configureEach {
                    linkTaskProvider.configure {
                        doLast {
                            val resRoot = extension.resDir.get().asFile
                            if (!resRoot.exists()) return@doLast
                            val prefix = extension.iosResourcesPrefix.get()
                            val frameworkDir = if (outputDirectory.name.endsWith(".framework")) {
                                outputDirectory
                            } else {
                                File(outputDirectory, "${baseName}.framework")
                            }
                            val resourcesDir = File(frameworkDir, "Resources")
                            val destDir = if (prefix.isBlank()) {
                                resourcesDir
                            } else {
                                File(resourcesDir, prefix)
                            }
                            project.delete(destDir)
                            project.copy {
                                from(resRoot)
                                into(destDir)
                            }
                            patchFrameworkInfoPlist(
                                frameworkDir,
                                baseName,
                                extension.iosFrameworkBundleId.get(),
                            )
                        }
                    }
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
    }
}

private fun patchFrameworkInfoPlist(
    frameworkDir: File,
    baseName: String,
    bundleId: String,
) {
    val infoPlist = File(frameworkDir, "Info.plist")
    if (!infoPlist.exists()) return
    val content = infoPlist.readText()
    var updated = content
    if (bundleId.isNotBlank()) {
        val bundleRegex = Regex("<key>CFBundleIdentifier</key>\\s*<string>[^<]*</string>")
        updated = if (bundleRegex.containsMatchIn(updated)) {
            bundleRegex.replace(
                updated,
                "<key>CFBundleIdentifier</key>\n    <string>$bundleId</string>"
            )
        } else {
            updated
        }
    }
    val entries = buildList {
        if (!updated.contains("<key>CFBundlePackageType</key>")) {
            add("    <key>CFBundlePackageType</key>\n    <string>FMWK</string>")
        }
        if (!updated.contains("<key>CFBundleExecutable</key>")) {
            add("    <key>CFBundleExecutable</key>\n    <string>$baseName</string>")
        }
        if (!updated.contains("<key>CFBundleName</key>")) {
            add("    <key>CFBundleName</key>\n    <string>$baseName</string>")
        }
        if (bundleId.isNotBlank() && !updated.contains("<key>CFBundleIdentifier</key>")) {
            add("    <key>CFBundleIdentifier</key>\n    <string>$bundleId</string>")
        }
    }
    if (entries.isEmpty() && updated == content) return
    val marker = "</dict>"
    val index = updated.lastIndexOf(marker)
    if (index == -1) return
    val insert = entries.joinToString("\n", postfix = "\n")
    val finalText = if (entries.isEmpty()) {
        updated
    } else {
        updated.substring(0, index) + insert + updated.substring(index)
    }
    infoPlist.writeText(finalText)
}
