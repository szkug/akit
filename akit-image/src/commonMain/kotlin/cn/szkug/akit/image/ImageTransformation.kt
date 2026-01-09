package cn.szkug.akit.image


interface ImageTransformation<T> {
    fun key(): String
    fun transform(context: PlatformContext, resource: T, width: Int, height: Int): T
}
