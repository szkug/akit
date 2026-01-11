package cn.szkug.akit.graph.ninepatch

actual fun isRawNinePatch(
    source: NinePatchPixelSource?,
    chunkBytes: ByteArray?,
): Boolean {
    return parseNinePatch(source, chunkBytes).type == NinePatchType.Raw
}
