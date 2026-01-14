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
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            api(libs.androidx.appcompat.resources)
            implementation(libs.bundles.glide)
            implementation(projects.akitLibraries.glideNinepatchModule)
        }
        iosMain.dependencies {
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.darwin)
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "cn.szkug.akit.compose.image"

    compileSdk = AndroidSdkVersions.COMPILE

    defaultConfig {
        minSdk = AndroidSdkVersions.MIN
    }

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}