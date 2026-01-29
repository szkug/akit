package cn.szkug.akit.graph.renderscript

expect object Toolkit {
    fun blend(
        mode: BlendingMode,
        sourceArray: ByteArray,
        destArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        restriction: Range2d? = null
    )

    fun blur(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        radius: Int = 5,
        restriction: Range2d? = null
    ): ByteArray

    fun colorMatrix(
        inputArray: ByteArray,
        inputVectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        outputVectorSize: Int = inputVectorSize,
        matrix: FloatArray,
        addVector: FloatArray? = null,
        restriction: Range2d? = null
    ): ByteArray

    fun convolve(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        coefficients: FloatArray,
        restriction: Range2d? = null
    ): ByteArray

    fun histogram(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        restriction: Range2d? = null
    ): IntArray

    fun histogramDot(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        coefficients: FloatArray? = null,
        restriction: Range2d? = null
    ): IntArray

    fun lut(
        inputArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        table: LookupTable,
        restriction: Range2d? = null
    ): ByteArray

    fun lut3d(
        inputArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        cube: Rgba3dArray,
        restriction: Range2d? = null
    ): ByteArray

    fun resize(
        inputArray: ByteArray,
        vectorSize: Int,
        inputSizeX: Int,
        inputSizeY: Int,
        outputSizeX: Int,
        outputSizeY: Int,
        restriction: Range2d? = null
    ): ByteArray

    fun yuvToRgb(
        inputArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        format: YuvFormat
    ): ByteArray

    fun shutdown()
}

enum class BlendingMode(val value: Int) {
    CLEAR(0),
    SRC(1),
    DST(2),
    SRC_OVER(3),
    DST_OVER(4),
    SRC_IN(5),
    DST_IN(6),
    SRC_OUT(7),
    DST_OUT(8),
    SRC_ATOP(9),
    DST_ATOP(10),
    XOR(11),
    MULTIPLY(12),
    ADD(13),
    SUBTRACT(14)
}

class LookupTable {
    var red = ByteArray(256) { it.toByte() }
    var green = ByteArray(256) { it.toByte() }
    var blue = ByteArray(256) { it.toByte() }
    var alpha = ByteArray(256) { it.toByte() }
}

enum class YuvFormat(val value: Int) {
    NV21(0x11),
    YV12(0x32315659)
}

data class Range2d(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int
) {
    constructor() : this(0, 0, 0, 0)
}

class Rgba3dArray(val values: ByteArray, val sizeX: Int, val sizeY: Int, val sizeZ: Int) {
    init {
        require(values.size >= sizeX * sizeY * sizeZ * 4)
    }

    operator fun get(x: Int, y: Int, z: Int): ByteArray {
        val index = indexOfVector(x, y, z)
        return ByteArray(4) { values[index + it] }
    }

    operator fun set(x: Int, y: Int, z: Int, value: ByteArray) {
        require(value.size == 4)
        val index = indexOfVector(x, y, z)
        for (i in 0..3) {
            values[index + i] = value[i]
        }
    }

    private fun indexOfVector(x: Int, y: Int, z: Int): Int {
        require(x in 0 until sizeX)
        require(y in 0 until sizeY)
        require(z in 0 until sizeZ)
        return ((z * sizeY + y) * sizeX + x) * 4
    }
}
