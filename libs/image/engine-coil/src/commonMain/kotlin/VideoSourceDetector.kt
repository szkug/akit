package munchkin.image.coil.support

import okio.BufferedSource

private const val MP4_HEADER_PROBE_BYTES = 12L

internal fun isLikelyVideoSource(mimeType: String?, source: BufferedSource): Boolean {
    if (mimeType?.startsWith("video/", ignoreCase = true) == true) return true
    val header = try {
        source.peek().readByteArray(MP4_HEADER_PROBE_BYTES)
    } catch (_: Exception) {
        return false
    }
    if (header.size < MP4_HEADER_PROBE_BYTES) return false
    return header[4] == 'f'.code.toByte() &&
        header[5] == 't'.code.toByte() &&
        header[6] == 'y'.code.toByte() &&
        header[7] == 'p'.code.toByte()
}
