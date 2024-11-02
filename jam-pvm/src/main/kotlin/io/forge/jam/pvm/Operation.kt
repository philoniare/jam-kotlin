package io.forge.jam.pvm

object Operation {
    /**
     * Unsigned division. Returns UInt.MAX_VALUE if rhs is 0.
     */
    @JvmStatic
    fun divu(lhs: UInt, rhs: UInt): UInt =
        if (rhs == 0u) UInt.MAX_VALUE else lhs / rhs

    /**
     * Unsigned division. Returns ULong.MAX_VALUE if rhs is 0.
     */
    @JvmStatic
    fun divu64(lhs: ULong, rhs: ULong): ULong =
        if (rhs == 0uL) ULong.MAX_VALUE else lhs / rhs

    /**
     * Unsigned remainder. Returns lhs if rhs is 0.
     */
    @JvmStatic
    fun remu(lhs: UInt, rhs: UInt): UInt =
        if (rhs == 0u) lhs else lhs % rhs

    /**
     * Unsigned remainder. Returns lhs if rhs is 0.
     */
    @JvmStatic
    fun remu64(lhs: ULong, rhs: ULong): ULong =
        if (rhs == 0uL) lhs else lhs % rhs

    /**
     * Signed division with special handling for edge cases.
     * Returns -1 if rhs is 0.
     * Returns lhs if dividing Int.MIN_VALUE by -1 to avoid overflow.
     */
    @JvmStatic
    fun div(lhs: Int, rhs: Int): Int = when {
        rhs == 0 -> -1
        lhs == Int.MIN_VALUE && rhs == -1 -> lhs
        else -> lhs / rhs
    }

    /**
     * Signed division with special handling for edge cases.
     * Returns -1 if rhs is 0.
     * Returns lhs if dividing Long.MIN_VALUE by -1 to avoid overflow.
     */
    @JvmStatic
    fun div64(lhs: Long, rhs: Long): Long = when {
        rhs == 0L -> -1L
        lhs == Long.MIN_VALUE && rhs == -1L -> lhs
        else -> lhs / rhs
    }

    /**
     * Signed remainder with special handling for edge cases.
     * Returns lhs if rhs is 0.
     * Returns 0 if computing remainder of Int.MIN_VALUE by -1.
     */
    @JvmStatic
    fun rem(lhs: Int, rhs: Int): Int = when {
        rhs == 0 -> lhs
        lhs == Int.MIN_VALUE && rhs == -1 -> 0
        else -> lhs % rhs
    }

    /**
     * Signed remainder with special handling for edge cases.
     * Returns lhs if rhs is 0.
     * Returns 0 if computing remainder of Long.MIN_VALUE by -1.
     */
    @JvmStatic
    fun rem64(lhs: Long, rhs: Long): Long = when {
        rhs == 0L -> lhs
        lhs == Long.MIN_VALUE && rhs == -1L -> 0L
        else -> lhs % rhs
    }

    /**
     * High word of signed multiplication.
     */
    @JvmStatic
    fun mulh(lhs: Int, rhs: Int): Int =
        ((lhs.toLong() * rhs.toLong()) shr 32).toInt()

    /**
     * High word of signed multiplication (64-bit).
     */
    @JvmStatic
    fun mulh64(lhs: Long, rhs: Long): Long {
        // Simulate 128-bit multiplication using lower and upper bits
        val lhsLow = lhs and 0xFFFFFFFFL
        val lhsHigh = lhs shr 32
        val rhsLow = rhs and 0xFFFFFFFFL
        val rhsHigh = rhs shr 32

        val lowLow = lhsLow * rhsLow
        val lowHigh = lhsLow * rhsHigh
        val highLow = lhsHigh * rhsLow
        val highHigh = lhsHigh * rhsHigh

        val mid = (lowLow shr 32) + (lowHigh and 0xFFFFFFFFL) + (highLow and 0xFFFFFFFFL)
        val carry = mid shr 32

        return highHigh + (lowHigh shr 32) + (highLow shr 32) + carry
    }

    /**
     * High word of signed * unsigned multiplication.
     */
    @JvmStatic
    fun mulhsu(lhs: Int, rhs: UInt): Int =
        ((lhs.toLong() * rhs.toLong()) shr 32).toInt()

    /**
     * High word of signed * unsigned multiplication (64-bit).
     */
    @JvmStatic
    fun mulhsu64(lhs: Long, rhs: ULong): Long {
        val rhsLong = rhs.toLong()
        return mulh64(lhs, rhsLong)
    }

    /**
     * High word of unsigned multiplication.
     */
    @JvmStatic
    fun mulhu(lhs: UInt, rhs: UInt): UInt =
        ((lhs.toLong() * rhs.toLong()) shr 32).toUInt()

    /**
     * High word of unsigned multiplication (64-bit).
     */
    @JvmStatic
    fun mulhu64(lhs: ULong, rhs: ULong): ULong {
        val lhsLong = lhs.toLong()
        val rhsLong = rhs.toLong()
        return mulh64(lhsLong, rhsLong).toULong()
    }
}
