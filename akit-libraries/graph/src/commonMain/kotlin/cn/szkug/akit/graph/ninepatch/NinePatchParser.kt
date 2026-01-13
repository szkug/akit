package cn.szkug.akit.graph.ninepatch

data class NinePatchParseResult(
    val type: NinePatchType,
    val chunk: NinePatchChunk?,
)

fun parseNinePatch(
    source: NinePatchPixelSource?,
    chunkBytes: ByteArray?,
): NinePatchParseResult {
    val type = determineNinePatchType(source, chunkBytes)
    val chunk = when (type) {
        NinePatchType.Chunk -> chunkBytes?.let { NinePatchChunk.parse(it) }
        NinePatchType.Raw -> NinePatchChunk.createChunkFromRawSource(source)
        NinePatchType.None -> null
    }
    return NinePatchParseResult(type, chunk)
}

expect fun isRawNinePatch(
    source: NinePatchPixelSource?,
    chunkBytes: ByteArray?,
): Boolean
