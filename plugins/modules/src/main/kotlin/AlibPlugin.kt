import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension


class AlibPlugin : Plugin<Project>  {

    override fun apply(target: Project)  = with(target) {
        configPlugin()
        configExtension()
    }

    private fun Project.configPlugin() = with(pluginManager) {
        apply("com.android.library")
    }

    private fun Project.configExtension() = with(extensions) {
        configure<LibraryExtension> {

            compileSdk = AndroidSdkVersions.COMPILE

            defaultConfig {
                minSdk = AndroidSdkVersions.MIN
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables.useSupportLibrary = true
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        " proguard-rules.pro"
                    )
                }
            }

            sourceSets["main"].apply {
                java.srcDirs("src/main/java", "src/main/kotlin")
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    }
}