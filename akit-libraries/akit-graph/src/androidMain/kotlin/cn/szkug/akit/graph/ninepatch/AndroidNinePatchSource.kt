package cn.szkug.akit.graph.ninepatch

import android.graphics.Bitmap

private class AndroidNinePatchSource(private val bitmap: Bitmap) : NinePatchPixelSource {
    override val width: Int
        get() = bitmap.width
    override val height: Int
        get() = bitmap.height

    override fun getPixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)
}


fun Bitmap.asNinePatchSource(): NinePatchPixelSource = AndroidNinePatchSource(this)
