package munchkin.resources.runtime

interface ImageTransformation<T> {
    fun key(): String
    fun transform(context: RuntimeEngineContext, resource: T, width: Int, height: Int): T
}
