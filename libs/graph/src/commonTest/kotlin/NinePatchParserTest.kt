package munchkin.graph.ninepatch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NinePatchParserTest {
    @Test
    fun parseNinePatchBuildsChunkFromRawBorderSource() {
        val source = FakeNinePatchPixelSource(
            width = 5,
            height = 5,
            pixels = intArrayOf(
                T, B, T, T, T,
                B, C, C, C, T,
                T, C, C, C, T,
                T, C, C, C, B,
                T, B, B, T, T,
            ),
        )

        val result = parseNinePatch(source, chunkBytes = null)

        assertEquals(NinePatchType.Raw, result.type)
        assertNotNull(result.chunk)
        assertEquals(listOf(NinePatchDiv(0, 1)), result.chunk.xDivs)
        assertEquals(listOf(NinePatchDiv(0, 1)), result.chunk.yDivs)
        assertEquals(NinePatchRect(left = 0, top = 2, right = 1, bottom = 0), result.chunk.padding)
    }

    private class FakeNinePatchPixelSource(
        override val width: Int,
        override val height: Int,
        private val pixels: IntArray,
    ) : NinePatchPixelSource {
        override fun getPixel(x: Int, y: Int): Int = pixels[y * width + x]
    }

    private companion object {
        const val T = 0x00000000
        const val B = 0xFF000000.toInt()
        const val C = 0xFF112233.toInt()
    }
}
