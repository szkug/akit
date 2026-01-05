import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.project.alib)
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "cn.szkug.graphics.ninepatch"

    compileSdk = AndroidSdkVersions.COMPILE

    defaultConfig {
        minSdk = AndroidSdkVersions.MIN
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

mavenPublishing {

    val version = properties["publish.version"] as String
    val group = properties["publish.group"] as String

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group, "akit-ninepatch", version)

    pom {
        name = "Akit Nine Patch"
        description = "KMP NinePatch chunk parser and helpers."
    }
}
