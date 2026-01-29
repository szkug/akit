@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.szkug.akit.graph.renderscript

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import platform.Accelerate.kvImageEdgeExtend
import platform.Accelerate.kvImageGetTempBufferSize
import platform.Accelerate.vImageBoxConvolve_ARGB8888
import platform.Accelerate.vImageBoxConvolve_Planar8
import platform.Accelerate.vImage_Buffer

// This string is used for error messages.
private const val externalName = "RenderScript Toolkit"

actual object Toolkit {
    actual fun blend(
        mode: BlendingMode,
        sourceArray: ByteArray,
        destArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        restriction: Range2d?
    ) {
        require(sourceArray.size >= sizeX * sizeY * 4) {
            "$externalName blend. sourceArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*4 < ${sourceArray.size}."
        }
        require(destArray.size >= sizeX * sizeY * 4) {
            "$externalName blend. sourceArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*4 < ${sourceArray.size}."
        }
        validateRestriction("blend", sizeX, sizeY, restriction)

        val range = restriction ?: Range2d(0, sizeX, 0, sizeY)
        for (y in range.startY until range.endY) {
            var index = (y * sizeX + range.startX) * 4
            val end = (y * sizeX + range.endX) * 4
            while (index < end) {
                val sR = sourceArray[index].toInt() and 0xFF
                val sG = sourceArray[index + 1].toInt() and 0xFF
                val sB = sourceArray[index + 2].toInt() and 0xFF
                val sA = sourceArray[index + 3].toInt() and 0xFF

                val dR = destArray[index].toInt() and 0xFF
                val dG = destArray[index + 1].toInt() and 0xFF
                val dB = destArray[index + 2].toInt() and 0xFF
                val dA = destArray[index + 3].toInt() and 0xFF

                when (mode) {
                    BlendingMode.CLEAR -> {
                        destArray[index] = 0
                        destArray[index + 1] = 0
                        destArray[index + 2] = 0
                        destArray[index + 3] = 0
                    }
                    BlendingMode.SRC -> {
                        destArray[index] = sR.toByte()
                        destArray[index + 1] = sG.toByte()
                        destArray[index + 2] = sB.toByte()
                        destArray[index + 3] = sA.toByte()
                    }
                    BlendingMode.DST -> Unit
                    BlendingMode.SRC_OVER -> {
                        val invSA = 255 - sA
                        destArray[index] = clampToByte(sR + ((dR * invSA) shr 8))
                        destArray[index + 1] = clampToByte(sG + ((dG * invSA) shr 8))
                        destArray[index + 2] = clampToByte(sB + ((dB * invSA) shr 8))
                        destArray[index + 3] = clampToByte(sA + ((dA * invSA) shr 8))
                    }
                    BlendingMode.DST_OVER -> {
                        val invDA = 255 - dA
                        destArray[index] = clampToByte(dR + ((sR * invDA) shr 8))
                        destArray[index + 1] = clampToByte(dG + ((sG * invDA) shr 8))
                        destArray[index + 2] = clampToByte(dB + ((sB * invDA) shr 8))
                        destArray[index + 3] = clampToByte(dA + ((sA * invDA) shr 8))
                    }
                    BlendingMode.SRC_IN -> {
                        destArray[index] = ((sR * dA) shr 8).toByte()
                        destArray[index + 1] = ((sG * dA) shr 8).toByte()
                        destArray[index + 2] = ((sB * dA) shr 8).toByte()
                        destArray[index + 3] = ((sA * dA) shr 8).toByte()
                    }
                    BlendingMode.DST_IN -> {
                        destArray[index] = ((dR * sA) shr 8).toByte()
                        destArray[index + 1] = ((dG * sA) shr 8).toByte()
                        destArray[index + 2] = ((dB * sA) shr 8).toByte()
                        destArray[index + 3] = ((dA * sA) shr 8).toByte()
                    }
                    BlendingMode.SRC_OUT -> {
                        val invDA = 255 - dA
                        destArray[index] = ((sR * invDA) shr 8).toByte()
                        destArray[index + 1] = ((sG * invDA) shr 8).toByte()
                        destArray[index + 2] = ((sB * invDA) shr 8).toByte()
                        destArray[index + 3] = ((sA * invDA) shr 8).toByte()
                    }
                    BlendingMode.DST_OUT -> {
                        val invSA = 255 - sA
                        destArray[index] = ((dR * invSA) shr 8).toByte()
                        destArray[index + 1] = ((dG * invSA) shr 8).toByte()
                        destArray[index + 2] = ((dB * invSA) shr 8).toByte()
                        destArray[index + 3] = ((dA * invSA) shr 8).toByte()
                    }
                    BlendingMode.SRC_ATOP -> {
                        val invSA = 255 - sA
                        destArray[index] = clampToByte(((sR * dA) + (dR * invSA)) shr 8)
                        destArray[index + 1] = clampToByte(((sG * dA) + (dG * invSA)) shr 8)
                        destArray[index + 2] = clampToByte(((sB * dA) + (dB * invSA)) shr 8)
                        destArray[index + 3] = dA.toByte()
                    }
                    BlendingMode.DST_ATOP -> {
                        val invDA = 255 - dA
                        destArray[index] = clampToByte(((dR * sA) + (sR * invDA)) shr 8)
                        destArray[index + 1] = clampToByte(((dG * sA) + (sG * invDA)) shr 8)
                        destArray[index + 2] = clampToByte(((dB * sA) + (sB * invDA)) shr 8)
                        destArray[index + 3] = sA.toByte()
                    }
                    BlendingMode.XOR -> {
                        destArray[index] = (sR xor dR).toByte()
                        destArray[index + 1] = (sG xor dG).toByte()
                        destArray[index + 2] = (sB xor dB).toByte()
                        destArray[index + 3] = (sA xor dA).toByte()
                    }
                    BlendingMode.MULTIPLY -> {
                        destArray[index] = ((sR * dR) shr 8).toByte()
                        destArray[index + 1] = ((sG * dG) shr 8).toByte()
                        destArray[index + 2] = ((sB * dB) shr 8).toByte()
                        destArray[index + 3] = ((sA * dA) shr 8).toByte()
                    }
                    BlendingMode.ADD -> {
                        destArray[index] = clampToByte(dR + sR)
                        destArray[index + 1] = clampToByte(dG + sG)
                        destArray[index + 2] = clampToByte(dB + sB)
                        destArray[index + 3] = clampToByte(dA + sA)
                    }
                    BlendingMode.SUBTRACT -> {
                        destArray[index] = clampToByte(dR - sR)
                        destArray[index + 1] = clampToByte(dG - sG)
                        destArray[index + 2] = clampToByte(dB - sB)
                        destArray[index + 3] = clampToByte(dA - sA)
                    }
                }

                index += 4
            }
        }
    }

    actual fun blur(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        radius: Int,
        restriction: Range2d?
    ): ByteArray {
        require(vectorSize == 1 || vectorSize == 4) {
            "$externalName blur. The vectorSize should be 1 or 4. $vectorSize provided."
        }
        require(inputArray.size >= sizeX * sizeY * vectorSize) {
            "$externalName blur. inputArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*$vectorSize < ${inputArray.size}."
        }
        require(radius in 1..25) {
            "$externalName blur. The radius should be between 1 and 25. $radius provided."
        }
        validateRestriction("blur", sizeX, sizeY, restriction)

        val kernelSize = (radius * 2 + 1).coerceAtLeast(1)
        val outputArray = ByteArray(inputArray.size)
        val success = boxConvolve(
            inputArray = inputArray,
            outputArray = outputArray,
            sizeX = sizeX,
            sizeY = sizeY,
            vectorSize = vectorSize,
            kernelSize = kernelSize
        )
        if (!success) {
            boxBlurFallback(inputArray, outputArray, sizeX, sizeY, vectorSize, radius)
        }
        if (restriction != null) {
            zeroOutsideRestriction(outputArray, sizeX, sizeY, vectorSize, restriction)
        }
        return outputArray
    }

    actual fun colorMatrix(
        inputArray: ByteArray,
        inputVectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        outputVectorSize: Int,
        matrix: FloatArray,
        addVector: FloatArray?,
        restriction: Range2d?
    ): ByteArray {
        require(inputVectorSize in 1..4) {
            "$externalName colorMatrix. The inputVectorSize should be between 1 and 4. " +
                "$inputVectorSize provided."
        }
        require(outputVectorSize in 1..4) {
            "$externalName colorMatrix. The outputVectorSize should be between 1 and 4. " +
                "$outputVectorSize provided."
        }
        require(inputArray.size >= sizeX * sizeY * inputVectorSize) {
            "$externalName colorMatrix. inputArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*$inputVectorSize < ${inputArray.size}."
        }
        require(matrix.size == 16) {
            "$externalName colorMatrix. matrix should have 16 entries. ${matrix.size} provided."
        }
        require(addVector == null || addVector.size == 4) {
            "$externalName colorMatrix. addVector should be null or have 4 entries. " +
                "${addVector?.size} provided."
        }
        validateRestriction("colorMatrix", sizeX, sizeY, restriction)

        val outputStride = paddedSize(outputVectorSize)
        val inputStride = paddedSize(inputVectorSize)
        val outputArray = ByteArray(sizeX * sizeY * outputStride)
        val add = addVector ?: floatArrayOf(0f, 0f, 0f, 0f)
        val addScaled = floatArrayOf(add[0] * 255f, add[1] * 255f, add[2] * 255f, add[3] * 255f)

        val range = restriction ?: Range2d(0, sizeX, 0, sizeY)
        for (y in range.startY until range.endY) {
            for (x in range.startX until range.endX) {
                val inIndex = (y * sizeX + x) * inputStride
                val r = inputArray[inIndex].toInt() and 0xFF
                val g = if (inputVectorSize >= 2) inputArray[inIndex + 1].toInt() and 0xFF else 0
                val b = if (inputVectorSize >= 3) inputArray[inIndex + 2].toInt() and 0xFF else 0
                val a = if (inputVectorSize == 4) inputArray[inIndex + 3].toInt() and 0xFF else 0

                val rf = r.toFloat()
                val gf = g.toFloat()
                val bf = b.toFloat()
                val af = a.toFloat()

                val sum0 = rf * matrix[0] + gf * matrix[4] + bf * matrix[8] + af * matrix[12] + addScaled[0]
                val sum1 = rf * matrix[1] + gf * matrix[5] + bf * matrix[9] + af * matrix[13] + addScaled[1]
                val sum2 = rf * matrix[2] + gf * matrix[6] + bf * matrix[10] + af * matrix[14] + addScaled[2]
                val sum3 = rf * matrix[3] + gf * matrix[7] + bf * matrix[11] + af * matrix[15] + addScaled[3]

                val outIndex = (y * sizeX + x) * outputStride
                outputArray[outIndex] = clampToByte(sum0)
                if (outputVectorSize >= 2) {
                    outputArray[outIndex + 1] = clampToByte(sum1)
                }
                if (outputVectorSize >= 3) {
                    outputArray[outIndex + 2] = clampToByte(sum2)
                }
                if (outputVectorSize >= 4 || outputVectorSize == 3) {
                    outputArray[outIndex + 3] = clampToByte(sum3)
                }
            }
        }

        return outputArray
    }

    actual fun convolve(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        coefficients: FloatArray,
        restriction: Range2d?
    ): ByteArray {
        require(vectorSize in 1..4) {
            "$externalName convolve. The vectorSize should be between 1 and 4. " +
                "$vectorSize provided."
        }
        require(inputArray.size >= sizeX * sizeY * vectorSize) {
            "$externalName convolve. inputArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*$vectorSize < ${inputArray.size}."
        }
        require(coefficients.size == 9 || coefficients.size == 25) {
            "$externalName convolve. Only 3x3 or 5x5 convolutions are supported. " +
                "${coefficients.size} coefficients provided."
        }
        validateRestriction("convolve", sizeX, sizeY, restriction)

        val outputArray = ByteArray(inputArray.size)
        val kernelSize = if (coefficients.size == 9) 3 else 5
        val radius = kernelSize / 2
        val stride = paddedSize(vectorSize)
        val channelCount = if (vectorSize == 3) 4 else vectorSize
        val range = restriction ?: Range2d(0, sizeX, 0, sizeY)

        for (y in range.startY until range.endY) {
            for (x in range.startX until range.endX) {
                val outIndex = (y * sizeX + x) * stride
                for (c in 0 until channelCount) {
                    var sum = 0f
                    var ky = -radius
                    var coeffIndex = 0
                    while (ky <= radius) {
                        val yy = (y + ky).coerceIn(0, sizeY - 1)
                        var kx = -radius
                        while (kx <= radius) {
                            val xx = (x + kx).coerceIn(0, sizeX - 1)
                            val inIndex = (yy * sizeX + xx) * stride + c
                            val value = inputArray[inIndex].toInt() and 0xFF
                            sum += value * coefficients[coeffIndex]
                            coeffIndex++
                            kx++
                        }
                        ky++
                    }
                    outputArray[outIndex + c] = clampToByte(sum + 0.5f)
                }
            }
        }

        return outputArray
    }

    actual fun histogram(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        restriction: Range2d?
    ): IntArray {
        require(vectorSize in 1..4) {
            "$externalName histogram. The vectorSize should be between 1 and 4. " +
                "$vectorSize provided."
        }
        require(inputArray.size >= sizeX * sizeY * vectorSize) {
            "$externalName histogram. inputArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*$vectorSize < ${inputArray.size}."
        }
        validateRestriction("histogram", sizeX, sizeY, restriction)

        val outputArray = IntArray(256 * paddedSize(vectorSize))
        val stride = paddedSize(vectorSize)
        val range = restriction ?: Range2d(0, sizeX, 0, sizeY)

        for (y in range.startY until range.endY) {
            var index = (y * sizeX + range.startX) * stride
            val end = (y * sizeX + range.endX) * stride
            when (vectorSize) {
                4 -> {
                    while (index < end) {
                        outputArray[(inputArray[index].toInt() and 0xFF) shl 2]++
                        outputArray[((inputArray[index + 1].toInt() and 0xFF) shl 2) + 1]++
                        outputArray[((inputArray[index + 2].toInt() and 0xFF) shl 2) + 2]++
                        outputArray[((inputArray[index + 3].toInt() and 0xFF) shl 2) + 3]++
                        index += 4
                    }
                }
                3 -> {
                    while (index < end) {
                        outputArray[(inputArray[index].toInt() and 0xFF) shl 2]++
                        outputArray[((inputArray[index + 1].toInt() and 0xFF) shl 2) + 1]++
                        outputArray[((inputArray[index + 2].toInt() and 0xFF) shl 2) + 2]++
                        index += 4
                    }
                }
                2 -> {
                    while (index < end) {
                        outputArray[(inputArray[index].toInt() and 0xFF) shl 1]++
                        outputArray[((inputArray[index + 1].toInt() and 0xFF) shl 1) + 1]++
                        index += 2
                    }
                }
                1 -> {
                    while (index < end) {
                        outputArray[inputArray[index].toInt() and 0xFF]++
                        index += 1
                    }
                }
            }
        }

        return outputArray
    }

    actual fun histogramDot(
        inputArray: ByteArray,
        vectorSize: Int,
        sizeX: Int,
        sizeY: Int,
        coefficients: FloatArray?,
        restriction: Range2d?
    ): IntArray {
        require(vectorSize in 1..4) {
            "$externalName histogramDot. The vectorSize should be between 1 and 4. " +
                "$vectorSize provided."
        }
        require(inputArray.size >= sizeX * sizeY * vectorSize) {
            "$externalName histogramDot. inputArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*$vectorSize < ${inputArray.size}."
        }
        validateHistogramDotCoefficients(coefficients, vectorSize)
        validateRestriction("histogramDot", sizeX, sizeY, restriction)

        val outputArray = IntArray(256)
        val actualCoefficients = coefficients ?: floatArrayOf(0.299f, 0.587f, 0.114f, 0f)
        val dotI = IntArray(4)
        dotI[0] = (actualCoefficients[0] * 256f + 0.5f).toInt()
        dotI[1] = (actualCoefficients.getOrElse(1) { 0f } * 256f + 0.5f).toInt()
        dotI[2] = (actualCoefficients.getOrElse(2) { 0f } * 256f + 0.5f).toInt()
        dotI[3] = (actualCoefficients.getOrElse(3) { 0f } * 256f + 0.5f).toInt()

        val stride = paddedSize(vectorSize)
        val range = restriction ?: Range2d(0, sizeX, 0, sizeY)
        for (y in range.startY until range.endY) {
            var index = (y * sizeX + range.startX) * stride
            val end = (y * sizeX + range.endX) * stride
            when (vectorSize) {
                4 -> {
                    while (index < end) {
                        val t = (dotI[0] * (inputArray[index].toInt() and 0xFF)) +
                            (dotI[1] * (inputArray[index + 1].toInt() and 0xFF)) +
                            (dotI[2] * (inputArray[index + 2].toInt() and 0xFF)) +
                            (dotI[3] * (inputArray[index + 3].toInt() and 0xFF))
                        outputArray[(t + 0x7f) shr 8]++
                        index += 4
                    }
                }
                3 -> {
                    while (index < end) {
                        val t = (dotI[0] * (inputArray[index].toInt() and 0xFF)) +
                            (dotI[1] * (inputArray[index + 1].toInt() and 0xFF)) +
                            (dotI[2] * (inputArray[index + 2].toInt() and 0xFF))
                        outputArray[(t + 0x7f) shr 8]++
                        index += 4
                    }
                }
                2 -> {
                    while (index < end) {
                        val t = (dotI[0] * (inputArray[index].toInt() and 0xFF)) +
                            (dotI[1] * (inputArray[index + 1].toInt() and 0xFF))
                        outputArray[(t + 0x7f) shr 8]++
                        index += 2
                    }
                }
                1 -> {
                    while (index < end) {
                        val t = dotI[0] * (inputArray[index].toInt() and 0xFF)
                        outputArray[(t + 0x7f) shr 8]++
                        index += 1
                    }
                }
            }
        }

        return outputArray
    }

    actual fun lut(
        inputArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        table: LookupTable,
        restriction: Range2d?
    ): ByteArray {
        require(inputArray.size >= sizeX * sizeY * 4) {
            "$externalName lut. inputArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*4 < ${inputArray.size}."
        }
        validateRestriction("lut", sizeX, sizeY, restriction)

        val outputArray = ByteArray(inputArray.size)
        val range = restriction ?: Range2d(0, sizeX, 0, sizeY)
        for (y in range.startY until range.endY) {
            var index = (y * sizeX + range.startX) * 4
            val end = (y * sizeX + range.endX) * 4
            while (index < end) {
                val r = inputArray[index].toInt() and 0xFF
                val g = inputArray[index + 1].toInt() and 0xFF
                val b = inputArray[index + 2].toInt() and 0xFF
                val a = inputArray[index + 3].toInt() and 0xFF
                outputArray[index] = table.red[r]
                outputArray[index + 1] = table.green[g]
                outputArray[index + 2] = table.blue[b]
                outputArray[index + 3] = table.alpha[a]
                index += 4
            }
        }

        return outputArray
    }

    actual fun lut3d(
        inputArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        cube: Rgba3dArray,
        restriction: Range2d?
    ): ByteArray {
        require(inputArray.size >= sizeX * sizeY * 4) {
            "$externalName lut3d. inputArray is too small for the given dimensions. " +
                "$sizeX*$sizeY*4 < ${inputArray.size}."
        }
        require(
            cube.sizeX >= 2 && cube.sizeY >= 2 && cube.sizeZ >= 2 &&
                cube.sizeX <= 256 && cube.sizeY <= 256 && cube.sizeZ <= 256
        ) {
            "$externalName lut3d. The dimensions of the cube should be between 2 and 256. " +
                "(${cube.sizeX}, ${cube.sizeY}, ${cube.sizeZ}) provided."
        }
        validateRestriction("lut3d", sizeX, sizeY, restriction)

        val outputArray = ByteArray(inputArray.size)
        val dimsX = cube.sizeX - 1
        val dimsY = cube.sizeY - 1
        val dimsZ = cube.sizeZ - 1
        val coordMulX = ((dimsX.toFloat() * 0x8000f) / 255f).toInt()
        val coordMulY = ((dimsY.toFloat() * 0x8000f) / 255f).toInt()
        val coordMulZ = ((dimsZ.toFloat() * 0x8000f) / 255f).toInt()
        val strideY = cube.sizeX * 4
        val strideZ = strideY * cube.sizeY
        val cubeValues = cube.values

        val range = restriction ?: Range2d(0, sizeX, 0, sizeY)
        for (y in range.startY until range.endY) {
            var index = (y * sizeX + range.startX) * 4
            val end = (y * sizeX + range.endX) * 4
            while (index < end) {
                val r = inputArray[index].toInt() and 0xFF
                val g = inputArray[index + 1].toInt() and 0xFF
                val b = inputArray[index + 2].toInt() and 0xFF
                val a = inputArray[index + 3]

                val baseX = r * coordMulX
                val baseY = g * coordMulY
                val baseZ = b * coordMulZ

                val coordX = baseX shr 15
                val coordY = baseY shr 15
                val coordZ = baseZ shr 15

                val weight2X = baseX and 0x7fff
                val weight2Y = baseY and 0x7fff
                val weight2Z = baseZ and 0x7fff
                val weight1X = 0x8000 - weight2X
                val weight1Y = 0x8000 - weight2Y
                val weight1Z = 0x8000 - weight2Z

                val baseOffset = ((coordZ * cube.sizeY + coordY) * cube.sizeX + coordX) * 4
                val offset00 = baseOffset
                val offset10 = baseOffset + 4
                val offset01 = baseOffset + strideZ
                val offset11 = baseOffset + strideZ + strideY

                for (c in 0 until 3) {
                    val v000 = cubeValues[offset00 + c].toInt() and 0xFF
                    val v100 = cubeValues[offset10 + c].toInt() and 0xFF
                    val v010 = cubeValues[offset00 + strideY + c].toInt() and 0xFF
                    val v110 = cubeValues[offset10 + strideY + c].toInt() and 0xFF
                    val v001 = cubeValues[offset01 + c].toInt() and 0xFF
                    val v101 = cubeValues[offset01 + 4 + c].toInt() and 0xFF
                    val v011 = cubeValues[offset11 + c].toInt() and 0xFF
                    val v111 = cubeValues[offset11 + 4 + c].toInt() and 0xFF

                    val yz00 = ((v000 * weight1X) + (v100 * weight2X)) shr 7
                    val yz10 = ((v010 * weight1X) + (v110 * weight2X)) shr 7
                    val yz01 = ((v001 * weight1X) + (v101 * weight2X)) shr 7
                    val yz11 = ((v011 * weight1X) + (v111 * weight2X)) shr 7

                    val z0 = ((yz00 * weight1Y) + (yz10 * weight2Y)) shr 15
                    val z1 = ((yz01 * weight1Y) + (yz11 * weight2Y)) shr 15
                    val v = ((z0 * weight1Z) + (z1 * weight2Z)) shr 15
                    val v2 = (v + 0x7f) shr 8
                    outputArray[index + c] = v2.toByte()
                }
                outputArray[index + 3] = a
                index += 4
            }
        }

        return outputArray
    }

    actual fun resize(
        inputArray: ByteArray,
        vectorSize: Int,
        inputSizeX: Int,
        inputSizeY: Int,
        outputSizeX: Int,
        outputSizeY: Int,
        restriction: Range2d?
    ): ByteArray {
        require(vectorSize in 1..4) {
            "$externalName resize. The vectorSize should be between 1 and 4. $vectorSize provided."
        }
        require(inputArray.size >= inputSizeX * inputSizeY * vectorSize) {
            "$externalName resize. inputArray is too small for the given dimensions. " +
                "$inputSizeX*$inputSizeY*$vectorSize < ${inputArray.size}."
        }
        validateRestriction("resize", outputSizeX, outputSizeY, restriction)

        val stride = paddedSize(vectorSize)
        val channelCount = if (vectorSize == 3) 4 else vectorSize
        val outputArray = ByteArray(outputSizeX * outputSizeY * stride)

        val scaleX = inputSizeX.toFloat() / outputSizeX.toFloat()
        val scaleY = inputSizeY.toFloat() / outputSizeY.toFloat()
        val range = restriction ?: Range2d(0, outputSizeX, 0, outputSizeY)

        for (y in range.startY until range.endY) {
            val yf = (y + 0.5f) * scaleY - 0.5f
            val startY = floor(yf - 1f).toInt()
            val yfFrac = yf - floor(yf)
            val maxY = inputSizeY - 1
            val ys0 = max(0, startY)
            val ys1 = max(0, startY + 1)
            val ys2 = min(maxY, startY + 2)
            val ys3 = min(maxY, startY + 3)

            for (x in range.startX until range.endX) {
                val xf = (x + 0.5f) * scaleX - 0.5f
                val startX = floor(xf - 1f).toInt()
                val xfFrac = xf - floor(xf)
                val maxX = inputSizeX - 1
                val xs0 = max(0, startX)
                val xs1 = max(0, startX + 1)
                val xs2 = min(maxX, startX + 2)
                val xs3 = min(maxX, startX + 3)

                val outIndex = (y * outputSizeX + x) * stride
                for (c in 0 until channelCount) {
                    val p0 = cubicInterpolate(
                        inputArray.valueAt(ys0, xs0, c, inputSizeX, stride),
                        inputArray.valueAt(ys0, xs1, c, inputSizeX, stride),
                        inputArray.valueAt(ys0, xs2, c, inputSizeX, stride),
                        inputArray.valueAt(ys0, xs3, c, inputSizeX, stride),
                        xfFrac
                    )
                    val p1 = cubicInterpolate(
                        inputArray.valueAt(ys1, xs0, c, inputSizeX, stride),
                        inputArray.valueAt(ys1, xs1, c, inputSizeX, stride),
                        inputArray.valueAt(ys1, xs2, c, inputSizeX, stride),
                        inputArray.valueAt(ys1, xs3, c, inputSizeX, stride),
                        xfFrac
                    )
                    val p2 = cubicInterpolate(
                        inputArray.valueAt(ys2, xs0, c, inputSizeX, stride),
                        inputArray.valueAt(ys2, xs1, c, inputSizeX, stride),
                        inputArray.valueAt(ys2, xs2, c, inputSizeX, stride),
                        inputArray.valueAt(ys2, xs3, c, inputSizeX, stride),
                        xfFrac
                    )
                    val p3 = cubicInterpolate(
                        inputArray.valueAt(ys3, xs0, c, inputSizeX, stride),
                        inputArray.valueAt(ys3, xs1, c, inputSizeX, stride),
                        inputArray.valueAt(ys3, xs2, c, inputSizeX, stride),
                        inputArray.valueAt(ys3, xs3, c, inputSizeX, stride),
                        xfFrac
                    )
                    val value = cubicInterpolate(p0, p1, p2, p3, yfFrac)
                    outputArray[outIndex + c] = clampToByte(value + 0.5f)
                }
            }
        }

        return outputArray
    }

    actual fun yuvToRgb(
        inputArray: ByteArray,
        sizeX: Int,
        sizeY: Int,
        format: YuvFormat
    ): ByteArray {
        require(sizeX % 2 == 0 && sizeY % 2 == 0) {
            "$externalName yuvToRgb. Non-even dimensions are not supported. " +
                "$sizeX and $sizeY were provided."
        }

        val outputArray = ByteArray(sizeX * sizeY * 4)
        when (format) {
            YuvFormat.NV21 -> {
                val strideY = sizeX
                val strideU = strideY
                val strideV = strideY
                val offsetY = 0
                val offsetV = strideY * sizeY
                val offsetU = offsetV + 1
                for (y in 0 until sizeY) {
                    val rowY = offsetY + y * strideY
                    val rowU = offsetU + (y / 2) * strideU
                    val rowV = offsetV + (y / 2) * strideV
                    for (x in 0 until sizeX) {
                        val cx = (x / 2) * 2
                        val yValue = inputArray[rowY + x].toInt() and 0xFF
                        val uValue = inputArray[rowU + cx].toInt() and 0xFF
                        val vValue = inputArray[rowV + cx].toInt() and 0xFF
                        writeYuvPixel(outputArray, (y * sizeX + x) * 4, yValue, uValue, vValue)
                    }
                }
            }
            YuvFormat.YV12 -> {
                val strideY = roundUpTo16(sizeX)
                val strideU = roundUpTo16(strideY / 2)
                val strideV = strideU
                val offsetY = 0
                val offsetU = strideY * sizeY
                val offsetV = offsetU + strideV * sizeY / 2
                for (y in 0 until sizeY) {
                    val rowY = offsetY + y * strideY
                    val rowU = offsetU + (y / 2) * strideU
                    val rowV = offsetV + (y / 2) * strideV
                    for (x in 0 until sizeX) {
                        val cx = x / 2
                        val yValue = inputArray[rowY + x].toInt() and 0xFF
                        val uValue = inputArray[rowU + cx].toInt() and 0xFF
                        val vValue = inputArray[rowV + cx].toInt() and 0xFF
                        writeYuvPixel(outputArray, (y * sizeX + x) * 4, yValue, uValue, vValue)
                    }
                }
            }
        }
        return outputArray
    }

    actual fun shutdown() {
        // no-op for iOS
    }
}

