package munchkin.resources.runtime

interface SvgaAsyncLoadData {
    val payload: BinaryPayload
}

data class BinaryAsyncLoadData(
    override val payload: BinaryPayload,
) : SvgaAsyncLoadData

interface RuntimeSvgaRequestEngine<C: RuntimeEngineContext> : RuntimeRequestEngine<C> {
    suspend fun requestSvga(
        engineContext: C,
        source: BinarySource,
    ): BinaryAsyncLoadData
}
