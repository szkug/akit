plugins {
    alias(libs.plugins.project.alib)
}

android {
    namespace = "com.korilin.akit.glide.compose.image"

    buildFeatures.compose = true
    
    composeOptions.kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.base)

    implementation(libs.bundles.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}