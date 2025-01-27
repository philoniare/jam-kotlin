package io.forge.jam.pvm.program

import io.forge.jam.pvm.U128
import io.forge.jam.pvm.engine.InstructionSet

interface OpcodeVisitor<R, I : InstructionSet> {
    val instructionSet: I
    fun dispatch(opcode: UInt, chunk: U128, offset: UInt, skip: UInt): R
}
