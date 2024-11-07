package io.forge.jam.pvm.engine

import io.forge.jam.pvm.program.ProgramCounter

/**
 * Arguments structure for VM instructions.
 */
data class Args(
    val a0: UInt = 0u,
    val a1: UInt = 0u,
    val a2: UInt = 0u,
    val a3: UInt = 0u
) {
    companion object {
        /**
         * Creates a default Args instance with all values set to 0
         */
        fun default(): Args = Args()

        fun trap(programCounter: ProgramCounter) = Args(a0 = programCounter.value)
    }
}
