package munchkin.image.coil.support

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PngSupportTest {
    @Test
    fun detectsPngHeaderFromBytes() {
        val header = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
        )

        assertTrue(isPng(mimeType = null, bytes = header))
    }

    @Test
    fun rejectsNonPngSource() {
        val source = Buffer().writeUtf8("not-png")

        assertFalse(isPngSource(mimeType = null, source = source))
    }
}
