/**
 * Constants for Gradle task/configuration names and plugin wiring.
 *
 * Keeping these in one place avoids string duplication and makes it easier to
 * change names without missing references.
 */
object AkitResourcesGradleConstants {
    const val TASK_PREPARE_EMPTY = "prepareComposeMultiplatformResourceEmptyResDir"
    const val TASK_GENERATE = "generateComposeMultiplatformResourceResources"
    const val TASK_PREPARE_COMPOSE = "prepareComposeMultiplatformResourceComposeResources"
    const val TASK_SYNC_XCODE = "syncComposeMultiplatformResourceResourcesForXcode"
    const val TASK_EMBED_XCODE = "embedAndSignAppleFrameworkForXcode"

    const val CONFIG_ELEMENTS = "cmpComposeResourcesElements"
    const val CONFIG_CLASSPATH = "cmpComposeResourcesClasspath"

    const val GRADLE_PROP_OUTPUT_DIR = "cmp.ios.resources.outputDir"
    const val COCOAPODS_OUTPUT_DIR = "compose/cocoapods/compose-resources"

    const val SYNC_TASK_PREFIX = "syncComposeResourcesFor"
    const val SYNC_POD_TASK_PREFIX = "syncPodComposeResourcesFor"
    const val SYNC_IOS_TOKEN = "Ios"
    const val SYNC_XCODE_TOKEN = "Xcode"

    const val TASK_SYNC_FRAMEWORK = "syncFramework"
    const val TASK_POD_PUBLISH_PREFIX = "podPublish"
    const val TASK_POD_PUBLISH_SUFFIX = "XCFramework"

    const val CONFIG_MAIN_IMPLEMENTATION = "MainImplementation"
    const val CONFIG_MAIN_API = "MainApi"

    const val SOURCESET_COMMON = "commonMain"
    const val SOURCESET_ANDROID = "androidMain"
    const val SOURCESET_IOS = "iosMain"

    const val GENERATED_RES_ROOT = "generated/compose-resources"
    const val GENERATED_CODE_DIR = "code"
    const val GENERATED_IOS_RESOURCES_DIR = "iosResources"
    const val GENERATED_EMPTY_RES_DIR = "empty-res"

    const val KLIB_BASE_DIR = "classes/kotlin"
    const val KLIB_DIR_SUFFIX = "main/klib"
    const val KLIB_DEFAULT_VARIANT = "default"

    const val TARGET_IOS_ARM64 = "iosArm64"
    const val TARGET_IOS_X64 = "iosX64"
    const val TARGET_IOS_SIM_ARM64 = "iosSimulatorArm64"
}
