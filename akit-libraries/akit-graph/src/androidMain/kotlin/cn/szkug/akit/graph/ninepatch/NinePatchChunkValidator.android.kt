package cn.szkug.akit.graph.ninepatch

actual fun isNinePatchChunk(bytes: ByteArray?): Boolean {
    return android.graphics.NinePatch.isNinePatchChunk(bytes)
}
