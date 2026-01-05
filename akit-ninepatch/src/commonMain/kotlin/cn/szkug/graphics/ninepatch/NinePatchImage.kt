package cn.szkug.graphics.ninepatch

interface NinePatchImage {
    val width: Int
    val height: Int
    fun getPixel(x: Int, y: Int): Int
}
