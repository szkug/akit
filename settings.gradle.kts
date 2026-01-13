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
includeBuild("akit-libraries/resources-gradle-plugin")

// libraries
includeBuild("akit-libraries") {
    dependencySubstitution {
        substitute(module("cn.szkug.akit:akit-graph")).using(project(":akit-graph"))
        substitute(module("cn.szkug.akit:akit-image")).using(project(":akit-image"))
        substitute(module("cn.szkug.akit:akit-resources-runtime")).using(project(":resources-runtime"))
        substitute(module("cn.szkug.akit:glide-ninepatch-module")).using(project(":glide-ninepatch-module"))
        substitute(module("cn.szkug.akit:glide-blur-module")).using(project(":glide-blur-module"))
    }
}

// apps
include(":apps:android")
include(":apps:cmp")
include(":benchmark")
