package io.forge.jam.pvm.program

import io.forge.jam.pvm.engine.RegImm

/**
 * Represents a register in the VM architecture.
 * Each register has both an ABI name and a non-ABI numeric name.
 */
enum class Reg {
    RA,  // Return address register
    SP,  // Stack pointer
    T0,  // Temporary registers
    T1,
    T2,
    S0,  // Saved registers
    S1,
    A0,  // Argument registers
    A1,
    A2,
    A3,
    A4,
    A5;

    fun toIndex(): Int = ordinal

    /**
     * Returns the ABI name of the register (e.g., "ra", "sp", "a0").
     */
    fun abiName(): String = when (this) {
        RA -> "ra"
        SP -> "sp"
        T0 -> "t0"
        T1 -> "t1"
        T2 -> "t2"
        S0 -> "s0"
        S1 -> "s1"
        A0 -> "a0"
        A1 -> "a1"
        A2 -> "a2"
        A3 -> "a3"
        A4 -> "a4"
        A5 -> "a5"
    }

    /**
     * Returns the non-ABI numeric name of the register (e.g., "r0", "r1", "r2").
     */
    fun nameNonAbi(): String = when (this) {
        RA -> "r0"
        SP -> "r1"
        T0 -> "r2"
        T1 -> "r3"
        T2 -> "r4"
        S0 -> "r5"
        S1 -> "r6"
        A0 -> "r7"
        A1 -> "r8"
        A2 -> "r9"
        A3 -> "r10"
        A4 -> "r11"
        A5 -> "r12"
    }

    /**
     * Converts the register to its raw representation.
     */
    fun toRawReg(): RawReg = RawReg(ordinal.toUInt())

    /**
     * String representation using the ABI name.
     */
    override fun toString(): String = abiName()

    companion object {
        /**
         * Creates a register from a raw value.
         * @param value The raw register number
         * @return The corresponding register or null if the value is invalid
         */
        fun fromRaw(value: Int): Reg? = when (value) {
            0 -> RA
            1 -> SP
            2 -> T0
            3 -> T1
            4 -> T2
            5 -> S0
            6 -> S1
            7 -> A0
            8 -> A1
            9 -> A2
            10 -> A3
            11 -> A4
            12 -> A5
            else -> null
        }

        /**
         * List of all VM registers.
         */
        val ALL: List<Reg> = entries

        /**
         * List of all input/output argument registers.
         */
        val ARG_REGS: List<Reg> = listOf(A0, A1, A2, A3, A4, A5, T0, T1, T2)

        /**
         * Maximum number of input registers supported.
         */
        const val MAXIMUM_INPUT_REGS: Int = 9

        /**
         * Maximum number of output registers supported.
         */
        const val MAXIMUM_OUTPUT_REGS: Int = 2
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Reg.toRegImm(): RegImm = RegImm.RegValue(this)
