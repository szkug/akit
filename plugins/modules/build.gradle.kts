plugins {
    `kotlin-dsl`
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
        register("AlibPlugin") {
            id = "com.korilin.akit.alib"
            implementationClass = "AlibPlugin"
        }
    }
}
