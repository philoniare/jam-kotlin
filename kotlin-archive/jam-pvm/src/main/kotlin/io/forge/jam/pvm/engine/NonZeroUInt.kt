package io.forge.jam.pvm.engine

/**
 * Represents a 32-bit unsigned integer that is guaranteed to never be zero.
 * This is similar to Rust's NonZeroU32.
 */
@JvmInline
value class NonZeroUInt(val value: UInt) : Comparable<NonZeroUInt> {
    companion object {
        /**
         * Creates a new NonZeroUInt if the value is not zero
         * @return NonZeroUInt if value > 0, null otherwise
         */
        fun new(value: UInt): NonZeroUInt? =
            if (value != 0u) NonZeroUInt(value) else null

        /**
         * Creates a new NonZeroUInt without checking if the value is non-zero.
         * WARNING: Should only be used when you are absolutely certain the value is non-zero.
         */
        @Suppress("NOTHING_TO_INLINE")
        inline fun unsafeNew(value: UInt): NonZeroUInt {
            require(value != 0u) { "Attempted to create NonZeroUInt with zero value" }
            return NonZeroUInt(value)
        }
    }

    /**
     * Returns the value as a regular UInt
     */
    fun get(): UInt = value

    override fun compareTo(other: NonZeroUInt): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()
}

// Extension properties to make it more ergonomic to use
val NonZeroUInt.toInt: Int get() = value.toInt()
val NonZeroUInt.toLong: Long get() = value.toLong()
val NonZeroUInt.toULong: ULong get() = value.toULong()
