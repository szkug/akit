import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.project.alib)
    alias(libs.plugins.maven.publish)
    kotlin("kapt")
}

val renderScriptDir = "../../submodules/renderscript-intrinsics-replacement-toolkit/renderscript-toolkit"
val renderScriptSource = "$renderScriptDir/src/main"

android {
    namespace = "com.korilin.akit.glide.extensions.blur"

    sourceSets["main"].apply {
        java.srcDirs("$renderScriptSource/java")
    }

    defaultConfig.externalNativeBuild.cmake.cppFlags("-std=c++17")
    externalNativeBuild.cmake.path = file("$renderScriptSource/cpp/CMakeLists.txt")
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

    implementation(libs.glide.runtime)
    compileOnly(libs.glide.annotations)
    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}