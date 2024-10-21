package io.forge.jam.core

sealed class Result<out T, out E> {
    data class Ok<out T>(val value: T) : Result<T, Nothing>()
    data class Err<out E>(val error: E) : Result<Nothing, E>()

    fun isOk(): Boolean = this is Ok
    fun isErr(): Boolean = this is Err

    fun ok(): T? = when (this) {
        is Ok -> value
        is Err -> null
    }

    fun err(): E? = when (this) {
        is Ok -> null
        is Err -> error
    }

    fun <U> map(transform: (T) -> U): Result<U, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    fun <F> mapErr(transform: (E) -> F): Result<T, F> = when (this) {
        is Ok -> this
        is Err -> Err(transform(error))
    }

    companion object {
        fun <T> success(value: T): Result<T, Nothing> = Ok(value)
        fun <E> failure(error: E): Result<Nothing, E> = Err(error)
    }
}
