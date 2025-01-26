package com.korilin.akit.plugin.ninepatch

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.min


class ByteBufferBackedInputStream(private var buffer: ByteBuffer) : InputStream() {
    @Throws(IOException::class)
    override fun read(): Int {
        if (!buffer.hasRemaining()) {
            return -1
        }
        return buffer.get().toInt() and 0xFF
    }

    @Throws(IOException::class)
    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (!buffer.hasRemaining()) {
            return -1
        }

        val readLen = min(len, buffer.remaining())
        buffer[bytes, off, readLen]
        return readLen
    }
}

