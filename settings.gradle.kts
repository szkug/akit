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

rootProject.name = "munchkin-cats"

// build logic
includeBuild("plugins")

// libraries
include(":libs:graph")
include(":libs:image:image")
include(":libs:resource:loader-engine-coil")
include(":libs:resource:loader-engine-glide")
include(":libs:resource:runtime")
include(":libs:svga")

// apps
include(":apps:android")
include(":apps:cmp")
include(":apps:cmp-lib")
include(":apps:cmp-lib2")
include(":benchmark")
