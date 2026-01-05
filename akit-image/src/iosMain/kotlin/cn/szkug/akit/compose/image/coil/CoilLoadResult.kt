package cn.szkug.akit.compose.image.coil

internal sealed interface CoilLoadResult<T> {
    @JvmInline
    value class Error<T>(val throwable: Throwable?) : CoilLoadResult<T>

    @JvmInline
    value class Success<T>(val value: T) : CoilLoadResult<T>

    @JvmInline
    value class Cleared<T>(val placeholder: Unit = Unit) : CoilLoadResult<T>
}
