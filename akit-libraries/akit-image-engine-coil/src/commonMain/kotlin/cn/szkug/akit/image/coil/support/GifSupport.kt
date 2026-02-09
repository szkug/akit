package cn.szkug.akit.image.coil.support

import okio.BufferedSource

private const val GIF_HEADER_SIZE = 6L

internal fun isGifSource(mimeType: String?, source: BufferedSource): Boolean {
    if (mimeType?.startsWith("image/gif", ignoreCase = true) == true) return true
    val header = try {
        source.peek().readByteArray(GIF_HEADER_SIZE)
    } catch (_: Exception) {
        return false
    }
    return isGifHeader(header)
}

internal fun isGifHeader(bytes: ByteArray): Boolean {
    if (bytes.size < GIF_HEADER_SIZE) return false
    return bytes[0] == 'G'.code.toByte() &&
        bytes[1] == 'I'.code.toByte() &&
        bytes[2] == 'F'.code.toByte() &&
        bytes[3] == '8'.code.toByte() &&
        (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
        bytes[5] == 'a'.code.toByte()
}

internal fun parseGifLoopCount(bytes: ByteArray): Int? {
    var index = 0
    while (index + 2 < bytes.size) {
        if (bytes[index] == 0x21.toByte() && bytes[index + 1] == 0xFF.toByte()) {
            val blockSize = bytes.getOrNull(index + 2)?.toInt() ?: return null
            val appStart = index + 3
            val appEnd = appStart + blockSize
            if (appEnd > bytes.size) return null
            val appId = bytes.copyOfRange(appStart, appEnd)
            if (appId.size >= 11) {
                val appName = appId.copyOfRange(0, 11).decodeToString()
                if (appName == "NETSCAPE2.0" || appName == "ANIMEXTS1.0") {
                    val subBlockStart = appEnd
                    if (subBlockStart + 3 >= bytes.size) return null
                    val subBlockSize = bytes[subBlockStart].toInt() and 0xFF
                    if (subBlockSize >= 3 && bytes[subBlockStart + 1] == 0x01.toByte()) {
                        val lo = bytes[subBlockStart + 2].toInt() and 0xFF
                        val hi = bytes[subBlockStart + 3].toInt() and 0xFF
                        return (hi shl 8) or lo
                    }
                }
            }
        }
        index++
    }
    return null
}

internal fun gifRepeatCount(loopCount: Int?): Int {
    if (loopCount == null) return 0
    return if (loopCount == 0) -1 else loopCount
}
