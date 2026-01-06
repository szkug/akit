plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    id("com.korilin.akit.cmp-resources")
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
            implementation(compose.material3)
            implementation(projects.akitImage)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "com.korilin.samples.compose.trace.cmp"

    compileSdk = AndroidSdkVersions.COMPILE

    defaultConfig {
        minSdk = AndroidSdkVersions.MIN
    }

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

cmpResources {
    packageName.set("com.korilin.samples.compose.trace.cmp")
    androidNamespace.set("com.korilin.samples.compose.trace.cmp")
    iosResourcesDir.set(rootProject.layout.projectDirectory.dir("apps/ios/src/iosMain/resources"))
    iosResourcesPrefix.set("cmp-res")
}