private fun unsupported(function: String): Nothing {
    error("$externalName $function is not supported on iOS.")
}

private fun boxConvolve(
    inputArray: ByteArray,
    outputArray: ByteArray,
    sizeX: Int,
    sizeY: Int,
    vectorSize: Int,
    kernelSize: Int
): Boolean {
    val rowBytes = sizeX * vectorSize
    var error = -1L
    memScoped {
        val src = alloc<vImage_Buffer>()
        val dest = alloc<vImage_Buffer>()
        inputArray.usePinned { inputPinned ->
            outputArray.usePinned { outputPinned ->
                src.data = inputPinned.addressOf(0)
                src.height = sizeY.toULong()
                src.width = sizeX.toULong()
                src.rowBytes = rowBytes.toULong()

                dest.data = outputPinned.addressOf(0)
                dest.height = sizeY.toULong()
                dest.width = sizeX.toULong()
                dest.rowBytes = rowBytes.toULong()

                val kernel = kernelSize.toUInt()
                val flags = kvImageEdgeExtend
                val tempBufferSize = requiredTempBufferSize(src.ptr, dest.ptr, kernel, vectorSize)
                if (tempBufferSize < 0) {
                    println(
                        "AkitToolkit blur vImage tempBuffer error=$tempBufferSize " +
                            "sizeX=$sizeX sizeY=$sizeY vectorSize=$vectorSize " +
                            "kernelSize=$kernelSize rowBytes=$rowBytes"
                    )
                    error = tempBufferSize.toLong()
                    return@usePinned
                }
                if (tempBufferSize == 0) {
                    error = if (vectorSize == 4) {
                        vImageBoxConvolve_ARGB8888(
                            src = src.ptr,
                            dest = dest.ptr,
                            tempBuffer = null,
                            srcOffsetToROI_X = 0u,
                            srcOffsetToROI_Y = 0u,
                            kernel_height = kernel,
                            kernel_width = kernel,
                            backgroundColor = null,
                            flags = flags
                        )
                    } else {
                        vImageBoxConvolve_Planar8(
                            src = src.ptr,
                            dest = dest.ptr,
                            tempBuffer = null,
                            srcOffsetToROI_X = 0u,
                            srcOffsetToROI_Y = 0u,
                            kernel_height = kernel,
                            kernel_width = kernel,
                            backgroundColor = 0u.toUByte(),
                            flags = flags
                        )
                    }
                } else {
                    val tempBuffer = ByteArray(tempBufferSize)
                    tempBuffer.usePinned { tempPinned ->
                        error = if (vectorSize == 4) {
                            vImageBoxConvolve_ARGB8888(
                                src = src.ptr,
                                dest = dest.ptr,
                                tempBuffer = tempPinned.addressOf(0),
                                srcOffsetToROI_X = 0u,
                                srcOffsetToROI_Y = 0u,
                                kernel_height = kernel,
                                kernel_width = kernel,
                                backgroundColor = null,
                                flags = flags
                            )
                        } else {
                            vImageBoxConvolve_Planar8(
                                src = src.ptr,
                                dest = dest.ptr,
                                tempBuffer = tempPinned.addressOf(0),
                                srcOffsetToROI_X = 0u,
                                srcOffsetToROI_Y = 0u,
                                kernel_height = kernel,
                                kernel_width = kernel,
                                backgroundColor = 0u.toUByte(),
                                flags = flags
                            )
                        }
                    }
                }
            }
        }
    }
    if (error != 0L) {
        println(
            "AkitToolkit blur vImage error=$error sizeX=$sizeX sizeY=$sizeY vectorSize=$vectorSize " +
                "kernelSize=$kernelSize rowBytes=$rowBytes"
        )
    }
    return error == 0L
}

