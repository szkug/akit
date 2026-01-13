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
includeBuild("akit-libraries/resources/gradle-plugin")

// libraries
includeBuild("akit-libraries") {
    dependencySubstitution {
        substitute(module("cn.szkug.akit:akit-graph")).using(project(":graph"))
        substitute(module("cn.szkug.akit:akit-image")).using(project(":image"))
        substitute(module("cn.szkug.akit:akit-resources-runtime")).using(project(":resources:runtime"))
    }
}

// apps
include(":apps:android")
include(":apps:cmp")
include(":benchmark")

// glide
include(":glide:extensions-ninepatch")
include(":glide:extensions-blur")
include(":renderscript-toolkit-publish")
