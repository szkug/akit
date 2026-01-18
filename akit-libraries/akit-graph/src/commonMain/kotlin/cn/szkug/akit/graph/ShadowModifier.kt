package cn.szkug.akit.graph

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val MAX_SHADOW_SAMPLES = 24
private const val DEFAULT_SHADOW_ALPHA = 0.35f

/**
 * Draws a soft shadow by layering translated outlines.
 *
 * @param shape Shape of the shadow.
 * @param color Shadow color (alpha is respected).
 * @param spreadAngle Fan angle in degrees to spread the shadow directions.
 * @param effect Shadow size used to control spread length.
 * @param offset Shadow offset direction and base distance.
 */
fun Modifier.akitShadow(
    shape: Shape = RectangleShape,
    color: Color = Color.Black.copy(alpha = DEFAULT_SHADOW_ALPHA),
    spreadAngle: Float = 0f,
    effect: Dp = 16.dp,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val effectPx = effect.toPx().coerceAtLeast(0f)
    val baseOffsetPx = Offset(offset.x.toPx(), offset.y.toPx())
    val spreadRad = (spreadAngle.coerceIn(0f, 360f) * (PI / 180f)).toFloat()

    val samples = when {
        effectPx <= 0f -> 1
        else -> (effectPx / 2f).roundToInt().coerceIn(2, MAX_SHADOW_SAMPLES)
    }

    val baseAngle = if (baseOffsetPx.magnitude() > 0.5f) {
        atan2(baseOffsetPx.y, baseOffsetPx.x)
    } else {
        0f
    }
    val startAngle = baseAngle - spreadRad / 2f
    val angleStep = if (samples > 1) spreadRad / (samples - 1) else 0f

    val offsets = ArrayList<Offset>(samples)
    val alphas = FloatArray(samples)

    for (index in 0 until samples) {
        val t = if (samples == 1) 1f else (index + 1f) / samples
        val distance = effectPx * t
        val angle = startAngle + angleStep * index
        val dx = cos(angle) * distance
        val dy = sin(angle) * distance
        offsets.add(baseOffsetPx + Offset(dx, dy))

        alphas[index] = if (samples == 1) {
            color.alpha
        } else {
            val falloff = 1f - t
            (color.alpha * (0.2f + 0.8f * falloff)).coerceIn(0f, 1f)
        }
    }

    onDrawBehind {
        for (index in 0 until samples) {
            val alpha = alphas[index]
            if (alpha <= 0f) continue
            val shadowOffset = offsets[index]
            translate(shadowOffset.x, shadowOffset.y) {
                drawOutline(outline = outline, color = color.copy(alpha = alpha))
            }
        }
    }
}

private fun Offset.magnitude(): Float = sqrt(x * x + y * y)
