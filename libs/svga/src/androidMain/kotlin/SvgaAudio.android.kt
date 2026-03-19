package munchkin.svga

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
internal actual fun rememberSvgaAudioEnvironment(): SvgaAudioEnvironment {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidSvgaAudioEnvironment(context) }
}

private class AndroidSvgaAudioEnvironment(
    private val context: Context,
) : SvgaAudioEnvironment {
    override fun createController(movie: SvgaMovie?): SvgaAudioController {
        return AndroidSvgaAudioController(context, movie)
    }
}

private class AndroidSvgaAudioController(
    private val context: Context,
    private val movie: SvgaMovie?,
) : SvgaAudioController {
    private val activePlayers = linkedMapOf<String, MediaPlayer>()
    private var lastFrame = -1

    override fun resume() {
        activePlayers.values.forEach { player ->
            runCatching {
                if (!player.isPlaying) player.start()
            }
        }
    }

    override fun pause() {
        activePlayers.values.forEach { player ->
            runCatching {
                if (player.isPlaying) player.pause()
            }
        }
    }

    override fun stop() {
        activePlayers.values.forEach { player ->
            runCatching { player.stop() }
            player.release()
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
        val audioDir = File(context.cacheDir, "munchkin-svga-audio").apply { mkdirs() }
        val file = File(audioDir, asset.key.replace('/', '_'))
        if (!file.exists()) {
            file.writeBytes(asset.bytes)
        }
        val player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            if (asset.startTimeMillis > 0) {
                seekTo(asset.startTimeMillis.coerceAtLeast(0))
            }
            setOnCompletionListener {
                stop(asset.key)
            }
            start()
        }
        activePlayers[asset.key] = player
    }

    private fun stop(key: String) {
        val player = activePlayers.remove(key) ?: return
        runCatching { player.stop() }
        player.release()
    }
}
