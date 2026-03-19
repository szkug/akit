package munchkin.resources.loader.coil.support

import coil3.decode.Decoder

internal actual fun platformDecoderFactories(): List<Decoder.Factory> {
    return listOf(VideoFrameDecoder.Factory())
}
