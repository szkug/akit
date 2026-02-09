plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.project.alib)
}

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            api(projects.akitLibraries.akitImage)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.atomicfu)
            api(projects.akitLibraries.akitGraph)
            api(projects.akitLibraries.resourcesRuntime)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.lottie)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "cn.szkug.akit.image.engine.coil3"
    buildFeatures.compose = true
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
