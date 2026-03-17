dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// gradle feature
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "munchkin-build-logic"

include("modules")
