package io.forge.jam.pvm.program

import io.forge.jam.pvm.engine.RuntimeInstructionSet
import io.forge.jam.pvm.readSimpleVarint

/**
 * Static container for program-wide operations and lookup tables.
 */
class Program {
    companion object {
        private const val BITMASK_MAX: UInt = 24u

        fun isJumpTargetValid(
            instructionSet: RuntimeInstructionSet,
            code: ByteArray,
            bitmask: ByteArray,
            offset: UInt
        ): Boolean {
            if (!getBitForOffset(bitmask, code.size, offset)) {
                return false
            }

            if (offset == 0u) {
                return true
            }

            val skip = getPreviousInstructionSkip(bitmask, offset) ?: run {
                return false
            }

            val previousOffset = (offset - skip - 1u).toInt()
            val opcode = instructionSet.opcodeFromU8(code[previousOffset].toUByte()) ?: run {
                return false
            }

            return opcode.startsNewBasicBlock()
        }

        fun getPreviousInstructionSkip(bitmask: ByteArray, offset: UInt): UInt? {
            val shift = offset.toInt() and 7
            var mask = (bitmask[offset.toInt() shr 3].toUInt() and 0xFFu) shl 24

            // Build up the mask from previous bytes
            if (offset >= 8u) {
                mask = mask or ((bitmask[(offset.toInt() shr 3) - 1].toUInt() and 0xFFu) shl 16)
            }
            if (offset >= 16u) {
                mask = mask or ((bitmask[(offset.toInt() shr 3) - 2].toUInt() and 0xFFu) shl 8)
            }
            if (offset >= 24u) {
                mask = mask or (bitmask[(offset.toInt() shr 3) - 3].toUInt() and 0xFFu)
            }

            mask = mask shl (8 - shift)
            mask = mask shr 1

            val skip = (mask.countLeadingZeroBits() - 1).toUInt()

            return if (skip > BITMASK_MAX) null else skip
        }

        fun getBitForOffset(bitmask: ByteArray, codeLen: Int, offset: UInt): Boolean {
            val offsetInt = offset.toInt()
            val byteIndex = offsetInt shr 3

            if (byteIndex >= bitmask.size || offsetInt > codeLen) {
                return false
            }

            val shift = offsetInt and 7
            return ((bitmask[byteIndex].toInt() shr shift) and 1) == 1
        }

        fun findStartOfBasicBlock(
            instructionSet: RuntimeInstructionSet,
            code: ByteArray,
            bitmask: ByteArray,
            initialOffset: UInt
        ): UInt? {
            var offset = initialOffset
            if (!getBitForOffset(bitmask, code.size, offset)) {
                return null
            }

            if (offset == 0u) {
                return 0u
            }

            while (true) {
                val skip = getPreviousInstructionSkip(bitmask, offset) ?: return null
                val previousOffset = offset - skip - 1u
                val opcode = instructionSet.opcodeFromU8(
                    code[previousOffset.toInt()].toUByte()
                ) ?: Opcode.trap
                if (opcode.startsNewBasicBlock()) {
                    return offset
                }

                offset = previousOffset
                if (offset == 0u) {
                    return 0u
                }
            }
        }
    }


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