private fun validateHistogramDotCoefficients(coefficients: FloatArray?, vectorSize: Int) {
    require(coefficients == null || coefficients.size == vectorSize) {
        "$externalName histogramDot. The coefficients should be null or have $vectorSize values."
    }
    if (coefficients != null) {
        var sum = 0f
        for (i in 0 until vectorSize) {
            require(coefficients[i] >= 0.0f) {
                "$externalName histogramDot. Coefficients should not be negative. " +
                    "Coefficient $i was ${coefficients[i]}."
            }
            sum += coefficients[i]
        }
        require(sum <= 1.0f) {
            "$externalName histogramDot. Coefficients should add to 1 or less. Their sum is $sum."
        }
    }
}

private fun validateRestriction(tag: String, sizeX: Int, sizeY: Int, restriction: Range2d?) {
    if (restriction == null) return
    require(restriction.startX < sizeX && restriction.endX <= sizeX) {
        "$externalName $tag. sizeX should be greater than restriction.startX and greater " +
            "or equal to restriction.endX. $sizeX, ${restriction.startX}, " +
            "and ${restriction.endX} were provided respectively."
    }
    require(restriction.startY < sizeY && restriction.endY <= sizeY) {
        "$externalName $tag. sizeY should be greater than restriction.startY and greater " +
            "or equal to restriction.endY. $sizeY, ${restriction.startY}, " +
            "and ${restriction.endY} were provided respectively."
    }
    require(restriction.startX < restriction.endX) {
        "$externalName $tag. Restriction startX should be less than endX. " +
            "${restriction.startX} and ${restriction.endX} were provided respectively."
    }
    require(restriction.startY < restriction.endY) {
        "$externalName $tag. Restriction startY should be less than endY. " +
            "${restriction.startY} and ${restriction.endY} were provided respectively."
    }
}

