plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.project.alib)
    kotlin("kapt")
}
val renderScriptVersion = properties["renderscript.toolkit.version"] as? String ?: "1.0.1"

android {
    namespace = "cb.szkug.akit.glide.extensions.blur"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)
    api(projects.akitLibraries.renderscriptToolkitPublish)
    compileOnly(libs.glide.annotations)
    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
