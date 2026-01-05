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
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.5.0")
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

// apps
include(":app")
include(":benchmark")

// glide
include(":akit-image")
include(":akit-ninepatch")
include(":glide:extensions-ninepatch")
include(":glide:extensions-blur")
include(":renderscript-toolkit-publish")
