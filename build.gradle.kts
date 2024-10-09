import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.test) apply false
}

fun KotlinCompile.configOption() = kotlinOptions.apply {
    jvmTarget = "17"
}


allprojects {
    tasks.withType<KotlinCompile> {
        configOption()
    }
}