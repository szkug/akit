/**
 * Centralized constants used by the resources Gradle plugin.
 *
 * This file keeps string literals, separators, and environment variable names
 * in one place to reduce duplication and make changes predictable.
 */
object AkitResourcesConstants {
    const val LOG_PREFIX = "AkitResources"

    const val RESOURCE_ROOT_DIR = "compose-resources"
    const val KLIB_TEMP_DIR = "compose-resources/klib-prune"

    const val RES_FILE_IOS = "Res.ios.kt"
    const val RES_FILE_COMMON = "Res.kt"
    const val STRINGS_FILE = "Localizable.strings"
    const val STRINGS_DICT_FILE = "Localizable.stringsdict"
    const val LPROJ_SUFFIX = ".lproj"

    const val KLIB_COMMAND_DUMP_IR = "dump-ir"
    const val KLIB_EXTENSION = "klib"

    const val ENV_BUILT_PRODUCTS_DIR = "BUILT_PRODUCTS_DIR"
    const val ENV_UNLOCALIZED_RESOURCES_FOLDER_PATH = "UNLOCALIZED_RESOURCES_FOLDER_PATH"

    const val CALL_CHAIN_FILE_SEPARATOR = " | "
    const val CALL_CHAIN_STEP_SEPARATOR = " -> "
    const val CALL_CHAIN_LINE_SEPARATOR = " :: "
    const val CALL_CHAIN_TOP_LEVEL = "<top-level>"

    const val PATH_SEPARATOR = "/"
    const val LIST_SEPARATOR = ", "

    const val GENERATED_ID_PREFIX = "res_"
    const val GENERATED_ID_EMPTY = "res_unnamed"

    const val STRINGS_ASSIGN = " = "
    const val STRINGS_LINE_END = "\";\n"
}

/**
 * Centralized user-visible messages for resource pruning.
 *
 * Keep message formatting here so the rest of the code focuses on behavior.
 */
object AkitResourcesMessages {
    fun syncSkipMissingEnv(): String =
        "Skipping ${AkitResourcesGradleConstants.TASK_SYNC_XCODE}: missing " +
            "BUILT_PRODUCTS_DIR/UNLOCALIZED_RESOURCES_FOLDER_PATH."

    const val PRUNE_SKIP_NO_USAGE = "prune skipped: no usage detected in KLIB IR."
    const val PRUNE_SKIP_NO_DRAWABLE_RAW = "prune skipped: no drawable/raw usage detected."
    const val PRUNE_SKIP_NO_STRINGS = "prune skipped: no string usage detected."
    const val PRUNE_SKIP_KLIB_TOOL_MISSING = "prune skipped: klib tool not found."
    const val PRUNE_SKIP_NO_KLIB_FILES = "prune skipped: no klib files found."

    /** Format a message when an explicit KLIB tool path is missing. */
    fun pruneSkipKlibToolAt(path: String): String =
        "prune skipped: klib tool not found at $path."

    /** Format a message when KLIB IR scanning begins. */
    fun pruneScanStart(count: Int): String =
        "prune: scanning KLIB IR from $count libraries."

    /** Format a message when the `klib dump-ir` command fails. */
    fun pruneDumpIrFailed(path: String, exitCode: Int): String =
        "prune warning: klib dump-ir failed for $path (exit=$exitCode)."

    /** Format a message when referenced ids are missing from output resources. */
    fun pruneIdsNotFound(typeLabel: String, ids: String): String =
        "prune warning: $typeLabel ids not found in output resources: $ids"

    /** Format a message when no output resources are found for a type. */
    fun pruneOutputEmpty(typeLabel: String): String =
        "prune warning: $typeLabel output resources set is empty."

    /** Format a message for a used resource id. */
    fun used(typeLabel: String, id: String): String =
        "used $typeLabel: $id"

    /** Format a message for a call chain line. */
    fun call(chain: String): String =
        "call: $chain"

    /** Format a message when a drawable file is removed. */
    fun prunedDrawable(path: String): String =
        "pruned drawable: $path"

    /** Format a message when a raw file is removed. */
    fun prunedRaw(path: String): String =
        "pruned raw: $path"

    /** Format a message when a string entry is removed. */
    fun prunedString(path: String, key: String): String =
        "pruned string: $path key=$key"
}

/**
 * Regex patterns used for IR scanning and parsing.
 */
object AkitResourcesRegex {
    val IR_SIGNATURE = Regex("""Res\.(strings|drawable|raw)[.#]<get-([A-Za-z0-9_]+)>""")
    val IR_PROPERTY = Regex("""Res\.(strings|drawable|raw)\.([A-Za-z0-9_]+)\.<get-([A-Za-z0-9_]+)>""")
    val IR_DIRECT = Regex("""Res\.(strings|drawable|raw)\.<get-([A-Za-z0-9_]+)>""")
    val IR_ANY_PROPERTY = Regex("""Res\.([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\.<get-([A-Za-z0-9_]+)>""")
    val IR_FUN = Regex("""^\s*(FUN|CONSTRUCTOR)\b""")
    val IR_FILE = Regex("""^\s*FILE fqName:.*fileName:(.+)$""")
    val IR_SIGNATURE_BLOCK = Regex("""signature:\[[^\]]*<-\s*([^\]]+)\]""")
    val IR_NAME = Regex("""\bname:([^\s]+)""")
    val STRINGS_ENTRY =
        Regex("\"((?:\\\\.|[^\"\\\\])*)\"\\s*=\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*;")
    val DRAWABLE_SCALE_SUFFIX = Regex("@[23]x$")
}

/**
 * Simple logger helper to keep all output consistent and prefixed.
 */
object AkitResourcesLog {
    /** Print a line with a consistent prefix for easier filtering. */
    fun info(message: String) {
        println("${AkitResourcesConstants.LOG_PREFIX} $message")
    }
}
