package io.forge.jam.core

sealed class OptionalResult<out T, out E> {
    data class Ok<out T>(val value: T) : OptionalResult<T, Nothing>()
    data class Err<out E>(val error: E) : OptionalResult<Nothing, E>()

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

    fun <U> map(transform: (T) -> U): OptionalResult<U, E> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    fun <F> mapErr(transform: (E) -> F): OptionalResult<T, F> = when (this) {
        is Ok -> this
        is Err -> Err(transform(error))
    }

    companion object {
        fun <T> success(value: T): OptionalResult<T, Nothing> = Ok(value)
        fun <E> failure(error: E): OptionalResult<Nothing, E> = Err(error)
    }
}
