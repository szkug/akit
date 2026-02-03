package cn.szkug.akit.resources.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.szkug.akit.graph.toPainter

actual typealias ResourceId = Int

@Composable
actual fun stringResource(id: ResourceId, vararg formatArgs: Any): String {
    return androidx.compose.ui.res.stringResource(id, *formatArgs)
}

@Composable
actual fun pluralStringResource(id: ResourceId, vararg formatArgs: Any): String {
    val count = (formatArgs.firstOrNull() as? Number)?.toInt() ?: 0
    return androidx.compose.ui.res.pluralStringResource(id, count, *formatArgs)
}

@Composable
actual fun colorResource(id: ResourceId): Color {
    return androidx.compose.ui.res.colorResource(id)
}

@Composable
actual fun painterResource(id: ResourceId): Painter {
    val context = LocalContext.current
    return remember(context, id) {
        AppCompatResources.getDrawable(context, id)!!.toPainter()
    }
}

actual fun resolveResourcePath(id: ResourceId, localeOverride: String?): String? = null

@get:Composable
actual val ResourceId.toDp: Dp
    get() {
        val density = LocalDensity.current
        return readDimenValue(LocalContext.current, this).toDp(density)
    }

@get:Composable
actual val ResourceId.toSp: TextUnit
    get() {
        val density = LocalDensity.current
        return readDimenValue(LocalContext.current, this).toSp(density)
    }

private enum class DimenUnit {
    NONE,
    PX,
    DP,
    SP,
}

private data class DimenValue(
    val value: Float,
    val unit: DimenUnit,
)

@SuppressLint("WrongConstant")
private fun readDimenValue(context: Context, id: Int): DimenValue {
    val typedValue = TypedValue()
    return runCatching {
        context.resources.getValue(id, typedValue, true)
        when (typedValue.type) {
            TypedValue.TYPE_DIMENSION -> {
                val unit = typedValue.data and TypedValue.COMPLEX_UNIT_MASK
                val rawValue = TypedValue.complexToFloat(typedValue.data)
                when (unit) {
                    TypedValue.COMPLEX_UNIT_PX ->
                        DimenValue(rawValue, DimenUnit.PX)
                    TypedValue.COMPLEX_UNIT_DIP ->
                        DimenValue(rawValue, DimenUnit.DP)
                    TypedValue.COMPLEX_UNIT_SP ->
                        DimenValue(rawValue, DimenUnit.SP)
                    TypedValue.COMPLEX_UNIT_PT,
                    TypedValue.COMPLEX_UNIT_IN,
                    TypedValue.COMPLEX_UNIT_MM -> {
                        val px = TypedValue.applyDimension(unit, rawValue, context.resources.displayMetrics)
                        DimenValue(px, DimenUnit.PX)
                    }
                    else -> DimenValue(rawValue, DimenUnit.PX)
                }
            }
            TypedValue.TYPE_FLOAT,
            TypedValue.TYPE_INT_DEC,
            TypedValue.TYPE_INT_HEX -> DimenValue(0f, DimenUnit.NONE)
            else -> parseDimenString(context, typedValue.string?.toString())
        }
    }.getOrElse {
        DimenValue(0f, DimenUnit.NONE)
    }
}

private fun parseDimenString(context: Context, raw: String?): DimenValue {
    if (raw.isNullOrBlank()) return DimenValue(0f, DimenUnit.NONE)
    val trimmed = raw.trim()
    val match = Regex("""^([+-]?\d+(?:\.\d+)?)([a-zA-Z]+)?$""").find(trimmed) ?: return DimenValue(0f, DimenUnit.NONE)
    val value = match.groupValues[1].toFloatOrNull() ?: return DimenValue(0f, DimenUnit.NONE)
    val unit = match.groupValues.getOrNull(2).orEmpty().lowercase()
    return when (unit) {
        "dp", "dip" -> DimenValue(value, DimenUnit.DP)
        "sp" -> DimenValue(value, DimenUnit.SP)
        "px" -> DimenValue(value, DimenUnit.PX)
        "pt", "in", "mm" -> {
            val complexUnit = when (unit) {
                "pt" -> TypedValue.COMPLEX_UNIT_PT
                "in" -> TypedValue.COMPLEX_UNIT_IN
                else -> TypedValue.COMPLEX_UNIT_MM
            }
            val px = TypedValue.applyDimension(complexUnit, value, context.resources.displayMetrics)
            DimenValue(px, DimenUnit.PX)
        }
        else -> DimenValue(0f, DimenUnit.NONE)
    }
}

private fun DimenValue.toDp(density: Density): Dp = when (unit) {
    DimenUnit.NONE -> 0.dp
    DimenUnit.DP -> value.dp
    DimenUnit.PX -> (value / density.density).dp
    DimenUnit.SP -> (value * density.fontScale).dp
}

private fun DimenValue.toSp(density: Density): TextUnit {
    val spValue = when (unit) {
        DimenUnit.NONE -> 0f
        DimenUnit.SP -> value
        DimenUnit.DP -> value / density.fontScale
        DimenUnit.PX -> value / (density.density * density.fontScale)
    }
    return spValue.sp
}
