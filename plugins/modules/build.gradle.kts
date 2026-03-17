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
            id = "cn.szkug.munchkin.alib"
            implementationClass = "AlibPlugin"
        }
    }
}
