plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    signingConfigs {
        val debug = getByName("debug") {
            storeFile = file("${rootDir.absolutePath}/key-store")
            storePassword = "000000"
            keyAlias = "key0"
            keyPassword = "000000"
        }
        create("release") {
            initWith(debug)
        }
    }
    namespace = "com.korilin.samples.compose.trace"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.korilin.samples.compose.trace"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.base)
    implementation(libs.androidx.material3)

    implementation(libs.accompanist.drawablepainter)
    implementation(libs.recyclerview)

    kapt(libs.glide.compiler)

    implementation(libs.bundles.glide)
    implementation(libs.glide.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.androidx.runtime.tracing)

    // project module
    implementation(projects.glide.composeImage)
    implementation(projects.glide.pluginNinepatch)
    implementation(projects.glide.pluginBlur)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}