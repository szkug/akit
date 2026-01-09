import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            api(libs.androidx.appcompat.resources)
        }
        iosMain.dependencies {
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "cn.szkug.akit.graph"

    buildFeatures.compose = true

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

mavenPublishing {

    val version = properties["publish.version"] as String
    val group = properties["publish.group"] as String

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group, "akit-graph", version)

    pom {
        name = "Akit Graph"
        description = "Akit Multiplatform Graph"
    }
}
