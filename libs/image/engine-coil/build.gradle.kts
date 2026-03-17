plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

description = "Coil 3 engine for Munchkin Image on Android and iOS."

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":libs:image:image"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.atomicfu)
            api(project(":libs:graph"))
            api(project(":libs:resource:runtime"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.lottie)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "munchkin.image.coil"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
    }

    buildFeatures.compose = true
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
