plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

description = "KMP plugin and runtime for generating strongly typed Compose Multiplatform resources from Android style resource files."

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            api(libs.androidx.appcompat.resources)
        }
        iosMain.dependencies {
            api(project(":libs:graph"))
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "munchkin.resources.runtime"
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
