import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

val publishableModules = mapOf(
    ":libs:graph" to "Munchkin Graph",
    ":libs:image:image" to "Munchkin Image",
    ":libs:resource:loader" to "Munchkin Resource Loader",
    ":libs:resource:loader-engine-coil" to "Munchkin Resource Loader Coil Engine",
    ":libs:resource:loader-engine-glide" to "Munchkin Resource Loader Glide Engine",
    ":libs:resource:runtime" to "Munchkin Resource Runtime",
    ":libs:svga" to "Munchkin SVGA",
)

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.cocoapods) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    group = rootProject.group
    version = rootProject.version

    tasks.withType<KotlinCompilationTask<*>> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}

subprojects {
    val publicationName = publishableModules[path] ?: return@subprojects

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        pluginManager.apply("com.vanniktech.maven.publish")

        extensions.configure(MavenPublishBaseExtension::class.java) {
            if (path == ":libs:graph") {
                coordinates(artifactId = "graph")
            }

            pom {
                name.set(publicationName)
                description.set(project.description)
            }
        }
    }
}
