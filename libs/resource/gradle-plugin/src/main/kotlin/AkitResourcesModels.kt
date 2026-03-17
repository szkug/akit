/**
 * Resource type classification used by the pruning pipeline.
 */
enum class ResourceKind(val token: String) {
    DRAWABLE("drawable"),
    RAW("raw"),
    STRINGS("strings");

    companion object {
        /**
         * Map the token seen in IR to a [ResourceKind].
         */
        fun fromToken(token: String): ResourceKind? = values().firstOrNull { it.token == token }
    }
}

/**
 * Output of the IR scanner: resource ids referenced by compiled code plus call sites.
 */
data class UsedResources(
    val drawable: Set<String>,
    val raw: Set<String>,
    val strings: Set<String>,
    val drawableCalls: Map<String, Set<String>>,
    val rawCalls: Map<String, Set<String>>,
    val stringCalls: Map<String, Set<String>>,
) {
    /**
     * Whether all categories are empty (no usage found).
     */
    fun isEmpty(): Boolean = drawable.isEmpty() && raw.isEmpty() && strings.isEmpty()

    /**
     * Return ids by kind for generic logging and pruning.
     */
    fun idsFor(kind: ResourceKind): Set<String> = when (kind) {
        ResourceKind.DRAWABLE -> drawable
        ResourceKind.RAW -> raw
        ResourceKind.STRINGS -> strings
    }

    /**
     * Return call chains by kind for debugging.
     */
    fun callsFor(kind: ResourceKind): Map<String, Set<String>> = when (kind) {
        ResourceKind.DRAWABLE -> drawableCalls
        ResourceKind.RAW -> rawCalls
        ResourceKind.STRINGS -> stringCalls
    }
}

/**
 * Output of the resources directory scan: resource ids present in the final output.
 */
data class AvailableResources(
    val drawable: Set<String>,
    val raw: Set<String>,
    val strings: Set<String>,
) {
    /**
     * Return ids by kind for generic logging and pruning.
     */
    fun idsFor(kind: ResourceKind): Set<String> = when (kind) {
        ResourceKind.DRAWABLE -> drawable
        ResourceKind.RAW -> raw
        ResourceKind.STRINGS -> strings
    }
}
