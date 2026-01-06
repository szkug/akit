import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class CmpResourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<CmpResourcesExtension>("cmpResources")
        extension.packageName.convention("")
        extension.androidNamespace.convention("")
        extension.resDir.convention(layout.projectDirectory.dir("src/res"))
        extension.iosResourcesDir.convention(
            rootProject.layout.projectDirectory.dir("apps/ios/src/iosMain/resources")
        )
        extension.iosResourcesPrefix.convention("cmp-res")

        val generateTask = tasks.register<GenerateCmpResourcesTask>("generateCmpResources") {
            resDir.set(extension.resDir)
            outputDir.set(layout.buildDirectory.dir("generated/cmp-resources"))
            packageName.set(extension.packageName)
            androidNamespace.set(extension.androidNamespace)
            iosResourcesPrefix.set(extension.iosResourcesPrefix)
        }

        val copyIosResources = tasks.register<Sync>("copyCmpResourcesToIos") {
            val destDir = extension.iosResourcesDir.flatMap {
                extension.iosResourcesPrefix.map { prefix -> it.dir(prefix) }
            }
            from(extension.resDir)
            into(destDir)
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            with(extensions.getByType<KotlinMultiplatformExtension>()) {
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

            tasks.withType(KotlinCompilationTask::class.java).configureEach {
                dependsOn(generateTask)
                dependsOn(copyIosResources)
            }
        }

        pluginManager.withPlugin("com.android.library") {
            extensions.getByType<LibraryExtension>().sourceSets.getByName("main").apply {
                res.srcDir(extension.resDir)
            }
        }
    }
}
