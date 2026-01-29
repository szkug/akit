@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.szkug.akit.graph.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import cn.szkug.akit.graph.AnimatablePainter
import cn.szkug.akit.graph.EmptyPainter
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.Rect
import org.jetbrains.skia.skottie.Animation
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

private const val DEFAULT_DURATION_SEC = 1f

@Composable
actual fun rememberLottiePainter(resource: LottieResource): Painter {
    val resolvedPath = resolveLottiePath(resource.resource)
    return remember(resource.resource, resource.iterations, resolvedPath) {
        val animation = resolvedPath?.let { loadLottieAnimation(it) }
        if (animation == null) EmptyPainter else SkottiePainter(animation, resource.iterations)
    }
}

private class SkottiePainter(
    private val animation: Animation,
    private val iterations: Int,
) : Painter(), AnimatablePainter, RememberObserver {

    private var frameTimeSeconds by mutableFloatStateOf(0f)
    private var drawTick by mutableIntStateOf(0)
    private var animationJob: Job? = null

    init {
        animation.seekFrameTime(0f)
    }

    override val intrinsicSize: Size = Size(animation.width, animation.height)

    override fun DrawScope.onDraw() {
        drawTick
        val dst = Rect.makeXYWH(0f, 0f, size.width, size.height)
        animation.seekFrameTime(frameTimeSeconds)
        drawIntoCanvas { canvas ->
            animation.render(canvas.nativeCanvas, dst)
        }
    }

    override fun startAnimation(coroutineContext: kotlin.coroutines.CoroutineContext) {
        if (animationJob?.isActive == true) return
        val maxLoops = if (iterations < 0) Int.MAX_VALUE else iterations + 1
        val duration = animation.duration.takeIf { it > 0f } ?: DEFAULT_DURATION_SEC
        val frameContext = if (coroutineContext[MonotonicFrameClock] == null) {
            @Suppress("DEPRECATION")
            coroutineContext + DefaultMonotonicFrameClock
        } else {
            coroutineContext
        }
        animationJob = CoroutineScope(frameContext).launch {
            var startNanos = 0L
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (startNanos == 0L) startNanos = frameTimeNanos
                    val elapsedSec = (frameTimeNanos - startNanos) / 1_000_000_000.0
                    val loopsDone = (elapsedSec / duration).toInt()
                    if (loopsDone >= maxLoops) {
                        stopAnimation()
                        return@withFrameNanos
                    }
                    val timeInLoop = elapsedSec - loopsDone * duration
                    frameTimeSeconds = timeInLoop.toFloat()
                    drawTick++
                }
            }
        }
    }

    override fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
    }

    override fun onRemembered() {}

    override fun onAbandoned() = stopAnimation()

    override fun onForgotten() = stopAnimation()
}

private fun resolveLottiePath(resource: Any): String? = when (resource) {
    is String -> resource
    is NSURL -> resolveResourcePath(resource)
    else -> null
}

private data class ResourceInfo(
    val frameworkName: String,
    val prefix: String,
    val value: String,
)

private data class ResourcePath(
    val directory: String,
    val name: String,
    val extension: String,
)

private val bundleCache = mutableMapOf<String, NSBundle>()
private val preferredLocalesCache = mutableMapOf<String, List<String>>()

private fun resolveResourcePath(id: NSURL): String? {
    val rawPath = id.path?.trimStart('/') ?: return null
    if (!rawPath.contains('|') && !rawPath.contains("%7C") && !rawPath.contains("%7c")) {
        return id.path
    }
    val info = decodeResourceId(id)
    if (info.value.isBlank()) return null
    val path = parseResourcePath(info.value)
    val bundle = resourceBundle(info.frameworkName)
    val locales = preferredLocales(bundle, userDefaultsLanguage())
    return resolveResourcePath(bundle, info.prefix, path, locales)
}

private fun decodeResourceId(id: NSURL): ResourceInfo {
    val rawPath = id.path?.trimStart('/') ?: ""
    val decodedPath = rawPath.replace("%7C", "|").replace("%7c", "|")
    val parts = decodedPath.split('|', limit = 3)
    val frameworkName = parts.getOrNull(0).orEmpty()
    val prefix = parts.getOrNull(1).orEmpty()
    val value = parts.getOrNull(2).orEmpty()
    return ResourceInfo(frameworkName, prefix, value)
}

