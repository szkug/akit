package munchkin.resources.loader.coil.support

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoSourceDetectorTest {
    @Test
    fun detectsMp4HeaderWhenMimeTypeMissing() {
        val source = Buffer().write(byteArrayOf(0, 0, 0, 24, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), 0, 0, 0, 0))

        assertTrue(isLikelyVideoSource(mimeType = null, source = source))
    }

    @Test
    fun rejectsNonVideoHeader() {
        val source = Buffer().writeUtf8("plain-text")

        assertFalse(isLikelyVideoSource(mimeType = null, source = source))
    }
}
