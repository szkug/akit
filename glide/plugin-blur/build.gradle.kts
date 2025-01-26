plugins {
    alias(libs.plugins.project.alib)
}

val renderScriptDir = "../../submodules/renderscript-intrinsics-replacement-toolkit/renderscript-toolkit"
val renderScriptSource = "$renderScriptDir/src/main"

android {
    namespace = "com.korilin.akit.glide.plugin.blur"

    sourceSets["main"].apply {
        java.srcDirs("$renderScriptSource/java")
    }

    defaultConfig.externalNativeBuild.cmake.cppFlags("-std=c++17")
    externalNativeBuild.cmake.path = file("$renderScriptSource/cpp/CMakeLists.txt")
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}