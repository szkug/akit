import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.project.alib)
    id("cn.szkug.akit.resources")
}

val publishVersion = properties["publish.version"] as String
val publishGroup = properties["publish.group"] as String

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "AkitCmp"
            isStatic = true

        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            api("$publishGroup:akit-image:$publishVersion")
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
    namespace = "cn.szkug.akit.apps.cmp"

    compileSdk = AndroidSdkVersions.COMPILE

    defaultConfig {
        minSdk = AndroidSdkVersions.MIN
    }

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

cmpResources {
    packageName.set("cn.szkug.akit.apps.cmp")
    androidNamespace.set("cn.szkug.akit.apps.cmp")
    iosResourcesPrefix.set("cmp-res")
    iosFrameworkName.set("AkitCmp")
    iosFrameworkBundleId.set("cn.szkug.akit.apps.cmp")
}
