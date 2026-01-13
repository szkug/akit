plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.project.alib)
}

val renderScriptDir = "../../submodules/renderscript-intrinsics-replacement-toolkit/renderscript-toolkit"
val renderScriptSource = "$renderScriptDir/src/main"

android {
    namespace = "cn.szkug.renderscript.toolkit"

    sourceSets["main"].apply {
        java.srcDirs("$renderScriptSource/java")
    }

    defaultConfig.externalNativeBuild.cmake.cppFlags("-std=c++17")
    externalNativeBuild.cmake.path = file("$renderScriptSource/cpp/CMakeLists.txt")
}


dependencies {
    implementation(libs.androidx.core.ktx)
}
