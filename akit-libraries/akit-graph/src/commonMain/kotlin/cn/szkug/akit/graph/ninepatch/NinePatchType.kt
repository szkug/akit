package cn.szkug.akit.graph.ninepatch

enum class NinePatchType {
    Chunk,
    Raw,
    None,
}

fun determineNinePatchType(
    source: NinePatchPixelSource?,
    chunkBytes: ByteArray?,
): NinePatchType {
    if (chunkBytes != null && isNinePatchChunk(chunkBytes)) {
        return NinePatchType.Chunk
    }
    return if (NinePatchChunk.isRawNinePatchSource(source)) {
        NinePatchType.Raw
    } else {
        NinePatchType.None
    }
}

expect fun isNinePatchChunk(bytes: ByteArray?): Boolean
