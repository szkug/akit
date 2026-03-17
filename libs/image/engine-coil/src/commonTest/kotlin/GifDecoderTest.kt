package munchkin.image.coil.support

import kotlin.test.Test
import kotlin.test.assertEquals

class GifDecoderTest {
    @Test
    fun gifRepeatCountReturnsForeverForZeroLoopCount() {
        val bytes = byteArrayOf(
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
            0x21, 0xFF.toByte(), 0x0B,
            'N'.code.toByte(), 'E'.code.toByte(), 'T'.code.toByte(), 'S'.code.toByte(), 'C'.code.toByte(), 'A'.code.toByte(), 'P'.code.toByte(), 'E'.code.toByte(), '2'.code.toByte(), '.'.code.toByte(), '0'.code.toByte(),
            0x03, 0x01, 0x00, 0x00, 0x00,
        )

        assertEquals(-1, gifRepeatCount(bytes))
    }

    @Test
    fun gifRepeatCountReturnsLoopCount() {
        val bytes = byteArrayOf(
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
            0x21, 0xFF.toByte(), 0x0B,
            'N'.code.toByte(), 'E'.code.toByte(), 'T'.code.toByte(), 'S'.code.toByte(), 'C'.code.toByte(), 'A'.code.toByte(), 'P'.code.toByte(), 'E'.code.toByte(), '2'.code.toByte(), '.'.code.toByte(), '0'.code.toByte(),
            0x03, 0x01, 0x02, 0x00, 0x00,
        )

        assertEquals(2, gifRepeatCount(bytes))
    }
}
