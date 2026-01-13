import java.util.Properties
import kotlin.apply

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.0.0"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.gradle.plugin.kotlin)
    compileOnly(libs.gradle.plugin.android)
}

private val versionManager by lazy {
    val file = project.file("../version.properties")
    Properties().apply { load(file.inputStream()) }
}

version = versionManager["publish.version"] as String
group = "cn.szkug.akit.resources"

gradlePlugin {
    website.set("https://github.com/szkug/akit")
    vcsUrl.set("https://github.com/szkug/akit")

    plugins {
        register("AkitCmpResourcesPlugin") {
            id = "cn.szkug.akit.resources"
            implementationClass = "AkitCmpResourcesPlugin"
            displayName = "Akit Compose Multiplatform Resources Plugin"
            description = "Mange multiplatform resource & generate ResourceId"
            tags.set(listOf("kotlin", "Compose", "Kotlin Multiplatform"))
        }
    }
}
