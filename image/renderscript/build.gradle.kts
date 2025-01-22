plugins {
    alias(libs.plugins.project.alib)
}

android {
    namespace = "com.korilin.akit.image.renderscript"

    defaultConfig.externalNativeBuild.cmake.cppFlags("-std=c++17")
    externalNativeBuild.cmake.path = file("src/main/cpp/CMakeLists.txt")
}


kotlin {

}

dependencies {
    implementation(libs.androidx.appcompat)
}