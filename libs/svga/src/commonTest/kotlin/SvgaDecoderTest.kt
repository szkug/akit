@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package munchkin.svga

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvgaDecoderTest {

    @Test
    fun decodeZipSpec_resolvesAssetsAndKeepFrames() {
        val payload = buildStoredZip(
            mapOf(
                "movie.spec" to SPEC_FIXTURE.trimIndent().encodeToByteArray(),
                "images/rocket.png" to fakePngBytes(),
                "audio/track.mp3" to fakeMp3Bytes(),
            ),
        )

        val movie = SvgaDecoder.decode(payload)

        assertEquals("2.0", movie.version)
        assertEquals(120f, movie.width)
        assertEquals(80f, movie.height)
        assertEquals(24, movie.fps)
        assertEquals(2, movie.frames)
        assertTrue("rocket" in movie.bitmapAssets)
        assertTrue("track" in movie.audioAssets)
        val sprite = movie.sprites.single()
        assertEquals(2, sprite.frames.size)
        assertEquals(sprite.frames.first().shapes, sprite.frames.last().shapes)
        val audio = assertNotNull(movie.audioAssets["track"])
        assertEquals(1, audio.startFrame)
        assertEquals(2, audio.endFrame)
        assertEquals(30, audio.startTimeMillis)
        assertEquals(450, audio.totalTimeMillis)
    }

    @Test
    fun decodeZlibBinary_normalizesBitmapKeysAndClassifiesAudio() {
        val binary = ProtoBuf.encodeToByteArray(
            ProtoMovieEntity.serializer(),
            ProtoMovieEntity(
                version = "3.1",
                params = ProtoMovieParams(
                    viewBoxWidth = 64f,
                    viewBoxHeight = 64f,
                    fps = 20,
                    frames = 1,
                ),
                images = mapOf(
                    "rocket.matte" to fakePngBytes(),
                    "track" to fakeMp3Bytes(),
                ),
                sprites = listOf(
                    ProtoSpriteEntity(
                        imageKey = "rocket",
                        frames = listOf(
                            ProtoFrameEntity(
                                alpha = 1f,
                                layout = ProtoLayout(width = 24f, height = 24f),
                                transform = ProtoTransform(),
                            ),
                        ),
                    ),
                ),
                audios = listOf(
                    ProtoAudioEntity(
                        audioKey = "track",
                        startFrame = 0,
                        endFrame = 0,
                        startTime = 0,
                        totalTime = 120,
                    ),
                ),
            ),
        )

        val movie = SvgaDecoder.decode(wrapZlibStored(binary))

        assertEquals("3.1", movie.version)
        assertEquals(setOf("rocket"), movie.bitmapAssets.keys)
        val audio = assertNotNull(movie.audioAssets["track"])
        assertEquals(120, audio.totalTimeMillis)
        assertEquals(1, movie.sprites.size)
    }

    @Test
    fun dynamicEntity_dispatchesLastMatchingClickHandler() {
        val dynamic = SvgaDynamicEntity()
        var clicked = ""
        dynamic.setClickArea("background") { clicked = it }
        dynamic.setClickArea("foreground") { clicked = it }

        val hit = dynamic.dispatchClick(
            position = Offset(10f, 10f),
            regions = linkedMapOf(
                "background" to Rect(0f, 0f, 20f, 20f),
                "foreground" to Rect(5f, 5f, 15f, 15f),
            ),
        )

        assertTrue(hit)
        assertEquals("foreground", clicked)
        assertFalse(
            dynamic.dispatchClick(
                position = Offset(100f, 100f),
                regions = linkedMapOf("background" to Rect(0f, 0f, 20f, 20f)),
            ),
        )
    }

    @Test
    fun playerState_mutatorsUpdatePlaybackState() {
        val state = SvgaPlayerState(iterations = 1, autoPlay = false)

        assertFalse(state.isPlaying)
        state.play()
        assertTrue(state.isPlaying)
        state.seekToFrame(8)
        assertEquals(8, state.currentFrame)
        state.updateIterations(3)
        assertEquals(3, state.iterations)
        state.pause()
        assertFalse(state.isPlaying)
        state.stop()
        assertEquals(0, state.currentFrame)
        assertEquals(0, state.completedIterations)
        assertFalse(state.isPlaying)
        assertTrue(state.playbackVersion > 0)
    }

    @Test
    fun playerState_observersOnlyTrackControlMutations() {
        val state = SvgaPlayerState(iterations = 1, autoPlay = false)
        var notifications = 0
        val observer: SvgaPlaybackObserver = { notifications += 1 }

        state.addPlaybackObserver(observer)
        state.play()
        state.seekToFrame(2)
        state.updateIterations(5)
        state.pause()
        state.removePlaybackObserver(observer)
        state.stop()

        assertEquals(4, notifications)
    }
}

