plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.1.0"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.gradle.plugin.kotlin)
    compileOnly(libs.gradle.plugin.android)
    testImplementation(kotlin("test-junit5"))
}

group = "cn.szkug.munchkin.resources"

gradlePlugin {
    website.set("https://github.com/szkug/munchkin-cats/tree/main/libs/resource")
    vcsUrl.set("https://github.com/szkug/munchkin-cats")

    plugins {
        register("MunchkinCmpResourcesPlugin") {
            id = "cn.szkug.munchkin.resources"
            implementationClass = "MunchkinCmpResourcesPlugin"
            displayName = "Munchkin Resources Plugin"
            description = "KMP plugin and runtime for generating strongly typed Compose Multiplatform resources from Android style resource files ."
            tags.set(listOf("kotlin", "compose", "multiplatform", "resources"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
