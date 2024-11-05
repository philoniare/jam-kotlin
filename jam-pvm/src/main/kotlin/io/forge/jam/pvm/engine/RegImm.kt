package io.forge.jam.pvm.engine

import io.forge.jam.pvm.program.Reg


/**
 * Represents either a register or an immediate value
 */
sealed class RegImm {
    data class RegValue(val reg: Reg) : RegImm()
    data class ImmValue(val value: UInt) : RegImm()

    companion object {
        /**
         * Creates RegImm from a Register
         */
        @Suppress("NOTHING_TO_INLINE")
        inline operator fun invoke(reg: Reg): RegImm = RegValue(reg)

        /**
         * Creates RegImm from an unsigned integer
         */
        @Suppress("NOTHING_TO_INLINE")
        inline operator fun invoke(value: UInt): RegImm = ImmValue(value)
    }
}

/**
 * Interface for types that can be converted to RegImm
 * Similar to Rust's IntoRegImm trait
 */
interface IntoRegImm {
    fun into(): RegImm
}

/**
 * Extension function to convert UInt to RegImm
 */
@Suppress("NOTHING_TO_INLINE")
inline fun UInt.intoRegImm(): RegImm = RegImm.ImmValue(this)


@Suppress("NOTHING_TO_INLINE")
inline fun regImm(value: UInt): RegImm = value.intoRegImm()
