package munchkin.resources.loader

interface SvgaAsyncLoadData {
    val payload: BinaryPayload
}

data class BinaryAsyncLoadData(
    override val payload: BinaryPayload,
) : SvgaAsyncLoadData

interface SvgaAsyncRequestEngine {
    suspend fun requestSvga(
        engineContext: EngineContext,
        source: BinarySource,
    ): BinaryAsyncLoadData
}
