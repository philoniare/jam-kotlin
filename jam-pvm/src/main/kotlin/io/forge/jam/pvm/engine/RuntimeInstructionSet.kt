package io.forge.jam.pvm.engine

import io.forge.jam.pvm.program.Opcode

/**
 * Interface defining the contract for instruction sets
 */
interface InstructionSet {
    fun opcodeFromU8(byte: UByte): Opcode?
}

/**
 * Runtime instruction set configuration that determines the instruction set version and capabilities
 */
data class RuntimeInstructionSet(
    val allowSbrk: Boolean,
    val is64Bit: Boolean
) : InstructionSet {

    override fun opcodeFromU8(byte: UByte): Opcode? {
        return when {
            !is64Bit -> when {
                allowSbrk -> ISA32_V1.opcodeFromU8(byte)
                else -> ISA32_V1_NoSbrk.opcodeFromU8(byte)
            }

            else -> ISA64_V1.opcodeFromU8(byte)
        }
    }

    companion object {
        // Singleton instances of instruction sets
        private object ISA32_V1 : InstructionSet {
            override fun opcodeFromU8(byte: UByte): Opcode? {
                // Implementation for 32-bit ISA with sbrk
                TODO("Implement 32-bit ISA with sbrk")
            }
        }

        private object ISA32_V1_NoSbrk : InstructionSet {
            override fun opcodeFromU8(byte: UByte): Opcode? {
                // Implementation for 32-bit ISA without sbrk
                TODO("Implement 32-bit ISA without sbrk")
            }
        }

        private object ISA64_V1 : InstructionSet {
            override fun opcodeFromU8(byte: UByte): Opcode? {
                // Implementation for 64-bit ISA
                TODO("Implement 64-bit ISA")
            }
        }
    }
}
