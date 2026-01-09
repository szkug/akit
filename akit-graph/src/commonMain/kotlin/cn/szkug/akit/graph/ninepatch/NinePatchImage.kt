package cn.szkug.akit.graph.ninepatch

interface NinePatchImage {
    val width: Int
    val height: Int
    fun getPixel(x: Int, y: Int): Int
}
