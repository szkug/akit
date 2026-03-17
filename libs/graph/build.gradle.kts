plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

description = "Compose Multiplatform graphics helpers shared across Android and iOS."

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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            api(libs.androidx.appcompat.resources)
            implementation(libs.lottie)
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "munchkin.graph"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild.cmake.cppFlags("-std=c++17")
    }

    buildFeatures.compose = true
    externalNativeBuild.cmake.path = file("src/cpp/renderscript-toolkit/CMakeLists.txt")
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
