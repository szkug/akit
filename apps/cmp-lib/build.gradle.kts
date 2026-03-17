import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.project.alib)
    id("cn.szkug.munchkin.resources")
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "MunchkinCmpLibRes"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            api(projects.libs.image.image)
            api(projects.libs.graph)
            api(projects.libs.resource.runtime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
        }
        iosMain {}
    }

    jvmToolchain(17)
}

android {
    namespace = "munchkin.apps.cmp.lib"

    compileSdk = AndroidSdkVersions.COMPILE

    defaultConfig {
        minSdk = AndroidSdkVersions.MIN
    }

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

cmpResources {
    packageName.set("munchkin.apps.cmp")
    androidNamespace.set("munchkin.apps.cmp.lib")
    androidExtraResDir.set(layout.projectDirectory.dir("src/androidMain/res"))
    iosResourcesPrefix.set("MunchkinCmpLibRes")
}
