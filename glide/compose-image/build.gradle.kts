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
        url.set("https://github.com/korilin/akit")

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://github.com/korilin/akit/blob/main/LICENSE"
            }
        }

        developers {
            developer {
                id = "korilin"
                name = "Kori"
                email = "korilin.dev@gmail.com"
            }
        }

        scm {
            url.set("https://github.com/korilin/akit")
            connection.set("scm:git:git://github.com/korilin/akit.git")
            developerConnection.set("scm:git:ssh://git@github.com/korilin/akit.git")
        }
    }
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