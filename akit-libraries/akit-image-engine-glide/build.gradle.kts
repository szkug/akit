plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.project.alib)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            api(projects.akitLibraries.akitImage)
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
    namespace = "cn.szkug.akit.image.engine.glide"
    buildFeatures.compose = true
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
