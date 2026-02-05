package cn.szkug.akit.resources.runtime

import androidx.compose.ui.graphics.Color

/**
 * Color parsing helpers for values/Colors.strings.
 *
 * Responsibility: parse Android-style hex colors into Compose Color.
 * Performance: pure parsing; callers can cache resolved colors at a higher level.
 */
internal object ColorIos {
    fun parseColor(raw: String): Color {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || !trimmed.startsWith("#")) return Color.Unspecified
        val hex = trimmed.removePrefix("#")
        fun hexByte(value: String): Int? = value.toIntOrNull(16)
        val argb = when (hex.length) {
            3 -> {
                val r = hexByte("${hex[0]}${hex[0]}") ?: return Color.Unspecified
                val g = hexByte("${hex[1]}${hex[1]}") ?: return Color.Unspecified
                val b = hexByte("${hex[2]}${hex[2]}") ?: return Color.Unspecified
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            4 -> {
                val a = hexByte("${hex[0]}${hex[0]}") ?: return Color.Unspecified
                val r = hexByte("${hex[1]}${hex[1]}") ?: return Color.Unspecified
                val g = hexByte("${hex[2]}${hex[2]}") ?: return Color.Unspecified
                val b = hexByte("${hex[3]}${hex[3]}") ?: return Color.Unspecified
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            6 -> {
                val r = hexByte(hex.substring(0, 2)) ?: return Color.Unspecified
                val g = hexByte(hex.substring(2, 4)) ?: return Color.Unspecified
                val b = hexByte(hex.substring(4, 6)) ?: return Color.Unspecified
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            8 -> {
                val a = hexByte(hex.substring(0, 2)) ?: return Color.Unspecified
                val r = hexByte(hex.substring(2, 4)) ?: return Color.Unspecified
                val g = hexByte(hex.substring(4, 6)) ?: return Color.Unspecified
                val b = hexByte(hex.substring(6, 8)) ?: return Color.Unspecified
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            else -> return Color.Unspecified
        }
        return Color(argb)
    }
}
