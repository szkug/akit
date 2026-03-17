package munchkin.image

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageSizeTest {
    @Test
    fun clampToUsesMaximumBounds() {
        val size = ImageSize(width = 480, height = 320)

        val clamped = size.clampTo(AsyncImageSizeLimit(maxWidth = 200, maxHeight = 100))

        assertEquals(ImageSize(width = 200, height = 100), clamped)
    }

    @Test
    fun clampToUsesLimitForUnknownDimensions() {
        val size = ImageSize(width = 0, height = -1)

        val clamped = size.clampTo(AsyncImageSizeLimit(maxWidth = 120, maxHeight = 80))

        assertEquals(ImageSize(width = 120, height = 80), clamped)
    }
}
