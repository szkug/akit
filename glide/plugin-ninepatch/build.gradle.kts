plugins {
    alias(libs.plugins.project.alib)
    kotlin("kapt")
}


android {
    namespace = "com.korilin.akit.glide.plugin.ninepatch"

    sourceSets["main"].apply {
        java.srcDirs("../../submodules/NinePatchChunk/NinePatchChunk/Library/src/main/java")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)
    compileOnly(libs.glide.annotations)
    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}