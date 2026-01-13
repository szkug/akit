import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `kotlin-dsl`
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.gradle.plugin.kotlin)
    compileOnly(libs.gradle.plugin.android)
}

gradlePlugin {
    plugins {
        register("CmpResourcesPlugin") {
            id = "cn.szkug.akit.cmp-resources"
            implementationClass = "CmpResourcesPlugin"
        }
    }
}

mavenPublishing {

    val version = properties["publish.version"] as String
    val group = properties["publish.group"] as String

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group, "akit-resources-plugin", version)

    pom {
        name = "Akit Resources Plugin"
        description = "Akit Multiplatform Resources Gradle Plugin"
    }
}
