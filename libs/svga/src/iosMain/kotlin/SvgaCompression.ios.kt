package munchkin.svga

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.zlib.MAX_WBITS
import platform.zlib.ZLIB_VERSION
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream

internal actual fun inflateZlib(bytes: ByteArray): ByteArray = inflatePlatform(bytes, MAX_WBITS)

internal actual fun inflateRawDeflate(bytes: ByteArray, expectedSize: Int): ByteArray = inflatePlatform(bytes, -MAX_WBITS)

@OptIn(ExperimentalForeignApi::class)
private fun inflatePlatform(bytes: ByteArray, windowBits: Int): ByteArray = memScoped {
    val stream = alloc<z_stream>()
    stream.zalloc = null
    stream.zfree = null
    stream.opaque = null
    stream.avail_in = 0u
    stream.next_in = null
    val init = inflateInit2_(stream.ptr, windowBits, ZLIB_VERSION, sizeOf<z_stream>().convert())
    check(init == Z_OK) { "SVGA inflate init failed: $init" }
    val chunks = ArrayList<ByteArray>()
    var total = 0
    val outputBuffer = ByteArray(8 * 1024)
    bytes.usePinned { inputPinned ->
        stream.avail_in = bytes.size.convert()
        stream.next_in = inputPinned.addressOf(0).reinterpret<UByteVar>()
        var result = Z_OK
        while (true) {
            outputBuffer.usePinned { outputPinned ->
                stream.avail_out = outputBuffer.size.convert()
                stream.next_out = outputPinned.addressOf(0).reinterpret<UByteVar>()
                result = inflate(stream.ptr, Z_NO_FLUSH)
                val produced = outputBuffer.size - stream.avail_out.toInt()
                if (produced > 0) {
                    chunks += outputBuffer.copyOf(produced)
                    total += produced
                }
            }
            when (result) {
                Z_STREAM_END -> break
                Z_OK -> {
                    if (stream.avail_in.toInt() == 0 && stream.avail_out.toInt() > 0) break
                }
                else -> error("SVGA inflate failed: $result")
            }
        }
    }
    inflateEnd(stream.ptr)
    val merged = ByteArray(total)
    var offset = 0
    chunks.forEach { chunk ->
        chunk.copyInto(merged, destinationOffset = offset)
        offset += chunk.size
    }
    merged
}
