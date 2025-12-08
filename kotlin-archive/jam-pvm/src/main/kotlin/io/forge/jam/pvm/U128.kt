package io.forge.jam.pvm

data class U128(val low: ULong, val high: ULong) {
    companion object {
        fun fromLEBytes(bytes: ByteArray, offset: Int = 0): U128 {
            // Read low 64 bits (first 8 bytes)
            val low = bytes.slice(offset until offset + 8)
                .foldIndexed(0UL) { index, acc, byte ->
                    acc or (byte.toULong() and 0xFFUL shl (8 * index))
                }

            // Read high 64 bits (next 8 bytes)
            val high = bytes.slice(offset + 8 until offset + 16)
                .foldIndexed(0UL) { index, acc, byte ->
                    acc or (byte.toULong() and 0xFFUL shl (8 * index))
                }

            return U128(low, high)
        }
    }

    infix fun shr(n: Int): U128 {
        require(n >= 0)
        when {
            n >= 128 -> return U128(0u, 0u)
            n >= 64 -> return U128(high shr (n - 64), 0u)
            n == 0 -> return this
            else -> {
                val newLow = (low shr n) or (high shl (64 - n))
                val newHigh = high shr n
                return U128(newLow, newHigh)
            }
        }
    }

    fun toULong(): ULong = low
    fun toUInt(): UInt = low.toUInt()
}
