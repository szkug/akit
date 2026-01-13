package cn.szkug.akit.graph.ninepatch

class NinePatchChunk(
    var wasSerialized: Boolean = true,
    var xDivs: MutableList<NinePatchDiv> = mutableListOf(),
    var yDivs: MutableList<NinePatchDiv> = mutableListOf(),
    var padding: NinePatchRect = NinePatchRect(),
    var colors: IntArray = IntArray(0)
) {

    fun toBytes(): ByteArray {
        val capacity = 4 + (7 * 4) + xDivs.size * 2 * 4 + yDivs.size * 2 * 4 + colors.size * 4
        val writer = ByteArrayWriter(capacity)
        writer.writeByte(if (wasSerialized) 1 else 0)
        writer.writeByte(xDivs.size * 2)
        writer.writeByte(yDivs.size * 2)
        writer.writeByte(colors.size)

        writer.writeInt(0)
        writer.writeInt(0)

        writer.writeInt(padding.left)
        writer.writeInt(padding.right)
        writer.writeInt(padding.top)
        writer.writeInt(padding.bottom)

        writer.writeInt(0)

        xDivs.forEach { div ->
            writer.writeInt(div.start)
            writer.writeInt(div.stop)
        }
        yDivs.forEach { div ->
            writer.writeInt(div.start)
            writer.writeInt(div.stop)
        }
        colors.forEach { color ->
            writer.writeInt(color)
        }

        return writer.toByteArray()
    }

    companion object {
        const val NO_COLOR = 0x00000001
        const val TRANSPARENT_COLOR = 0x00000000
        const val DEFAULT_DENSITY = 160

        fun parse(data: ByteArray): NinePatchChunk {
            val reader = ByteArrayReader(data)
            val chunk = NinePatchChunk()
            chunk.wasSerialized = reader.readByte().toInt() != 0
            if (!chunk.wasSerialized) {
                throw ChunkNotSerializedException()
            }

            val divXCount = reader.readUnsignedByte()
            checkDivCount(divXCount)
            val divYCount = reader.readUnsignedByte()
            checkDivCount(divYCount)

            chunk.colors = IntArray(reader.readUnsignedByte())

            reader.readInt()
            reader.readInt()

            chunk.padding.left = reader.readInt()
            chunk.padding.right = reader.readInt()
            chunk.padding.top = reader.readInt()
            chunk.padding.bottom = reader.readInt()

            reader.readInt()

            val xDivsCount = divXCount shr 1
            chunk.xDivs = ArrayList(xDivsCount)
            readDivs(xDivsCount, reader, chunk.xDivs)

            val yDivsCount = divYCount shr 1
            chunk.yDivs = ArrayList(yDivsCount)
            readDivs(yDivsCount, reader, chunk.yDivs)

            for (i in chunk.colors.indices) {
                chunk.colors[i] = reader.readInt()
            }

            return chunk
        }

        fun createEmptyChunk(): NinePatchChunk = NinePatchChunk()

        fun createColorsArrayAndSet(chunk: NinePatchChunk?, bitmapWidth: Int, bitmapHeight: Int) {
            val colorsArray = createColorsArray(chunk, bitmapWidth, bitmapHeight)
            if (chunk != null) {
                chunk.colors = colorsArray
            }
        }

        fun createColorsArray(chunk: NinePatchChunk?, bitmapWidth: Int, bitmapHeight: Int): IntArray {
            if (chunk == null) return IntArray(0)
            val xRegions = getRegions(chunk.xDivs, bitmapWidth)
            val yRegions = getRegions(chunk.yDivs, bitmapHeight)
            return IntArray(xRegions.size * yRegions.size) { NO_COLOR }
        }

        internal fun isRawNinePatchSource(source: NinePatchPixelSource?): Boolean {
            if (source == null) return false
            if (source.width < 3 || source.height < 3) return false
            if (!isCornerPixelsTransparent(source)) return false
            if (!hasNinePatchBorder(source)) return false
            return true
        }

        internal fun createChunkFromRawSource(source: NinePatchPixelSource?): NinePatchChunk {
            if (source == null) return createEmptyChunk()
            return try {
                createChunkFromRawSource(source, true)
            } catch (e: Exception) {
                createEmptyChunk()
            }
        }

        internal fun createChunkFromRawSource(source: NinePatchPixelSource, checkSource: Boolean): NinePatchChunk {
            if (checkSource && !isRawNinePatchSource(source)) {
                return createEmptyChunk()
            }
            val out = NinePatchChunk()
            setupStretchableRegions(source, out)
            setupPadding(source, out)
            setupColors(source, out)
            return out
        }

        private fun readDivs(divs: Int, reader: ByteArrayReader, target: MutableList<NinePatchDiv>) {
            repeat(divs) {
                target.add(NinePatchDiv(reader.readInt(), reader.readInt()))
            }
        }

        private fun checkDivCount(divCount: Int) {
            if (divCount == 0 || (divCount and 1) != 0) {
                throw DivLengthException("Div count should be aliquot 2 and more then 0, but was: $divCount")
            }
        }

        private fun setupColors(source: NinePatchPixelSource, out: NinePatchChunk) {
            val bitmapWidth = source.width - 2
            val bitmapHeight = source.height - 2
            val xRegions = getRegions(out.xDivs, bitmapWidth)
            val yRegions = getRegions(out.yDivs, bitmapHeight)
            out.colors = IntArray(xRegions.size * yRegions.size)

            var colorIndex = 0
            for (yDiv in yRegions) {
                for (xDiv in xRegions) {
                    val startX = xDiv.start + 1
                    val startY = yDiv.start + 1
                    if (hasSameColor(source, startX, xDiv.stop + 1, startY, yDiv.stop + 1)) {
                        var pixel = source.getPixel(startX, startY)
                        if (isTransparent(pixel)) {
                            pixel = TRANSPARENT_COLOR
                        }
                        out.colors[colorIndex] = pixel
                    } else {
                        out.colors[colorIndex] = NO_COLOR
                    }
                    colorIndex++
                }
            }
        }

        private fun hasSameColor(source: NinePatchPixelSource, startX: Int, stopX: Int, startY: Int, stopY: Int): Boolean {
            val color = source.getPixel(startX, startY)
            for (x in startX..stopX) {
                for (y in startY..stopY) {
                    if (color != source.getPixel(x, y)) return false
                }
            }
            return true
        }

        private fun setupPadding(source: NinePatchPixelSource, out: NinePatchChunk) {
            val maxXPixels = source.width - 2
            val maxYPixels = source.height - 2
            val xPaddings = getXDivs(source, source.height - 1)
            if (xPaddings.size > 1) {
                throw WrongPaddingException("Raw padding is wrong. Should be only one horizontal padding region")
            }
            val yPaddings = getYDivs(source, source.width - 1)
            if (yPaddings.size > 1) {
                throw WrongPaddingException("Column padding is wrong. Should be only one vertical padding region")
            }
            if (xPaddings.isEmpty()) xPaddings.add(out.xDivs[0])
            if (yPaddings.isEmpty()) yPaddings.add(out.yDivs[0])
            out.padding = NinePatchRect()
            out.padding.left = xPaddings[0].start
            out.padding.right = maxXPixels - xPaddings[0].stop
            out.padding.top = yPaddings[0].start
            out.padding.bottom = maxYPixels - yPaddings[0].stop
        }

        private fun setupStretchableRegions(source: NinePatchPixelSource, out: NinePatchChunk) {
            out.xDivs = getXDivs(source, 0)
            if (out.xDivs.isEmpty()) {
                throw DivLengthException("must be at least one horizontal stretchable region")
            }
            out.yDivs = getYDivs(source, 0)
            if (out.yDivs.isEmpty()) {
                throw DivLengthException("must be at least one vertical stretchable region")
            }
        }

        private fun getRegions(divs: List<NinePatchDiv>, max: Int): MutableList<NinePatchDiv> {
            val out = ArrayList<NinePatchDiv>()
            if (divs.isEmpty()) return out
            for (i in divs.indices) {
                val div = divs[i]
                if (i == 0 && div.start != 0) {
                    out.add(NinePatchDiv(0, div.start - 1))
                }
                if (i > 0) {
                    out.add(NinePatchDiv(divs[i - 1].stop, div.start - 1))
                }
                out.add(NinePatchDiv(div.start, div.stop - 1))
                if (i == divs.lastIndex && div.stop < max) {
                    out.add(NinePatchDiv(div.stop, max - 1))
                }
            }
            return out
        }

        private fun getYDivs(source: NinePatchPixelSource, column: Int): MutableList<NinePatchDiv> {
            val yDivs = ArrayList<NinePatchDiv>()
            var tmpDiv: NinePatchDiv? = null
            for (i in 1 until source.height) {
                tmpDiv = processChunk(source.getPixel(column, i), tmpDiv, i - 1, yDivs)
            }
            return yDivs
        }

        private fun getXDivs(source: NinePatchPixelSource, row: Int): MutableList<NinePatchDiv> {
            val xDivs = ArrayList<NinePatchDiv>()
            var tmpDiv: NinePatchDiv? = null
            for (i in 1 until source.width) {
                tmpDiv = processChunk(source.getPixel(i, row), tmpDiv, i - 1, xDivs)
            }
            return xDivs
        }

        private fun processChunk(pixel: Int, tmpDiv: NinePatchDiv?, position: Int, divs: MutableList<NinePatchDiv>): NinePatchDiv? {
            var currentDiv = tmpDiv
            if (isBlack(pixel)) {
                if (currentDiv == null) {
                    currentDiv = NinePatchDiv(start = position)
                }
            }
            if (isTransparent(pixel)) {
                if (currentDiv != null) {
                    currentDiv.stop = position
                    divs.add(currentDiv)
                    currentDiv = null
                }
            }
            return currentDiv
        }

        private fun hasNinePatchBorder(source: NinePatchPixelSource): Boolean {
            val width = source.width
            val height = source.height
            val lastXPixel = width - 1
            val lastYPixel = height - 1
            for (i in 1 until lastXPixel) {
                if (!isBorderPixel(source.getPixel(i, 0)) || !isBorderPixel(source.getPixel(i, lastYPixel))) {
                    return false
                }
            }
            for (i in 1 until lastYPixel) {
                if (!isBorderPixel(source.getPixel(0, i)) || !isBorderPixel(source.getPixel(lastXPixel, i))) {
                    return false
                }
            }
            if (getXDivs(source, 0).isEmpty()) return false
            if (getXDivs(source, lastYPixel).size > 1) return false
            if (getYDivs(source, 0).isEmpty()) return false
            if (getYDivs(source, lastXPixel).size > 1) return false
            return true
        }

        private fun isBorderPixel(pixel: Int): Boolean = isTransparent(pixel) || isBlack(pixel)

        private fun isCornerPixelsTransparent(source: NinePatchPixelSource): Boolean {
            val lastYPixel = source.height - 1
            val lastXPixel = source.width - 1
            return isTransparent(source.getPixel(0, 0)) &&
                isTransparent(source.getPixel(0, lastYPixel)) &&
                isTransparent(source.getPixel(lastXPixel, 0)) &&
                isTransparent(source.getPixel(lastXPixel, lastYPixel))
        }

        private fun isTransparent(color: Int): Boolean = ((color ushr 24) and 0xFF) == 0

        private fun isBlack(color: Int): Boolean = color == 0xFF000000.toInt()
    }

    private class ByteArrayReader(private val data: ByteArray) {
        private var position = 0

        fun readByte(): Byte {
            ensureAvailable(1)
            return data[position++]
        }

        fun readUnsignedByte(): Int = readByte().toInt() and 0xFF

        fun readInt(): Int {
            ensureAvailable(4)
            val b0 = data[position++].toInt() and 0xFF
            val b1 = data[position++].toInt() and 0xFF
            val b2 = data[position++].toInt() and 0xFF
            val b3 = data[position++].toInt() and 0xFF
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        private fun ensureAvailable(count: Int) {
            if (position + count > data.size) {
                throw IndexOutOfBoundsException("NinePatch chunk buffer underflow")
            }
        }
    }

    private class ByteArrayWriter(size: Int) {
        private val data = ByteArray(size)
        private var position = 0

        fun writeByte(value: Int) {
            data[position++] = value.toByte()
        }

        fun writeInt(value: Int) {
            data[position++] = (value and 0xFF).toByte()
            data[position++] = ((value ushr 8) and 0xFF).toByte()
            data[position++] = ((value ushr 16) and 0xFF).toByte()
            data[position++] = ((value ushr 24) and 0xFF).toByte()
        }

        fun toByteArray(): ByteArray = data
    }
}
