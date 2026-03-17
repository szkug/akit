package munchkin.graph.renderscript

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class Rgba3dArrayTest {
    @Test
    fun setAndGetUseExpectedVectorIndex() {
        val cube = Rgba3dArray(ByteArray(2 * 2 * 2 * 4), sizeX = 2, sizeY = 2, sizeZ = 2)
        val value = byteArrayOf(1, 2, 3, 4)

        cube[1, 0, 1] = value

        assertContentEquals(value, cube[1, 0, 1])
    }

    @Test
    fun constructorRequiresEnoughBytes() {
        assertFailsWith<IllegalArgumentException> {
            Rgba3dArray(ByteArray(7), sizeX = 1, sizeY = 1, sizeZ = 2)
        }
    }
}