private fun parseResourcePath(value: String): ResourcePath {
    val normalized = value.trimStart('/')
    val directory = normalized.substringBeforeLast('/', "")
    val fileName = normalized.substringAfterLast('/', normalized)
    val name = fileName.substringBeforeLast('.', fileName)
    val extension = fileName.substringAfterLast('.', "")
    if (extension.isBlank()) {
        error("Missing extension in resource path: $value")
    }
    return ResourcePath(directory = directory, name = name, extension = extension)
}

private fun resourceBundle(frameworkName: String): NSBundle {
    if (frameworkName.isBlank()) return NSBundle.mainBundle
    return bundleCache.getOrPut(frameworkName) {
        val frameworksPath = NSBundle.mainBundle.privateFrameworksPath ?: return@getOrPut NSBundle.mainBundle
        val frameworkPath = "$frameworksPath/$frameworkName.framework"
        NSBundle.bundleWithPath(frameworkPath) ?: NSBundle.mainBundle
    }
}

private const val appLanguageKey = "akit.app.language"
private const val appleLanguagesKey = "AppleLanguages"

private fun userDefaultsLanguage(): String? {
    val defaults = NSUserDefaults.standardUserDefaults
    val direct = defaults.stringForKey(appLanguageKey)?.trim()?.takeIf { it.isNotEmpty() }
    if (direct != null) return direct
    val languages = defaults.objectForKey(appleLanguagesKey) as? List<*>
    val first = languages?.firstOrNull() as? String
    return first?.trim()?.takeIf { it.isNotEmpty() }
}

private fun preferredLocales(bundle: NSBundle, overrideLocale: String?): List<String> {
    val key = buildString {
        append(bundle.bundlePath)
        append('|')
        append(overrideLocale.orEmpty())
    }
    return preferredLocalesCache.getOrPut(key) {
        val locales = if (overrideLocale.isNullOrBlank()) {
            bundle.preferredLocalizations.mapNotNull { it as? String }
        } else {
            listOf(overrideLocale)
        }
        val out = mutableListOf<String>()
        for (locale in locales) {
            val normalized = locale.replace('_', '-').trim()
            if (normalized.isBlank()) continue
            out += normalized
            val base = normalized.substringBefore('-')
            if (base.isNotBlank() && base != normalized) {
                out += base
            }
        }
        out += "Base"
        out += ""
        out.distinct()
    }
}

private fun resolveResourcePath(
    bundle: NSBundle,
    prefix: String,
    path: ResourcePath,
    locales: List<String>,
): String? {
    for (locale in locales) {
        val directory = localizedResourceDirectory(prefix, path.directory, locale)
        val resolved = pathForResource(bundle, path.name, path.extension, directory)
        if (resolved != null) return resolved
    }
    return null
}

private fun localizedResourceDirectory(prefix: String, directory: String, locale: String): String {
    if (locale.isBlank()) return resourceDirectory(prefix, directory)
    val lproj = "$locale.lproj"
    return if (prefix.isBlank()) {
        if (directory.isBlank()) lproj else "$lproj/$directory"
    } else {
        if (directory.isBlank()) "$prefix/$lproj" else "$prefix/$lproj/$directory"
    }
}

private fun resourceDirectory(prefix: String, directory: String): String {
    if (prefix.isBlank()) return directory
    if (directory.isBlank()) return prefix
    return "$prefix/$directory"
}

private fun pathForResource(bundle: NSBundle, name: String, extension: String, directory: String): String? {
    return if (directory.isBlank()) {
        bundle.pathForResource(name, extension)
    } else {
        bundle.pathForResource(name, extension, directory)
            ?: bundle.pathForResource(name, extension)
    }
}

private fun loadLottieAnimation(filePath: String): Animation? {
    val data = NSData.dataWithContentsOfFile(filePath) ?: return null
    val bytes = data.toByteArray()
    if (bytes.isEmpty()) return null
    val json = bytes.decodeToString()
    return runCatching { Animation.makeFromString(json) }.getOrNull()
}

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val source = this.bytes ?: return ByteArray(0)
    val buffer = ByteArray(length)
    buffer.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, this.length)
    }
    return buffer
}
