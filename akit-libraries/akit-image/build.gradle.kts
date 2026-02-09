plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.project.alib)
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            api(projects.akitLibraries.akitGraph)
            api(projects.akitLibraries.resourcesRuntime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.appcompat.resources)
        }
        iosMain.dependencies {
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "cn.szkug.akit.compose.image"

    compileSdk = AndroidSdkVersions.COMPILE

    defaultConfig {
        minSdk = AndroidSdkVersions.MIN
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
