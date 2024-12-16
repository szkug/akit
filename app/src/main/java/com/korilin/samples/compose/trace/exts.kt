package com.korilin.samples.compose.trace

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.NinePatchDrawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.core.content.ContextCompat
import com.skydoves.cloudy.CloudyState
import com.skydoves.cloudy.internals.render.RenderScriptToolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


val Dp.sp @Composable get() = with(LocalDensity.current) { toSp() }

val Dp.px @Composable get() = with(LocalDensity.current) { toPx() }

fun Modifier.draw9Patch(
    @DrawableRes ninePatchRes: Int,
) = composed {
    val context = LocalContext.current
    drawBehind {
        drawIntoCanvas {
            val ninePatch = ContextCompat.getDrawable(context, ninePatchRes)!! as NinePatchDrawable
            ninePatch.run {
                bounds = Rect(0, 0, size.width.toInt(), size.height.toInt())
                draw(it.nativeCanvas)
            }
        }
    }
}

@Composable
public fun Modifier.customBlur(
    radius: Int = 10,
    enabled: Boolean = true,
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
): Modifier {
    if (!enabled) {
        return this
    }

    // This local inspection preview only works over Android 12.
    if (LocalInspectionMode.current) {
        return this.blur(radius = radius.dp)
    }

    return this then BlurModifierNodeElement(
        graphicsLayer = graphicsLayer,
        radius = radius,
    )
}

private data class BlurModifierNodeElement(
    private val graphicsLayer: GraphicsLayer,
    val radius: Int = 10,
    val onStateChanged: (CloudyState) -> Unit = {}
) : ModifierNodeElement<BlurModifierNode>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "blur"
        properties["blur"] = radius
    }


    override fun create(): BlurModifierNode = BlurModifierNode(
        radius = radius,
    )

    override fun update(node: BlurModifierNode) {
        node.update(radius)
    }
}

private class BlurModifierNode(
    private var radius: Int = 10,
) : LayoutModifierNode, DrawModifierNode, Modifier.Node() {


    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints.offset())
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    private var blurredBitmap: ImageBitmap? = null
    private var layer: GraphicsLayer? = null
        set(value) {
            field?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
            field = value
        }

    override fun onAttach() {
        super.onAttach()
        Log.d("BlurNode", "onAttach")
        layer = requireGraphicsContext().createGraphicsLayer()
        blurredBitmap = null
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("BlurNode", "onDetach")
        layer = null
        blurredBitmap = null
    }

    override fun onReset() {
        super.onReset()
        blurredBitmap = null
        layer = requireGraphicsContext().createGraphicsLayer()
        invalidateDraw()
        Log.d("BlurNode", "onReset")
    }

    fun update(radius: Int) {
        this.radius = radius
        blurredBitmap = null
        layer = requireGraphicsContext().createGraphicsLayer()
        invalidateDraw()
        Log.d("BlurNode", "update")
    }

    override fun ContentDrawScope.draw() {
        // call record to capture the content in the graphics layer
        Log.d("BlurNode", "draw")
        layer?.record {
            // draw the contents of the composable into the graphics layer
            this@draw.drawContent()
        }
        val blurred = blurredBitmap

        if (blurred == null) iterativeBlur()
        else drawImage(blurred)
    }

    private fun iterativeBlur() = coroutineScope.launch(Dispatchers.IO) {
        runCatching {
            val targetBitmap = layer?.toImageBitmap()?.asAndroidBitmap()
                ?.copy(Bitmap.Config.ARGB_8888, true)
            if (targetBitmap != null) iterativeBlur(
                androidBitmap = targetBitmap,
                radius = radius
            )?.let {
                blurredBitmap = it.asImageBitmap()

                withContext(Dispatchers.Main) {
                    invalidateDraw()
                }
            }
        }
    }

    private fun iterativeBlur(
        androidBitmap: Bitmap,
        radius: Int
    ): Bitmap? {
        val iterate = (radius + 1) / 25
        var bitmap: Bitmap? = RenderScriptToolkit.blur(
            inputBitmap = androidBitmap,
            radius = (radius + 1) % 25
        )

        for (i in 0 until iterate) {
            bitmap = RenderScriptToolkit.blur(
                inputBitmap = bitmap,
                radius = 25
            )
        }

        return bitmap
    }
}