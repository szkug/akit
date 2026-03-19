package munchkin.svga

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

internal actual fun inflateZlib(bytes: ByteArray): ByteArray = inflate(bytes, nowrap = false)

internal actual fun inflateRawDeflate(bytes: ByteArray, expectedSize: Int): ByteArray = inflate(bytes, nowrap = true)

private fun inflate(bytes: ByteArray, nowrap: Boolean): ByteArray {
    val inflater = Inflater(nowrap)
    return try {
        inflater.setInput(bytes)
        val output = ByteArrayOutputStream(bytes.size.coerceAtLeast(1024))
        val buffer = ByteArray(8 * 1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count > 0) {
                output.write(buffer, 0, count)
                continue
            }
            if (inflater.needsInput() || inflater.needsDictionary()) break
        }
        output.toByteArray()
    } catch (error: DataFormatException) {
        throw IllegalStateException("SVGA inflate failed.", error)
    } finally {
        inflater.end()
    }
}
