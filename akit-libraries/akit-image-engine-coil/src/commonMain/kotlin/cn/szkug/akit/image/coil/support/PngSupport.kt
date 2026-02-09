package cn.szkug.akit.image.coil.support

import okio.BufferedSource

internal fun isPng(mimeType: String?, bytes: ByteArray): Boolean {
    if (mimeType?.startsWith("image/png", ignoreCase = true) == true) return true
    if (bytes.size < 8) return false
    return bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() &&
        bytes[3] == 0x47.toByte() &&
        bytes[4] == 0x0D.toByte() &&
        bytes[5] == 0x0A.toByte() &&
        bytes[6] == 0x1A.toByte() &&
        bytes[7] == 0x0A.toByte()
}

internal fun isPngSource(mimeType: String?, source: BufferedSource): Boolean {
    if (mimeType?.startsWith("image/png", ignoreCase = true) == true) return true
    val header = try {
        source.peek().readByteArray(8)
    } catch (_: Exception) {
        return false
    }
    return isPng(mimeType, header)
}
