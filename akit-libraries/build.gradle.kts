import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import kotlin.apply

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.cocoapods) apply false
    alias(libs.plugins.maven.publish) apply false
}


class VersionManager() {

    private val properties by lazy {
        val file = project.file("version.properties")
        Properties().apply { load(file.inputStream()) }
    }
    val group: String get() = properties.getProperty("publish.group")
    val version: String get() = properties.getProperty("publish.version")
}

val manager = VersionManager()

val mavenPublishPluginId = libs.plugins.maven.publish.get().pluginId

allprojects {

    pluginManager.apply(mavenPublishPluginId)

    val project = this

    project.extensions.findByType(MavenPublishBaseExtension::class.java)?.apply {
        group = manager.group
        version = manager.version

        coordinates(group as String, name, version as String)

        pom {
            name = project.name
            description = project.description ?: "Akit library ${project.name}"
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}