private fun paddedSize(vectorSize: Int) = if (vectorSize == 3) 4 else vectorSize

private fun clampToByte(value: Float): Byte {
    val clamped = when {
        value < 0f -> 0f
        value > 255.5f -> 255.5f
        else -> value
    }
    return clamped.toInt().toByte()
}

private fun clampToByte(value: Int): Byte {
    return when {
        value < 0 -> 0
        value > 255 -> 255
        else -> value
    }.toByte()
}

private fun zeroOutsideRestriction(
    array: ByteArray,
    sizeX: Int,
    sizeY: Int,
    vectorSize: Int,
    restriction: Range2d
) {
    val stride = vectorSize * sizeX
    for (y in 0 until sizeY) {
        val rowStart = y * stride
        val rowEnd = rowStart + stride
        val minX = restriction.startX
        val maxX = restriction.endX
        val startFill = if (y in restriction.startY until restriction.endY) {
            rowStart + minX * vectorSize
        } else {
            rowStart
        }
        val endFill = if (y in restriction.startY until restriction.endY) {
            rowStart + maxX * vectorSize
        } else {
            rowEnd
        }
        for (i in rowStart until startFill) {
            array[i] = 0
        }
        for (i in endFill until rowEnd) {
            array[i] = 0
        }
        if (y !in restriction.startY until restriction.endY) {
            for (i in rowStart until rowEnd) {
                array[i] = 0
            }
        }
    }
}

