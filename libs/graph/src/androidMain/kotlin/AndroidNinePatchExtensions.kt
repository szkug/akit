package munchkin.graph.ninepatch

fun NinePatchRect.toAndroidRect() = android.graphics.Rect(
    left, top, right, bottom
)
