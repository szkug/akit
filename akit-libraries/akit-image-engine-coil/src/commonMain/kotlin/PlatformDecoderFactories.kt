package akit.image.coil.support

import coil3.decode.Decoder

internal expect fun platformDecoderFactories(): List<Decoder.Factory>
