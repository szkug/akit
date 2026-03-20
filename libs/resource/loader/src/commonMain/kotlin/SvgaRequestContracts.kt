package munchkin.resources.loader

interface SvgaAsyncLoadData {
    val payload: BinaryPayload
}

data class BinaryAsyncLoadData(
    override val payload: BinaryPayload,
) : SvgaAsyncLoadData

interface SvgaAsyncRequestEngine<C: EngineContext> : RequestEngine<C> {
    suspend fun requestSvga(
        engineContext: C,
        source: BinarySource,
    ): BinaryAsyncLoadData
}
