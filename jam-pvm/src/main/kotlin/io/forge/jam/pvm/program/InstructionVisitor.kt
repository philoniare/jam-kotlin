package io.forge.jam.pvm.program

/**
 * Visitor interface for processing different types of VM instructions.
 * Each method represents a different instruction type and returns a generic result.
 *
 * @param R The return type for all visitor methods
 */
interface InstructionVisitor<R> {
    // Two register with immediate operations
    fun mulUpperUnsignedUnsignedImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun mulUpperUnsignedUnsignedImm64(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setLessThanUnsignedImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setLessThanUnsigned64Imm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setLessThanSignedImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setLessThanSigned64Imm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalLeftImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalLeft64Imm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalRightImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalRight64Imm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftArithmeticRightImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftArithmeticRight64Imm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun negateAndAddImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setGreaterThanUnsignedImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setGreaterThanUnsigned64Imm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setGreaterThanSignedImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun setGreaterThanSigned64Imm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalRightImmAlt(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalRight64ImmAlt(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftArithmeticRightImmAlt(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftArithmeticRight64ImmAlt(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalLeftImmAlt(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun shiftLogicalLeft64ImmAlt(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun cmovIfZeroImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun cmovIfNotZeroImm(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun branchEq(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun branchNotEq(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun branchLessUnsigned(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun branchLessSigned(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun branchGreaterOrEqualUnsigned(reg1: RawReg, reg2: RawReg, imm: UInt): R
    fun branchGreaterOrEqualSigned(reg1: RawReg, reg2: RawReg, imm: UInt): R

    // Three register operations
    fun add(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun add64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun sub(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun sub64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun and(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun and64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun xor(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun xor64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun or(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun or64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mul(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mul64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mulUpperSignedSigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mulUpperSignedSigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mulUpperUnsignedUnsigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mulUpperUnsignedUnsigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mulUpperSignedUnsigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun mulUpperSignedUnsigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun setLessThanUnsigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun setLessThanUnsigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun setLessThanSigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun setLessThanSigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun shiftLogicalLeft(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun shiftLogicalLeft64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun shiftLogicalRight(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun shiftLogicalRight64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun shiftArithmeticRight(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun shiftArithmeticRight64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun divUnsigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun divUnsigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun divSigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun divSigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun remUnsigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun remUnsigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun remSigned(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun remSigned64(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun cmovIfZero(reg1: RawReg, reg2: RawReg, reg3: RawReg): R
    fun cmovIfNotZero(reg1: RawReg, reg2: RawReg, reg3: RawReg): R

    // Single immediate operations
    fun jump(imm: UInt): R
    fun ecalli(imm: UInt): R

    // Two immediate operations
    fun storeImmU8(imm1: UInt, imm2: UInt): R
    fun storeImmU16(imm1: UInt, imm2: UInt): R
    fun storeImmU32(imm1: UInt, imm2: UInt): R
    fun storeImmU64(imm1: UInt, imm2: UInt): R

    // Two register operations
    fun moveReg(reg1: RawReg, reg2: RawReg): R
    fun sbrk(reg1: RawReg, reg2: RawReg): R

    // Two register with two immediate operations
    fun loadImmAndJumpIndirect(reg1: RawReg, reg2: RawReg, imm1: UInt, imm2: UInt): R

    // Invalid instruction
    fun invalid(): R
}
