package cn.szkug.akit.image


interface ImageTransformation<T> {
    fun key(): String
    fun transform(context: PlatformImageContext, resource: T, width: Int, height: Int): T
}
