plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    alias(libs.plugins.kotlin.compose)
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
    namespace = "cn.szkug.samples.compose.trace"
    compileSdk = 35


    defaultConfig {
        applicationId = "cn.szkug.samples.compose.trace"
        minSdk = 24
        targetSdk = 35
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
            isMinifyEnabled = true
            isShrinkResources = true
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
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

    // project modules
    implementation(projects.akitLibraries.akitImage)
    implementation(projects.akitLibraries.akitImageEngineGlide)
    implementation(projects.apps.cmp)
    implementation(projects.apps.cmpLib2)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.ui.test.manifest)
}
