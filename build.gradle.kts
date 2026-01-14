import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

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

val akitLibrariesDir = file("akit-libraries").toPath()
val akitVersionProperties = Properties().apply {
    file("akit-libraries/version.properties").inputStream().use { load(it) }
}
val akitPublishGroup = akitVersionProperties.getProperty("publish.group")
val akitPublishVersion = akitVersionProperties.getProperty("publish.version")
val mavenPublishPluginId = libs.plugins.maven.publish.get().pluginId

allprojects {
    tasks.withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }
}

subprojects {
    if (!projectDir.toPath().startsWith(akitLibrariesDir)) return@subprojects

    extensions.extraProperties["publish.group"] = akitPublishGroup
    extensions.extraProperties["publish.version"] = akitPublishVersion

    pluginManager.apply(mavenPublishPluginId)

    val project = this

    println("Config Publish: ${project.name}")

    extensions.configure<MavenPublishBaseExtension> {
        group = akitPublishGroup
        version = akitPublishVersion

        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()

        coordinates(group as String, name, version as String)

        pom {
            name = project.name
            description = project.description ?: "Akit library ${project.name}"
        }
    }
}
