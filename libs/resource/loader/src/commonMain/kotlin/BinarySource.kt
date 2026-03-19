package munchkin.resources.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import munchkin.resources.runtime.RawResourceId
import kotlin.reflect.KClass

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

data class BinaryAsyncLoadData(
    val payload: BinaryPayload,
)

interface BinaryEngineContext

typealias BinaryEngineContextProvider = @Composable () -> BinaryEngineContext

object LocalBinaryEngineContextRegister {

    private val registration = mutableMapOf<KClass<out BinaryRequestEngine>, BinaryEngineContextProvider>()

    fun register(type: KClass<out BinaryRequestEngine>, provider: BinaryEngineContextProvider) {
        registration[type] = provider
    }

    @Composable
    fun resolve(engine: BinaryRequestEngine): BinaryEngineContext {
        val provider = registration[engine::class]
            ?: error("No BinaryEngineContext provider found, it must register first.")
        return provider.invoke()
    }
}

interface BinaryRequestEngine {
    suspend fun requestBinary(
        engineContext: BinaryEngineContext,
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
