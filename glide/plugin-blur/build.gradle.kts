plugins {
    alias(libs.plugins.project.alib)
}


android {
    namespace = "com.korilin.akit.glide.plugin.blur"
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)
    implementation(projects.image.renderscript)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}