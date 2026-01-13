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
    includeBuild("../plugins")
}

plugins {
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

// open projects accessors feature
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "akit-libraries"

include(":akit-graph")
include(":akit-image")
include(":glide-blur-module")
include(":glide-ninepatch-module")
include(":resources-runtime")
include(":renderscript-toolkit-publish")
