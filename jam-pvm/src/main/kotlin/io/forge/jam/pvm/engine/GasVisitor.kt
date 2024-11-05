package io.forge.jam.pvm.engine

import io.forge.jam.pvm.program.RawReg

class GasVisitor {
    private var cost: UInt = 0u
    private var lastBlockCost: UInt? = null

    fun startNewBasicBlock() {
        lastBlockCost = cost
        cost = 0u
    }

    fun takeBlockCost(): UInt? = lastBlockCost.also { lastBlockCost = null }

    fun invalid() {
        trap()
    }

    fun trap() {
        cost += 1u
        startNewBasicBlock()
    }

    fun fallthrough() {
        cost += 1u
        startNewBasicBlock()
    }

    fun sbrk(_d: RawReg, _s: RawReg) {
        cost += 1u
    }

    fun ecalli(_imm: UInt) {
        cost += 1u
    }

    fun setLessThanUnsigned(_d: RawReg, _s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun setLessThanSigned(_d: RawReg, _s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun shiftLogicalRight32(_d: RawReg, _s: RawReg, _imm: UInt) {
        cost += 1u
    }

    fun shiftArithmeticRight32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun shiftLogicalLeft32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun shiftLogicalRight64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun shiftArithmeticRight64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun shiftLogicalLeft64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun xor(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun and(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun or(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun add32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun add64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun sub32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun sub64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun mul32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun mul64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun mulUpperSignedSigned(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun mulUpperUnsignedUnsigned(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun mulUpperSignedUnsigned(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun divUnsigned32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun divSigned32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun remUnsigned32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun remSigned32(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun divUnsigned64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun divSigned64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun remUnsigned64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun remSigned64(d: RawReg, s1: RawReg, s2: RawReg) {
        cost += 1u
    }

    fun mulImm32(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun mulImm64(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun setLessThanUnsignedImm(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun setLessThanSignedImm(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun setGreaterThanUnsignedImm(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun setGreaterThanSignedImm(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun shiftLogicalRightImm32(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun shiftArithmeticRightImm32(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun shiftLogicalLeftImm32(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun shiftLogicalRightImmAlt32(d: RawReg, s2: RawReg, s1: UInt) {
        cost += 1u
    }

    fun shiftArithmeticRightImmAlt32(d: RawReg, s2: RawReg, s1: UInt) {
        cost += 1u
    }

    fun shiftLogicalLeftImmAlt32(d: RawReg, s2: RawReg, s1: UInt) {
        cost += 1u
    }

    fun shiftLogicalRightImm64(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun shiftArithmeticRightImm64(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun shiftLogicalLeftImm64(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun shiftLogicalRightImmAlt64(d: RawReg, s2: RawReg, s1: UInt) {
        cost += 1u
    }

    fun shiftArithmeticRightImmAlt64(d: RawReg, s2: RawReg, s1: UInt) {
        cost += 1u
    }

    fun shiftLogicalLeftImmAlt64(d: RawReg, s2: RawReg, s1: UInt) {
        cost += 1u
    }

    fun orImm(d: RawReg, s: RawReg, imm: UInt) {
        cost += 1u
    }

    fun andImm(d: RawReg, s: RawReg, imm: UInt) {
        cost += 1u
    }

    fun xorImm(d: RawReg, s: RawReg, imm: UInt) {
        cost += 1u
    }

    fun moveReg(d: RawReg, s: RawReg) {
        cost += 1u
    }

    fun cmovIfZero(d: RawReg, s: RawReg, c: RawReg) {
        cost += 1u
    }

    fun cmovIfNotZero(d: RawReg, s: RawReg, c: RawReg) {
        cost += 1u
    }

    fun cmovIfZeroImm(d: RawReg, c: RawReg, s: UInt) {
        cost += 1u
    }

    fun cmovIfNotZeroImm(d: RawReg, c: RawReg, s: UInt) {
        cost += 1u
    }

    fun addImm32(d: RawReg, s: RawReg, imm: UInt) {
        cost += 1u
    }

    fun addImm64(d: RawReg, s: RawReg, imm: UInt) {
        cost += 1u
    }

    fun negateAndAddImm32(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun negateAndAddImm64(d: RawReg, s1: RawReg, s2: UInt) {
        cost += 1u
    }

    fun storeImmIndirectU8(base: RawReg, offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeImmIndirectU16(base: RawReg, offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeImmIndirectU32(base: RawReg, offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeImmIndirectU64(base: RawReg, offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeIndirectU8(src: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun storeIndirectU16(src: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun storeIndirectU32(src: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun storeIndirectU64(src: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun storeImmU8(offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeImmU16(offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeImmU32(offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeImmU64(offset: UInt, value: UInt) {
        cost += 1u
    }

    fun storeU8(src: RawReg, offset: UInt) {
        cost += 1u
    }

    fun storeU16(src: RawReg, offset: UInt) {
        cost += 1u
    }

    fun storeU32(src: RawReg, offset: UInt) {
        cost += 1u
    }

    fun storeU64(src: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadIndirectU8(dst: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadIndirectI8(dst: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadIndirectU16(dst: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadIndirectI16(dst: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadIndirectU32(dst: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadIndirectI32(dst: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadIndirectU64(dst: RawReg, base: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadU8(dst: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadI8(dst: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadU16(dst: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadI16(dst: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadU32(dst: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadI32(dst: RawReg, offset: UInt) {
        cost += 1u
    }

    fun loadU64(dst: RawReg, offset: UInt) {
        cost += 1u
    }

    fun branchLessUnsigned(s1: RawReg, s2: RawReg, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchLessSigned(s1: RawReg, s2: RawReg, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchGreaterOrEqualUnsigned(s1: RawReg, s2: RawReg, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchGreaterOrEqualSigned(s1: RawReg, s2: RawReg, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchEq(s1: RawReg, s2: RawReg, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchNotEq(s1: RawReg, s2: RawReg, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchEqImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchNotEqImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchLessUnsignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchLessSignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchGreaterOrEqualUnsignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchGreaterOrEqualSignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchLessOrEqualUnsignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchLessOrEqualSignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchGreaterUnsignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun branchGreaterSignedImm(s1: RawReg, s2: UInt, imm: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun loadImm(dst: RawReg, value: UInt) {
        cost += 1u
    }

    fun loadImm64(dst: RawReg, value: ULong) {
        cost += 1u
    }

    fun loadImmAndJump(ra: RawReg, value: UInt, target: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun loadImmAndJumpIndirect(ra: RawReg, base: RawReg, value: UInt, offset: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun jump(target: UInt) {
        cost += 1u
        startNewBasicBlock()
    }

    fun jumpIndirect(base: RawReg, offset: UInt) {
        cost += 1u
        startNewBasicBlock()
    }
}
