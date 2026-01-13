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
            implementation(compose.ui)
            api(projects.akitGraph)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            api(libs.androidx.appcompat.resources)
        }
        iosMain.dependencies {
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "cn.szkug.akit.resources.runtime"

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
