import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.project.alib)
    id("com.korilin.akit.cmp-resources")
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "AkitCmp"
        }
    }

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

val xcodeConfiguration = (project.findProperty("CONFIGURATION") as String?)?.uppercase() ?: "DEBUG"
val sdkName = (project.findProperty("SDK_NAME") as String?) ?: "iphonesimulator"
val archs = (project.findProperty("ARCHS") as String?) ?: ""
val targetName = when {
    sdkName.startsWith("iphoneos") -> "iosArm64"
    archs.contains("x86_64") -> "iosX64"
    else -> "iosSimulatorArm64"
}
val framework = kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(xcodeConfiguration)

tasks.register<Sync>("syncAkitCmpFramework") {
    dependsOn(framework.linkTaskProvider)
    from(framework.outputDirectory)
    into(layout.buildDirectory.dir("xcode-frameworks"))
}

cmpResources {
    packageName.set("com.korilin.samples.compose.trace.cmp")
    androidNamespace.set("com.korilin.samples.compose.trace.cmp")
    iosResourcesDir.set(rootProject.layout.projectDirectory.dir("apps/ios/Resources"))
    iosResourcesPrefix.set("cmp-res")
}
