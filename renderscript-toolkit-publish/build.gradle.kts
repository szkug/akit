import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.project.alib)
    alias(libs.plugins.maven.publish)
}

val renderScriptDir = "../submodules/renderscript-intrinsics-replacement-toolkit/renderscript-toolkit"
val renderScriptSource = "$renderScriptDir/src/main"
val renderScriptVersion = properties["renderscript.toolkit.version"] as? String ?: "1.0.1"

version = renderScriptVersion
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

    coordinates(group as String, "renderscript-toolkit", renderScriptVersion)

    pom {
        name = "Akit renderscript toolkit"
        description = "publish renderscript toolkit lib"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
