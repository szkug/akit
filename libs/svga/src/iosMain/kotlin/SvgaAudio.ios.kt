@file:OptIn(
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package munchkin.svga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile

@Composable
internal actual fun rememberSvgaAudioEnvironment(): SvgaAudioEnvironment {
    return remember { AppleSvgaAudioEnvironment() }
}

private class AppleSvgaAudioEnvironment : SvgaAudioEnvironment {
    override fun createController(movie: SvgaMovie?): SvgaAudioController {
        return AppleSvgaAudioController(movie)
    }
}

private class AppleSvgaAudioController(
    private val movie: SvgaMovie?,
) : SvgaAudioController {
    private val activePlayers = linkedMapOf<String, AVAudioPlayer>()
    private var lastFrame = -1

    override fun resume() {
        activePlayers.values.forEach { player ->
            runCatching { player.play() }
        }
    }

    override fun pause() {
        activePlayers.values.forEach { player ->
            runCatching { player.pause() }
        }
    }

    override fun stop() {
        activePlayers.values.forEach { player ->
            runCatching { player.stop() }
        }
        activePlayers.clear()
        lastFrame = -1
    }

    override fun onFrame(frameIndex: Int) {
        val assets = movie?.audioAssets?.values ?: return
        if (frameIndex <= lastFrame) {
            stop()
        }
        lastFrame = frameIndex
        assets.forEach { asset ->
            if (frameIndex == asset.startFrame && asset.key !in activePlayers) {
                start(asset)
            }
            if (frameIndex >= asset.endFrame && asset.key in activePlayers) {
                stop(asset.key)
            }
        }
    }

    private fun start(asset: SvgaAudioAsset) {
        val filePath = NSTemporaryDirectory() + "/munchkin_svga_${asset.key.replace('/', '_')}"
        asset.bytes.writeToFile(filePath)
        val player = AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(filePath), error = null)
        if (asset.startTimeMillis > 0) {
            player.currentTime = asset.startTimeMillis.toDouble() / 1000.0
        }
        player.prepareToPlay()
        player.play()
        activePlayers[asset.key] = player
    }

    private fun stop(key: String) {
        val player = activePlayers.remove(key) ?: return
        runCatching { player.stop() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.writeToFile(path: String) {
    val data = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    data.writeToFile(path, atomically = true)
}
