/**
 * Logs resource usage and call chains to help diagnose pruning decisions.
 */
class ResourceUsageLogger(
    private val enabled: Boolean,
    private val logger: MunchkinResourcesLog = MunchkinResourcesLog
) {
    /**
     * Emit usage information for each resource kind.
     *
     * This includes:
     * - ids referenced by compiled code
     * - ids missing from output resources (diagnostic)
     * - call chains extracted from IR
     */
    fun logUsage(used: UsedResources, available: AvailableResources) {
        if (!enabled) return
        for (kind in ResourceKind.values()) {
            logKind(kind, used.idsFor(kind), available.idsFor(kind), used.callsFor(kind))
        }
    }

    /**
     * Log usage and missing ids for a single resource kind.
     */
    private fun logKind(
        kind: ResourceKind,
        usedIds: Set<String>,
        availableIds: Set<String>,
        calls: Map<String, Set<String>>,
    ) {
        if (usedIds.isEmpty()) return
        val missing = usedIds.filterNot { availableIds.contains(it) }
        if (missing.isNotEmpty()) {
            logger.info(MunchkinResourcesMessages.pruneIdsNotFound(kind.token, missing.joinToString(MunchkinResourcesConstants.LIST_SEPARATOR)))
            if (availableIds.isEmpty()) {
                logger.info(MunchkinResourcesMessages.pruneOutputEmpty(kind.token))
            }
        }
        for (id in usedIds) {
            logger.info(MunchkinResourcesMessages.used(kind.token, id))
            val callSites = calls[id].orEmpty()
            for (call in callSites) {
                logger.info(MunchkinResourcesMessages.call(call))
            }
        }
    }
}
