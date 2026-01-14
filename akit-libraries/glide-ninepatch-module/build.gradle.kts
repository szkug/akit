plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.project.alib)
    kotlin("kapt")
}


android {
    namespace = "cn.szkug.akit.glide.extensions.ninepatch"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)
    implementation(projects.akitLibraries.akitGraph)
    compileOnly(libs.glide.annotations)
    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
