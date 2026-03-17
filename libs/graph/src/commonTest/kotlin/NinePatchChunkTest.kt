package munchkin.graph.ninepatch

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NinePatchChunkTest {
    @Test
    fun serializeRoundTripPreservesChunkFields() {
        val original = NinePatchChunk(
            wasSerialized = true,
            xDivs = mutableListOf(NinePatchDiv(1, 3), NinePatchDiv(5, 8)),
            yDivs = mutableListOf(NinePatchDiv(0, 2)),
            padding = NinePatchRect(left = 2, top = 4, right = 6, bottom = 8),
            colors = intArrayOf(NinePatchChunk.NO_COLOR, NinePatchChunk.TRANSPARENT_COLOR, 0xFFAABBCC.toInt()),
        )

        val parsed = NinePatchChunk.parse(original.toBytes())

        assertEquals(original.wasSerialized, parsed.wasSerialized)
        assertEquals(original.xDivs, parsed.xDivs)
        assertEquals(original.yDivs, parsed.yDivs)
        assertEquals(original.padding, parsed.padding)
        assertContentEquals(original.colors, parsed.colors)
    }

    @Test
    fun parseRejectsOddDivCounts() {
        val bytes = byteArrayOf(
            1, 1, 2, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )

        assertFailsWith<DivLengthException> {
            NinePatchChunk.parse(bytes)
        }
    }
}
