plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

description = "Coil-based image and SVGA loader engines for Munchkin Resource Loader."

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":libs:resource:loader"))
            api(project(":libs:graph"))
            api(project(":libs:resource:runtime"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.atomicfu)
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
    namespace = "munchkin.resources.loader.coil"
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
