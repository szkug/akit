plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

description = "Glide engine for Munchkin Image on Android."

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            api(project(":libs:image:image"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(compose.ui)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.bundles.glide)
            compileOnly(libs.glide.annotations)
            implementation(libs.lottie)
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "munchkin.image.glide"
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
