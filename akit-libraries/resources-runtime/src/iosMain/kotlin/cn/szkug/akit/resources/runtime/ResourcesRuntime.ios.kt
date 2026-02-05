package cn.szkug.akit.resources.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import platform.Foundation.NSBundle

actual typealias ResourceId = NSResourceId
actual typealias StringResourceId = NSURLResourceId
actual typealias PluralStringResourceId = NSURLResourceId
actual typealias ColorResourceId = NSURLResourceId
actual typealias RawResourceId = NSURLResourceId
actual typealias PaintableResourceId = NSURLResourceId
actual typealias DimenResourceId = NSURLResourceId

/**
 * Load a localized string from Localizable.strings.
 *
 * Performance: uses cached string tables in LocalizationIos and `remember(id)` to avoid
 * repeated ID parsing during recomposition.
 */
@Composable
actual fun stringResource(id: StringResourceId, vararg formatArgs: Any): String {
    val localeOverride = LocalizationIos.userDefaultsLanguage()
    val info = remember(id) { ResourceIdsIos.decodeResourceId(id) }
    val bundle = NSBundle.mainBundle
    val locales = remember(localeOverride) { LocalizationIos.preferredLocales(bundle, localeOverride) }
    val raw = LocalizationIos.loadLocalizedStringOrThrow(bundle, info.prefix, info.value, locales)
    return LocalizationIos.formatString(raw, formatArgs)
}

/**
 * Load a plural string using Foundation's localizedStringWithFormat.
 *
 * Performance: relies on LocalizationIos caches; format normalization happens once per call.
 */
@Composable
actual fun pluralStringResource(
    id: PluralStringResourceId,
    count: Int,
    vararg formatArgs: Any,
): String {
    val localeOverride = LocalizationIos.userDefaultsLanguage()
    val info = remember(id) { ResourceIdsIos.decodeResourceId(id) }
    val bundle = NSBundle.mainBundle
    val locales = remember(localeOverride) { LocalizationIos.preferredLocales(bundle, localeOverride) }
    return LocalizationIos.loadLocalizedPluralOrThrow(bundle, info.prefix, info.value, locales, count, formatArgs)
}

/**
 * Load a color from Colors.strings.
 *
 * Performance: cached values tables + lightweight parsing.
 */
@Composable
actual fun colorResource(id: ColorResourceId): Color {
    val localeOverride = LocalizationIos.userDefaultsLanguage()
    val info = remember(id) { ResourceIdsIos.decodeResourceId(id) }
    val bundle = NSBundle.mainBundle
    val locales = remember(localeOverride) { LocalizationIos.preferredLocales(bundle, localeOverride) }
    val raw = LocalizationIos.loadLocalizedValueOrThrow(
        bundle,
        info.prefix,
        info.value,
        locales,
        LocalizationIos.valuesColorsFile,
        "color",
    )
    val color = ColorIos.parseColor(raw)
    if (color == Color.Unspecified) {
        error("Invalid color value for ${info.prefix}|${info.value}: \"$raw\"")
    }
    return color
}

/**
 * Load a bitmap or vector painter.
 *
 * Performance: vector XML parsing and bitmap decoding are wrapped in `remember` to avoid
 * repeated IO/parse work during recomposition.
 */
@Composable
actual fun painterResource(id: PaintableResourceId): Painter {
    val localeOverride = LocalizationIos.userDefaultsLanguage()
    val density = LocalDensity.current
    val (info, path) = remember(id) {
        val info = ResourceIdsIos.decodeResourceId(id)
        val path = ResourceIdsIos.parseResourcePath(info.value)
        info to path
    }
    val bundle = NSBundle.mainBundle
    val locales = remember(localeOverride) { LocalizationIos.preferredLocales(bundle, localeOverride) }
    return if (path.extension.equals("xml", ignoreCase = true)) {
        val vector = remember(path, localeOverride, density.density, density.fontScale) {
            val filePath = ImageIos.resolveImagePath(bundle, info.prefix, path, locales)
            val xml = filePath?.let { ImageIos.loadTextAtPath(it) }
            xml?.let { ImageIos.parseVectorXml(it, bundle, info.prefix, locales, density) }
        } ?: error("Failed to resolve vector path: name=${path.name} extension=${path.extension} directory=${path.directory}")
        rememberVectorPainter(vector)
    } else {
        remember(id, localeOverride) {
            val painter = ImageIos.loadPainter(bundle, info.prefix, path, locales)
            if (painter != null) return@remember painter
            error("Failed to resolve image path: name=${path.name} extension=${path.extension} directory=${path.directory}")
        }
    }
}

/**
 * Resolve a resource path without loading it.
 *
 * Performance: uses cached preferred locales and scale candidates.
 */
actual fun resolveResourcePath(id: ResourceId, localeOverride: String?): String? {
    val info = ResourceIdsIos.decodeResourceId(id)
    if (info.value.isBlank()) return null
    val path = ResourceIdsIos.parseResourcePath(info.value)
    val bundle = NSBundle.mainBundle
    val locales = LocalizationIos.preferredLocales(bundle, localeOverride ?: LocalizationIos.userDefaultsLanguage())
    return ImageIos.resolveImagePath(bundle, info.prefix, path, locales)
}

/**
 * Resolve a dimension value as Dp.
 *
 * Performance: dimen table parsing is cached; conversion is cheap.
 */
@get:Composable
actual val DimenResourceId.toDp: Dp
    get() {
        val localeOverride = LocalizationIos.userDefaultsLanguage()
        val info = remember(this) { ResourceIdsIos.decodeResourceId(this) }
        val bundle = NSBundle.mainBundle
        val locales = remember(localeOverride) { LocalizationIos.preferredLocales(bundle, localeOverride) }
        val value = LocalizationIos.loadLocalizedDimen(bundle, info.prefix, info.value, locales)
        return DimenIos.toDp(value, LocalDensity.current)
    }

/**
 * Resolve a dimension value as Sp.
 *
 * Performance: dimen table parsing is cached; conversion is cheap.
 */
@get:Composable
actual val DimenResourceId.toSp: TextUnit
    get() {
        val localeOverride = LocalizationIos.userDefaultsLanguage()
        val info = remember(this) { ResourceIdsIos.decodeResourceId(this) }
        val bundle = NSBundle.mainBundle
        val locales = remember(localeOverride) { LocalizationIos.preferredLocales(bundle, localeOverride) }
        val value = LocalizationIos.loadLocalizedDimen(bundle, info.prefix, info.value, locales)
        return DimenIos.toSp(value, LocalDensity.current)
    }
