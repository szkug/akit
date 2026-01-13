import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.project.alib)
    alias(libs.plugins.maven.publish)
    kotlin("kapt")
}

val publishVersion = properties["publish.version"] as String
val publishGroup = properties["publish.group"] as String
val glideGroup = "$publishGroup.glide"


android {
    namespace = "cn.szkug.akit.glide.extensions.ninepatch"
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(glideGroup, "extension-ninepatch", publishVersion)

    pom {
        name = "Akit Glide Ninepatch Extension"
        description = "A Glide blur extension."
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)
    implementation(projects.graph)
    compileOnly(libs.glide.annotations)
    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
