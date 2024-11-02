package io.forge.jam.pvm.program

import io.forge.jam.pvm.readSimpleVarint

/**
 * Static container for program-wide operations and lookup tables.
 */
object Program {
    /**
     * Lookup table built with offset 1
     */
    val TABLE_1: LookupTable = LookupTable.build(1)

    /**
     * Lookup table built with offset 2
     */
    val TABLE_2: LookupTable = LookupTable.build(2)

    /**
     * Sign extends a value at a specific bit position
     */
    private fun signExtendAt(value: UInt, bitsToCut: UInt): UInt {
        // Simulate Rust's wrapping behavior
        return ((((value.toLong() shl bitsToCut.toInt()) and 0xFFFFFFFF).toInt()
            shr bitsToCut.toInt()) and 0xFFFFFFFF.toInt()).toUInt()
    }

    /**
     * Reads immediate argument from chunk
     */
    fun readArgsImm(chunk: ULong, skip: UInt): UInt =
        readSimpleVarint(chunk.toUInt(), skip)

    /**
     * Reads offset argument from chunk
     */
    fun readArgsOffset(chunk: ULong, instructionOffset: UInt, skip: UInt): UInt =
        instructionOffset + readArgsImm(chunk, skip) // UInt addition wraps automatically

    /**
     * Reads two immediate arguments from chunk
     */
    fun readArgsImm2(chunk: ULong, skip: UInt): Pair<UInt, UInt> {
        val (imm1Bits, imm1Skip, imm2Bits) = TABLE_1.get(skip, chunk.toUInt())
        var shiftedChunk = chunk shr 8
        val imm1 = signExtendAt(shiftedChunk.toUInt(), imm1Bits)
        shiftedChunk = shiftedChunk shr imm1Skip.toInt()
        val imm2 = signExtendAt(shiftedChunk.toUInt(), imm2Bits)
        return Pair(imm1, imm2)
    }

    /**
     * Reads register and immediate arguments from chunk
     */
    fun readArgsRegImm(chunk: ULong, skip: UInt): Pair<RawReg, UInt> {
        val reg = RawReg(chunk.toUInt())
        val shiftedChunk = chunk shr 8
        val (_, _, immBits) = TABLE_1.get(skip, 0u)
        val imm = signExtendAt(shiftedChunk.toUInt(), immBits)
        return Pair(reg, imm)
    }

    /**
     * Reads register and two immediate arguments from chunk
     */
    fun readArgsRegImm2(chunk: ULong, skip: UInt): Triple<RawReg, UInt, UInt> {
        val reg = RawReg(chunk.toUInt())
        val (imm1Bits, imm1Skip, imm2Bits) = TABLE_1.get(skip, chunk.toUInt() shr 4)
        var shiftedChunk = chunk shr 8
        val imm1 = signExtendAt(shiftedChunk.toUInt(), imm1Bits)
        shiftedChunk = shiftedChunk shr imm1Skip.toInt()
        val imm2 = signExtendAt(shiftedChunk.toUInt(), imm2Bits)
        return Triple(reg, imm1, imm2)
    }

    /**
     * Reads register and immediate offset arguments from chunk
     */
    fun readArgsRegImmOffset(
        chunk: ULong,
        instructionOffset: UInt,
        skip: UInt
    ): Triple<RawReg, UInt, UInt> {
        val (reg, imm1, imm2) = readArgsRegImm2(chunk, skip)
        return Triple(reg, imm1, instructionOffset + imm2)
    }

    /**
     * Reads two registers and two immediate arguments from chunk
     */
    fun readArgsRegs2Imm2(chunk: ULong, skip: UInt): Array<Any> {
        val value = chunk.toUInt()
        val reg1 = RawReg(value)
        val reg2 = RawReg(value shr 4)
        val imm1Aux = value shr 8

        val (imm1Bits, imm1Skip, imm2Bits) = TABLE_2.get(skip, imm1Aux)
        var shiftedChunk = chunk shr 16
        val imm1 = signExtendAt(shiftedChunk.toUInt(), imm1Bits)
        shiftedChunk = shiftedChunk shr imm1Skip.toInt()
        val imm2 = signExtendAt(shiftedChunk.toUInt(), imm2Bits)
        return arrayOf(reg1, reg2, imm1, imm2)
    }

    /**
     * Reads two registers and immediate argument from chunk
     */
    fun readArgsRegs2Imm(chunk: ULong, skip: UInt): Triple<RawReg, RawReg, UInt> {
        val value = chunk.toUInt()
        val reg1 = RawReg(value)
        val reg2 = RawReg(value shr 4)
        val shiftedChunk = chunk shr 8
        val (_, _, immBits) = TABLE_1.get(skip, 0u)
        val imm = signExtendAt(shiftedChunk.toUInt(), immBits)
        return Triple(reg1, reg2, imm)
    }

    /**
     * Reads two registers and offset argument from chunk
     */
    fun readArgsRegs2Offset(
        chunk: ULong,
        instructionOffset: UInt,
        skip: UInt
    ): Triple<RawReg, RawReg, UInt> {
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip)
        return Triple(reg1, reg2, instructionOffset + imm)
    }

    /**
     * Reads three registers from chunk
     */
    fun readArgsRegs3(chunk: ULong): Triple<RawReg, RawReg, RawReg> {
        val value = chunk.toUInt()
        return Triple(
            RawReg(value shr 8),
            RawReg(value),
            RawReg(value shr 4)
        )
    }

    /**
     * Reads two registers from chunk
     */
    fun readArgsRegs2(chunk: ULong): Pair<RawReg, RawReg> {
        val value = chunk.toUInt()
        return Pair(RawReg(value), RawReg(value shr 4))
    }
}
