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
val renderScriptVersion = properties["renderscript.toolkit.version"] as? String ?: "1.0.1"

android {
    namespace = "cb.szkug.akit.glide.extensions.blur"
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(glideGroup, "extension-blur", publishVersion)

    pom {
        name = "Akit Glide Blur Extension"
        description = "A Glide blur extension."
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.glide.runtime)
    api("$publishGroup:renderscript-toolkit:$renderScriptVersion")
    compileOnly(libs.glide.annotations)
    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
