import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.project.alib)
    alias(libs.plugins.maven.publish)
}

val renderScriptDir = "../submodules/renderscript-intrinsics-replacement-toolkit/renderscript-toolkit"
val renderScriptSource = "$renderScriptDir/src/main"

version = "1.0.1"
group = properties["publish.group"] as String

android {
    namespace = "cn.szkug.renderscript.toolkit"

    sourceSets["main"].apply {
        java.srcDirs("$renderScriptSource/java")
    }

    defaultConfig.externalNativeBuild.cmake.cppFlags("-std=c++17")
    externalNativeBuild.cmake.path = file("$renderScriptSource/cpp/CMakeLists.txt")
}

mavenPublishing {

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group as String, "renderscript-toolkit", version as String)

    pom {
        name = "Akit renderscript toolkit"
        description = "publish renderscript toolkit lib"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}