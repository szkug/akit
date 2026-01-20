pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// open projects accessors feature
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "akit"

// build logic
includeBuild("plugins")
includeBuild("akit-libraries/resources-gradle-plugin")

// libraries
fun includeAkitLibraries(vararg module: String) {
    include(module.map { ":akit-libraries$it" })
}

includeAkitLibraries(
    ":akit-graph",
    ":akit-image",
    ":glide-blur-module",
    ":resources-runtime",
    ":renderscript-toolkit-publish",
)

// apps
include(":apps:android")
include(":apps:cmp")
include(":benchmark")
