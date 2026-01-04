import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.project.alib)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.korilin.akit.glide.compose.image"

    buildFeatures.compose = true
    
    composeOptions.kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
}

mavenPublishing {

    val version = properties["publish.version"] as String
    val group = properties["publish.group"] as String + ".glide"

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group, "compose-image", version)

    pom {
        name = "Akit Glide Compose Image"
        description = "A Compose Image library base on Glide."
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.base)
    implementation("org.jetbrains.compose.components:components-resources:1.8.2")

    implementation(libs.bundles.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}