private fun cubicInterpolate(p0: Float, p1: Float, p2: Float, p3: Float, x: Float): Float {
    return p1 + 0.5f * x * (p2 - p0 + x * (2f * p0 - 5f * p1 + 4f * p2 - p3 +
        x * (3f * (p1 - p2) + p3 - p0)))
}

private fun ByteArray.valueAt(y: Int, x: Int, c: Int, width: Int, stride: Int): Float {
    val index = (y * width + x) * stride + c
    return (this[index].toInt() and 0xFF).toFloat()
}

private fun roundUpTo16(value: Int): Int = (value + 15) and 15.inv()

private fun writeYuvPixel(outputArray: ByteArray, outIndex: Int, y: Int, u: Int, v: Int) {
    val yValue = y - 16
    val uValue = u - 128
    val vValue = v - 128

    var r = (yValue * 298 + vValue * 409 + 128) shr 8
    var g = (yValue * 298 - uValue * 100 - vValue * 208 + 128) shr 8
    var b = (yValue * 298 + uValue * 516 + 128) shr 8

    if (r < 0) r = 0
    if (r > 255) r = 255
    if (g < 0) g = 0
    if (g > 255) g = 255
    if (b < 0) b = 0
    if (b > 255) b = 255

    outputArray[outIndex] = r.toByte()
    outputArray[outIndex + 1] = g.toByte()
    outputArray[outIndex + 2] = b.toByte()
    outputArray[outIndex + 3] = 0xFF.toByte()
}

