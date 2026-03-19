package munchkin.resources.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import munchkin.image.AsyncLoadData
import munchkin.image.AsyncRequestEngine
import munchkin.image.EngineContext
import munchkin.resources.runtime.RawResourceId

sealed interface BinarySource {
    data class Url(
        val value: String,
        val headers: Map<String, String> = emptyMap(),
    ) : BinarySource

    data class Raw(
        val id: RawResourceId,
    ) : BinarySource

    data class FilePath(
        val path: String,
    ) : BinarySource

    data class UriPath(
        val value: String,
    ) : BinarySource

    data class Bytes(
        val value: ByteArray,
        val cacheKey: String = "inline-bytes",
    ) : BinarySource
}

data class BinaryPayload(
    val bytes: ByteArray,
    val cacheKey: String,
    val source: BinarySource,
)

class BinaryAsyncLoadData(
    val payload: BinaryPayload,
) : AsyncLoadData {
    override fun painter(): Painter = EmptyBinaryPainter
}

interface BinaryRequestEngine {
    suspend fun requestBinary(
        engineContext: EngineContext,
        source: BinarySource,
    ): BinaryAsyncLoadData
}

internal interface PlatformBinarySourceLoader {
    suspend fun load(source: BinarySource): BinaryPayload
}

@Composable
internal expect fun rememberPlatformBinarySourceLoader(): PlatformBinarySourceLoader

@Composable
fun rememberFallbackBinaryPayloadLoader(): suspend (BinarySource) -> BinaryPayload {
    val loader = rememberPlatformBinarySourceLoader()
    return remember(loader) { { source -> loader.load(source) } }
}

fun BinarySource.cacheKey(): String {
    return when (this) {
        is BinarySource.Bytes -> cacheKey
        is BinarySource.FilePath -> path
        is BinarySource.Raw -> id.toString()
        is BinarySource.UriPath -> value
        is BinarySource.Url -> value
    }
}

internal object EmptyBinaryPainter : Painter() {
    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() = Unit
}
