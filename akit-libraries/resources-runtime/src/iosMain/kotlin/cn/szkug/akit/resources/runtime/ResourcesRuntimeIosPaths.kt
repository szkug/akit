package cn.szkug.akit.resources.runtime

import platform.Foundation.NSBundle

/**
 * Path and bundle resolution helpers for compose-resources on iOS.
 *
 * Responsibility: construct bundle-relative resource paths and localized directories.
 * Performance: pure string operations; used by higher-level caches in LocalizationIos/ImageIos.
 */
internal object ResourcePathsIos {
    const val composeResourcesRoot = "compose-resources"

    fun resourceDirectory(prefix: String, directory: String): String {
        val parts = mutableListOf(composeResourcesRoot)
        if (prefix.isNotBlank()) parts += prefix
        if (directory.isNotBlank()) parts += directory
        return parts.joinToString("/")
    }

    fun bundleForPrefix(bundle: NSBundle, prefix: String): NSBundle {
        val resourcePath = bundle.resourcePath ?: return bundle
        val relative = resourceDirectory(prefix, "")
        val fullPath = if (relative.isBlank()) resourcePath else "$resourcePath/$relative"
        return NSBundle.bundleWithPath(fullPath) ?: bundle
    }

    fun localizedBundleForLocale(bundle: NSBundle, locale: String): NSBundle? {
        if (locale.isBlank()) return bundle
        val path = bundle.pathForResource(locale, "lproj") ?: return null
        return NSBundle.bundleWithPath(path)
    }

    fun localizedResourceDirectory(prefix: String, directory: String, locale: String): String {
        if (locale.isBlank()) return resourceDirectory(prefix, directory)
        val lproj = "$locale.lproj"
        val parts = mutableListOf(composeResourcesRoot)
        if (prefix.isNotBlank()) parts += prefix
        parts += lproj
        if (directory.isNotBlank()) parts += directory
        return parts.joinToString("/")
    }

    fun pathForResource(
        bundle: NSBundle,
        name: String,
        extension: String,
        directory: String,
    ): String? {
        return if (directory.isBlank()) {
            bundle.pathForResource(name, extension)
        } else {
            bundle.pathForResource(name, extension, directory)
                ?: bundle.pathForResource(name, extension)
        }
    }

    fun fullResourcePath(
        bundle: NSBundle,
        directory: String,
        name: String,
        extension: String,
    ): String {
        val file = if (extension.isBlank()) name else "$name.$extension"
        return if (directory.isBlank()) {
            "${bundle.bundlePath}/$file"
        } else {
            "${bundle.bundlePath}/$directory/$file"
        }
    }

    fun logMissingResource(kind: String, paths: List<String>) {
        if (paths.isEmpty()) return
        val message = buildString {
            append("AkitResources missing ")
            append(kind)
            append(" path(s):\n")
            for (path in paths) {
                append(" - ")
                append(path)
                append('\n')
            }
        }
        println(message.trimEnd())
    }
}