private fun requiredTempBufferSize(
    src: CPointer<vImage_Buffer>,
    dest: CPointer<vImage_Buffer>,
    kernel: UInt,
    vectorSize: Int
): Int {
    val flags = kvImageGetTempBufferSize or kvImageEdgeExtend
    val size = if (vectorSize == 4) {
        vImageBoxConvolve_ARGB8888(
            src = src,
            dest = dest,
            tempBuffer = null,
            srcOffsetToROI_X = 0u,
            srcOffsetToROI_Y = 0u,
            kernel_height = kernel,
            kernel_width = kernel,
            backgroundColor = null,
            flags = flags
        )
    } else {
        vImageBoxConvolve_Planar8(
            src = src,
            dest = dest,
            tempBuffer = null,
            srcOffsetToROI_X = 0u,
            srcOffsetToROI_Y = 0u,
            kernel_height = kernel,
            kernel_width = kernel,
            backgroundColor = 0u.toUByte(),
            flags = flags
        )
    }
    return size.toInt()
}

private fun boxBlurFallback(
    inputArray: ByteArray,
    outputArray: ByteArray,
    sizeX: Int,
    sizeY: Int,
    vectorSize: Int,
    radius: Int
) {
    val channels = vectorSize
    val window = radius * 2 + 1
    val stride = sizeX * channels
    val temp = ByteArray(inputArray.size)

    for (y in 0 until sizeY) {
        val rowOffset = y * stride
        for (c in 0 until channels) {
            var sum = 0
            for (i in -radius..radius) {
                val x = i.coerceIn(0, sizeX - 1)
                sum += inputArray[rowOffset + x * channels + c].toInt() and 0xFF
            }
            var index = rowOffset + c
            for (x in 0 until sizeX) {
                temp[index] = (sum / window).toByte()
                val outX = (x - radius).coerceIn(0, sizeX - 1)
                val inX = (x + radius + 1).coerceIn(0, sizeX - 1)
                sum += (inputArray[rowOffset + inX * channels + c].toInt() and 0xFF) -
                    (inputArray[rowOffset + outX * channels + c].toInt() and 0xFF)
                index += channels
            }
        }
    }

    for (x in 0 until sizeX) {
        val colOffset = x * channels
        for (c in 0 until channels) {
            var sum = 0
            for (i in -radius..radius) {
                val y = i.coerceIn(0, sizeY - 1)
                sum += temp[y * stride + colOffset + c].toInt() and 0xFF
            }
            var index = colOffset + c
            for (y in 0 until sizeY) {
                outputArray[index] = (sum / window).toByte()
                val outY = (y - radius).coerceIn(0, sizeY - 1)
                val inY = (y + radius + 1).coerceIn(0, sizeY - 1)
                sum += (temp[inY * stride + colOffset + c].toInt() and 0xFF) -
                    (temp[outY * stride + colOffset + c].toInt() and 0xFF)
                index += stride
            }
        }
    }
}
