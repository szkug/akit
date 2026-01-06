package cn.szkug.akit.compose.image.coil

internal sealed interface CoilLoadResult<T> {
    value class Error<T>(val throwable: Throwable?) : CoilLoadResult<T>

    value class Success<T>(val value: T) : CoilLoadResult<T>
    value class Cleared<T>(val placeholder: Any = Unit) : CoilLoadResult<T>
}
