package cn.szkug.akit.resources.runtime

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dimen parsing and unit conversion for iOS resources.
 *
 * Responsibility: parse values from Dimens.strings and convert to Dp/Sp.
 * Performance: parsing happens once per key via LocalizationIos cache; conversions are lightweight.
 */
internal object DimenIos {
    private const val dpPerInch = 160f
    private const val dpPerMm = dpPerInch / 25.4f
    private const val dpPerPt = dpPerInch / 72f

    enum class Unit {
        NONE,
        PX,
        DP,
        SP,
        IN,
        MM,
        PT,
    }

    data class Value(
        val value: Float,
        val unit: Unit,
    )

    fun parse(raw: String): Value? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val match = Regex("""^([+-]?\d+(?:\.\d+)?)([a-zA-Z]+)?$""").find(trimmed) ?: return null
        val value = match.groupValues[1].toFloatOrNull() ?: return null
        val unit = match.groupValues.getOrNull(2).orEmpty().lowercase()
        val dimenUnit = when (unit) {
            "dp", "dip" -> Unit.DP
            "sp" -> Unit.SP
            "px" -> Unit.PX
            "in" -> Unit.IN
            "mm" -> Unit.MM
            "pt" -> Unit.PT
            else -> return null
        }
        return Value(value, dimenUnit)
    }

    fun toDp(value: Value, density: Density): Dp = when (value.unit) {
        Unit.NONE -> 0.dp
        Unit.DP -> value.value.dp
        Unit.PX -> (value.value / density.density).dp
        Unit.SP -> (value.value * density.fontScale).dp
        Unit.IN -> (value.value * dpPerInch).dp
        Unit.MM -> (value.value * dpPerMm).dp
        Unit.PT -> (value.value * dpPerPt).dp
    }

    fun toSp(value: Value, density: Density): TextUnit {
        val spValue = when (value.unit) {
            Unit.NONE -> 0f
            Unit.SP -> value.value
            Unit.DP -> value.value / density.fontScale
            Unit.PX -> value.value / (density.density * density.fontScale)
            Unit.IN -> (value.value * dpPerInch) / density.fontScale
            Unit.MM -> (value.value * dpPerMm) / density.fontScale
            Unit.PT -> (value.value * dpPerPt) / density.fontScale
        }
        return spValue.sp
    }
}
