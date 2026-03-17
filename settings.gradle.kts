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

rootProject.name = "munchkin-sample"

// build logic
includeBuild("plugins")
includeBuild("libs/graph")
includeBuild("libs/image")
includeBuild("libs/resource")

// apps
include(":apps:android")
include(":apps:cmp")
include(":apps:cmp-lib")
include(":apps:cmp-lib2")
include(":benchmark")
