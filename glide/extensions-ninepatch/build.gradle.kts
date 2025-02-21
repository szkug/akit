import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.project.alib)
    alias(libs.plugins.maven.publish)
    kotlin("kapt")
}


android {
    namespace = "com.korilin.akit.glide.extensions.ninepatch"

    sourceSets["main"].apply {
        java.srcDirs("../../submodules/NinePatchChunk/NinePatchChunk/Library/src/main/java")
    }
}

mavenPublishing {

    val version = properties["publish.version"] as String
    val group = properties["publish.group"] as String + ".glide"

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group, "extension-ninepatch", version)

    pom {
        name = "Akit Glide Ninepatch Extension"
        description = "A Glide blur extension."
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