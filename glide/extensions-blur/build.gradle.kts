import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.project.alib)
    alias(libs.plugins.maven.publish)
    kotlin("kapt")
}

android {
    namespace = "cb.szkug.akit.glide.extensions.blur"
}

mavenPublishing {

    val version = properties["publish.version"] as String
    val group = properties["publish.group"] as String + ".glide"

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group, "extension-blur", version)

    pom {
        name = "Akit Glide Blur Extension"
        description = "A Glide blur extension."
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)
    api(projects.renderscriptToolkitPublish)
    compileOnly(libs.glide.annotations)
    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}