private const val SPEC_FIXTURE = """
{
  "ver": "2.0",
  "movie": {
    "viewBox": {
      "width": 120,
      "height": 80
    },
    "fps": 24,
    "frames": 2
  },
  "images": {
    "rocket": "rocket.png",
    "track": "track.mp3"
  },
  "audios": [
    {
      "audioKey": "track",
      "startFrame": 1,
      "endFrame": 2,
      "startTime": 30,
      "totalTime": 450
    }
  ],
  "sprites": [
    {
      "imageKey": "rocket",
      "frames": [
        {
          "alpha": 1,
          "layout": {
            "x": 12,
            "y": 8,
            "width": 36,
            "height": 24
          },
          "transform": {
            "a": 1,
            "d": 1
          },
          "shapes": [
            {
              "type": "rect",
              "args": {
                "x": 0,
                "y": 0,
                "width": 20,
                "height": 12,
                "cornerRadius": 3
              },
              "styles": {
                "fill": [255, 64, 64, 255]
              }
            }
          ]
        },
        {
          "alpha": 1,
          "layout": {
            "x": 12,
            "y": 8,
            "width": 36,
            "height": 24
          },
          "transform": {
            "a": 1,
            "d": 1
          },
          "shapes": [
            {
              "type": "keep"
            }
          ]
        }
      ]
    }
  ]
}
"""

private fun fakePngBytes(): ByteArray = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    0x00, 0x00, 0x00, 0x00,
)

private fun fakeMp3Bytes(): ByteArray = byteArrayOf(
    'I'.code.toByte(),
    'D'.code.toByte(),
    '3'.code.toByte(),
    0x04,
    0x00,
    0x00,
    0x00,
    0x00,
)

private fun buildStoredZip(entries: Map<String, ByteArray>): ByteArray {
    val locals = ArrayList<ByteArray>(entries.size)
    val centrals = ArrayList<ByteArray>(entries.size)
    var localOffset = 0
    entries.forEach { (name, data) ->
        val nameBytes = name.encodeToByteArray()
        val local = ByteArrayBuilder().apply {
            int(0x04034B50)
            short(20)
            short(0)
            short(0)
            short(0)
            short(0)
            int(0)
            int(data.size)
            int(data.size)
            short(nameBytes.size)
            short(0)
            bytes(nameBytes)
            bytes(data)
        }.toByteArray()
        locals += local
        centrals += ByteArrayBuilder().apply {
            int(0x02014B50)
            short(20)
            short(20)
            short(0)
            short(0)
            short(0)
            short(0)
            int(0)
            int(data.size)
            int(data.size)
            short(nameBytes.size)
            short(0)
            short(0)
            short(0)
            short(0)
            int(0)
            int(localOffset)
            bytes(nameBytes)
        }.toByteArray()
        localOffset += local.size
    }
    val centralDirectory = centrals.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    val end = ByteArrayBuilder().apply {
        int(0x06054B50)
        short(0)
        short(0)
        short(entries.size)
        short(entries.size)
        int(centralDirectory.size)
        int(localOffset)
        short(0)
    }.toByteArray()
    return locals.fold(ByteArray(0)) { acc, bytes -> acc + bytes } + centralDirectory + end
}

private fun wrapZlibStored(data: ByteArray): ByteArray {
    require(data.size <= 0xFFFF) { "Test fixture too large for stored block." }
    val adler = adler32(data)
    val len = data.size
    val nlen = len.inv() and 0xFFFF
    return ByteArrayBuilder().apply {
        byte(0x78)
        byte(0x01)
        byte(0x01)
        short(len)
        short(nlen)
        bytes(data)
        intBe(adler)
    }.toByteArray()
}

private fun adler32(data: ByteArray): Int {
    var s1 = 1
    var s2 = 0
    data.forEach { value ->
        s1 = (s1 + (value.toInt() and 0xFF)) % 65521
        s2 = (s2 + s1) % 65521
    }
    return (s2 shl 16) or s1
}

private class ByteArrayBuilder {
    private val bytes = ArrayList<Byte>()

    fun byte(value: Int) {
        bytes += value.toByte()
    }

    fun short(value: Int) {
        byte(value and 0xFF)
        byte((value ushr 8) and 0xFF)
    }

    fun int(value: Int) {
        byte(value and 0xFF)
        byte((value ushr 8) and 0xFF)
        byte((value ushr 16) and 0xFF)
        byte((value ushr 24) and 0xFF)
    }

    fun intBe(value: Int) {
        byte((value ushr 24) and 0xFF)
        byte((value ushr 16) and 0xFF)
        byte((value ushr 8) and 0xFF)
        byte(value and 0xFF)
    }

    fun bytes(value: ByteArray) {
        value.forEach { bytes += it }
    }

    fun toByteArray(): ByteArray = ByteArray(bytes.size) { index -> bytes[index] }
}
