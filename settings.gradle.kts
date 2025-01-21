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

// apps
include(":app")
include(":benchmark")

// glide
include(":glide:compose-image")

// images
include(":image:renderscript")
