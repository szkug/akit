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
            baseName = "MunchkinCmp"
            isStatic = true
            transitiveExport = false
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
            implementation(projects.apps.cmpLib2)
            api(projects.libs.resource.runtime)
            implementation(projects.libs.image.engineCoil)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(projects.libs.image.engineGlide)
        }
        iosMain {
            dependencies {
            }
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "munchkin.apps.cmp"

    compileSdk = AndroidSdkVersions.COMPILE

    defaultConfig {
        minSdk = AndroidSdkVersions.MIN
    }

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

cmpResources {
    packageName.set("munchkin.apps.cmp.host")
    androidNamespace.set("munchkin.apps.cmp")
    iosResourcesPrefix.set("MunchkinCmpHostRes")
    iosPruneUnused.set(true)
    iosPruneLogEnabled = true
}
