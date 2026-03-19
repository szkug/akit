package munchkin.resources.loader

interface ImageTransformation<T> {
    fun key(): String
    fun transform(context: EngineContext, resource: T, width: Int, height: Int): T
}
