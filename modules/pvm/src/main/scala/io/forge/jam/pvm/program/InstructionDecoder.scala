package io.forge.jam.pvm.program

import spire.math.{UInt, ULong}
import io.forge.jam.pvm.{Instruction, Opcode}

/**
 * Decodes PVM instructions from bytecode.
 */
object InstructionDecoder:

  /**
   * Decode an instruction from code at the given offset.
   *
   * @param code The bytecode
   * @param bitmask The instruction boundary bitmask
   * @param offset Current offset in code
   * @return (instruction, skip) where skip is the number of bytes consumed
   */
  def decode(code: Array[Byte], bitmask: Array[Byte], offset: Int): (Instruction, Int) =
    if offset >= code.length then
      return (Instruction.Panic, 1)

    val opcodeByte = code(offset) & 0xFF

    // Get instruction length from bitmask
    val skip = getSkip(bitmask, code.length, offset)

    // Read chunk of following bytes (up to 16 bytes for immediate values)
    val chunk = readChunk(code, offset + 1, skip - 1)

    val instruction = Opcode.fromByte(opcodeByte) match
      // ========================================================================
      // Argless instructions
      // ========================================================================
      case Some(Opcode.Panic) => Instruction.Panic
      case Some(Opcode.Fallthrough) => Instruction.Fallthrough
      case Some(Opcode.Memset) => Instruction.Memset

      // ========================================================================
      // Ecalli (host call)
      // ========================================================================
      case Some(Opcode.Ecalli) =>
        val imm = readImm(chunk, skip - 1)
        Instruction.Ecalli(imm)

      // ========================================================================
      // Sbrk
      // ========================================================================
      case Some(Opcode.Sbrk) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.Sbrk(reg, imm.toInt)

      // ========================================================================
      // Jump instructions
      // ========================================================================
      case Some(Opcode.Jump) =>
        val imm = readArgsOffset(chunk, offset, skip - 1)
        Instruction.Jump(imm)

      case Some(Opcode.JumpIndirect) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.JumpIndirect(reg, imm)

      // ========================================================================
      // Load immediate
      // ========================================================================
      case Some(Opcode.LoadImm) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadImm(reg, imm)

      case Some(Opcode.LoadImm64) =>
        // LoadImm64 needs 9 bytes (1 reg + 8 imm) which doesn't fit in Long chunk
        // Read register directly from code byte, immediate from next 8 bytes
        val regByte = code(offset + 1) & 0xFF
        val reg = regByte % 13
        val imm = readLong(code, offset + 2)  // immediate starts at offset+2 (after opcode and reg byte)
        Instruction.LoadImm64(reg, imm)

      // ========================================================================
      // Direct load instructions
      // ========================================================================
      case Some(Opcode.LoadU8) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadU8(reg, imm)

      case Some(Opcode.LoadI8) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadI8(reg, imm)

      case Some(Opcode.LoadU16) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadU16(reg, imm)

      case Some(Opcode.LoadI16) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadI16(reg, imm)

      case Some(Opcode.LoadU32) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadU32(reg, imm)

      case Some(Opcode.LoadI32) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadI32(reg, imm)

      case Some(Opcode.LoadU64) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.LoadU64(reg, imm)

      // ========================================================================
      // Direct store instructions
      // ========================================================================
      case Some(Opcode.StoreU8) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.StoreU8(reg, imm)

      case Some(Opcode.StoreU16) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.StoreU16(reg, imm)

      case Some(Opcode.StoreU32) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.StoreU32(reg, imm)

      case Some(Opcode.StoreU64) =>
        val (reg, imm) = readArgsRegImm(chunk, skip - 1)
        Instruction.StoreU64(reg, imm)

      // ========================================================================
      // Store immediate instructions
      // ========================================================================
      case Some(Opcode.StoreImmU8) =>
        val (addr, value) = readArgsImmImm(chunk, skip - 1)
        Instruction.StoreImmU8(addr, value.toByte)

      case Some(Opcode.StoreImmU16) =>
        val (addr, value) = readArgsImmImm(chunk, skip - 1)
        Instruction.StoreImmU16(addr, value.toShort)

      case Some(Opcode.StoreImmU32) =>
        val (addr, value) = readArgsImmImm(chunk, skip - 1)
        Instruction.StoreImmU32(addr, value.toInt)

      case Some(Opcode.StoreImmU64) =>
        val (addr, value) = readArgsImmImm(chunk, skip - 1)
        Instruction.StoreImmU64(addr, value)

      // ========================================================================
      // Indirect store instructions (with base register)
      // ========================================================================
      case Some(Opcode.StoreIndirectU8) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.StoreIndirectU8(reg1, reg2, imm)

      case Some(Opcode.StoreIndirectU16) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.StoreIndirectU16(reg1, reg2, imm)

      case Some(Opcode.StoreIndirectU32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.StoreIndirectU32(reg1, reg2, imm)

      case Some(Opcode.StoreIndirectU64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.StoreIndirectU64(reg1, reg2, imm)

      // ========================================================================
      // Indirect load instructions (with base register)
      // ========================================================================
      case Some(Opcode.LoadIndirectU8) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.LoadIndirectU8(reg1, reg2, imm)

      case Some(Opcode.LoadIndirectI8) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.LoadIndirectI8(reg1, reg2, imm)

      case Some(Opcode.LoadIndirectU16) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.LoadIndirectU16(reg1, reg2, imm)

      case Some(Opcode.LoadIndirectI16) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.LoadIndirectI16(reg1, reg2, imm)

      case Some(Opcode.LoadIndirectU32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.LoadIndirectU32(reg1, reg2, imm)

      case Some(Opcode.LoadIndirectI32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.LoadIndirectI32(reg1, reg2, imm)

      case Some(Opcode.LoadIndirectU64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.LoadIndirectU64(reg1, reg2, imm)

      // ========================================================================
      // Store immediate indirect (address in register + offset)
      // ========================================================================
      case Some(Opcode.StoreImmIndirectU8) =>
        val (reg, imm1, imm2) = readArgsRegImmImm(chunk, skip - 1)
        Instruction.StoreImmIndirectU8(reg, imm1, imm2.toByte)

      case Some(Opcode.StoreImmIndirectU16) =>
        val (reg, imm1, imm2) = readArgsRegImmImm(chunk, skip - 1)
        Instruction.StoreImmIndirectU16(reg, imm1, imm2.toShort)

      case Some(Opcode.StoreImmIndirectU32) =>
        val (reg, imm1, imm2) = readArgsRegImmImm(chunk, skip - 1)
        Instruction.StoreImmIndirectU32(reg, imm1, imm2.toInt)

      case Some(Opcode.StoreImmIndirectU64) =>
        val (reg, imm1, imm2) = readArgsRegImmImm(chunk, skip - 1)
        Instruction.StoreImmIndirectU64(reg, imm1, imm2)

      // ========================================================================
      // Load immediate and jump
      // ========================================================================
      case Some(Opcode.LoadImmAndJump) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.LoadImmAndJump(reg, imm1, imm2)

      case Some(Opcode.LoadImmAndJumpIndirect) =>
        val (reg1, reg2, imm1, imm2) = readArgsRegs2Imm2(chunk, skip - 1)
        Instruction.LoadImmAndJumpIndirect(reg1, reg2, imm1, imm2)

      // ========================================================================
      // Branch with immediate comparison
      // ========================================================================
      case Some(Opcode.BranchEqImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchEqImm(reg, imm1, imm2)

      case Some(Opcode.BranchNotEqImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchNotEqImm(reg, imm1, imm2)

      case Some(Opcode.BranchLessUnsignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchLessUnsignedImm(reg, imm1, imm2)

      case Some(Opcode.BranchLessSignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchLessSignedImm(reg, imm1, imm2)

      case Some(Opcode.BranchGreaterOrEqualUnsignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchGreaterOrEqualUnsignedImm(reg, imm1, imm2)

      case Some(Opcode.BranchGreaterOrEqualSignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchGreaterOrEqualSignedImm(reg, imm1, imm2)

      case Some(Opcode.BranchLessOrEqualSignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchLessOrEqualSignedImm(reg, imm1, imm2)

      case Some(Opcode.BranchLessOrEqualUnsignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchLessOrEqualUnsignedImm(reg, imm1, imm2)

      case Some(Opcode.BranchGreaterUnsignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchGreaterUnsignedImm(reg, imm1, imm2)

      case Some(Opcode.BranchGreaterSignedImm) =>
        val (reg, imm1, imm2) = readArgsRegImmOffset(chunk, offset, skip - 1)
        Instruction.BranchGreaterSignedImm(reg, imm1, imm2)

      // ========================================================================
      // Two-register immediate arithmetic
      // ========================================================================
      case Some(Opcode.AddImm32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.AddImm32(reg1, reg2, imm)

      case Some(Opcode.AddImm64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.AddImm64(reg1, reg2, imm)

      case Some(Opcode.AndImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.AndImm(reg1, reg2, imm)

      case Some(Opcode.XorImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.XorImm(reg1, reg2, imm)

      case Some(Opcode.OrImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.OrImm(reg1, reg2, imm)

      case Some(Opcode.MulImm32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.MulImm32(reg1, reg2, imm)

      case Some(Opcode.MulImm64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.MulImm64(reg1, reg2, imm)

      case Some(Opcode.SetLessThanUnsignedImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.SetLessThanUnsignedImm(reg1, reg2, imm)

      case Some(Opcode.SetLessThanSignedImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.SetLessThanSignedImm(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalLeftImm32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalLeftImm32(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalLeftImm64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalLeftImm64(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalRightImm32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalRightImm32(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalRightImm64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalRightImm64(reg1, reg2, imm)

      case Some(Opcode.ShiftArithmeticRightImm32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftArithmeticRightImm32(reg1, reg2, imm)

      case Some(Opcode.ShiftArithmeticRightImm64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftArithmeticRightImm64(reg1, reg2, imm)

      case Some(Opcode.NegateAndAddImm32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.NegateAndAddImm32(reg1, reg2, imm)

      case Some(Opcode.NegateAndAddImm64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.NegateAndAddImm64(reg1, reg2, imm)

      case Some(Opcode.SetGreaterThanUnsignedImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.SetGreaterThanUnsignedImm(reg1, reg2, imm)

      case Some(Opcode.SetGreaterThanSignedImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.SetGreaterThanSignedImm(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalLeftImmAlt32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalLeftImmAlt32(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalLeftImmAlt64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalLeftImmAlt64(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalRightImmAlt32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalRightImmAlt32(reg1, reg2, imm)

      case Some(Opcode.ShiftLogicalRightImmAlt64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftLogicalRightImmAlt64(reg1, reg2, imm)

      case Some(Opcode.ShiftArithmeticRightImmAlt32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftArithmeticRightImmAlt32(reg1, reg2, imm)

      case Some(Opcode.ShiftArithmeticRightImmAlt64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.ShiftArithmeticRightImmAlt64(reg1, reg2, imm)

      case Some(Opcode.CmovIfZeroImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.CmovIfZeroImm(reg1, reg2, imm)

      case Some(Opcode.CmovIfNotZeroImm) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.CmovIfNotZeroImm(reg1, reg2, imm)

      case Some(Opcode.RotateRightImm32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.RotateRightImm32(reg1, reg2, imm)

      case Some(Opcode.RotateRightImmAlt32) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.RotateRightImmAlt32(reg1, reg2, imm)

      case Some(Opcode.RotateRightImm64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.RotateRightImm64(reg1, reg2, imm)

      case Some(Opcode.RotateRightImmAlt64) =>
        val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip - 1)
        Instruction.RotateRightImmAlt64(reg1, reg2, imm)

      // ========================================================================
      // Two-register branch instructions
      // ========================================================================
      case Some(Opcode.BranchEq) =>
        val (reg1, reg2, imm) = readArgsRegs2Offset(chunk, offset, skip - 1)
        Instruction.BranchEq(reg1, reg2, imm)

      case Some(Opcode.BranchNotEq) =>
        val (reg1, reg2, imm) = readArgsRegs2Offset(chunk, offset, skip - 1)
        Instruction.BranchNotEq(reg1, reg2, imm)

      case Some(Opcode.BranchLessUnsigned) =>
        val (reg1, reg2, imm) = readArgsRegs2Offset(chunk, offset, skip - 1)
        Instruction.BranchLessUnsigned(reg1, reg2, imm)

      case Some(Opcode.BranchLessSigned) =>
        val (reg1, reg2, imm) = readArgsRegs2Offset(chunk, offset, skip - 1)
        Instruction.BranchLessSigned(reg1, reg2, imm)

      case Some(Opcode.BranchGreaterOrEqualUnsigned) =>
        val (reg1, reg2, imm) = readArgsRegs2Offset(chunk, offset, skip - 1)
        Instruction.BranchGreaterOrEqualUnsigned(reg1, reg2, imm)

      case Some(Opcode.BranchGreaterOrEqualSigned) =>
        val (reg1, reg2, imm) = readArgsRegs2Offset(chunk, offset, skip - 1)
        Instruction.BranchGreaterOrEqualSigned(reg1, reg2, imm)

      // ========================================================================
      // Three-register arithmetic (32-bit)
      // ========================================================================
      case Some(Opcode.Add32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Add32(reg1, reg2, reg3)

      case Some(Opcode.Sub32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Sub32(reg1, reg2, reg3)

      case Some(Opcode.Mul32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Mul32(reg1, reg2, reg3)

      case Some(Opcode.DivUnsigned32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.DivUnsigned32(reg1, reg2, reg3)

      case Some(Opcode.DivSigned32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.DivSigned32(reg1, reg2, reg3)

      case Some(Opcode.RemUnsigned32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RemUnsigned32(reg1, reg2, reg3)

      case Some(Opcode.RemSigned32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RemSigned32(reg1, reg2, reg3)

      case Some(Opcode.ShiftLogicalLeft32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.ShiftLogicalLeft32(reg1, reg2, reg3)

      case Some(Opcode.ShiftLogicalRight32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.ShiftLogicalRight32(reg1, reg2, reg3)

      case Some(Opcode.ShiftArithmeticRight32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.ShiftArithmeticRight32(reg1, reg2, reg3)

      // ========================================================================
      // Three-register arithmetic (64-bit)
      // ========================================================================
      case Some(Opcode.Add64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Add64(reg1, reg2, reg3)

      case Some(Opcode.Sub64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Sub64(reg1, reg2, reg3)

      case Some(Opcode.Mul64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Mul64(reg1, reg2, reg3)

      case Some(Opcode.DivUnsigned64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.DivUnsigned64(reg1, reg2, reg3)

      case Some(Opcode.DivSigned64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.DivSigned64(reg1, reg2, reg3)

      case Some(Opcode.RemUnsigned64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RemUnsigned64(reg1, reg2, reg3)

      case Some(Opcode.RemSigned64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RemSigned64(reg1, reg2, reg3)

      case Some(Opcode.ShiftLogicalLeft64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.ShiftLogicalLeft64(reg1, reg2, reg3)

      case Some(Opcode.ShiftLogicalRight64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.ShiftLogicalRight64(reg1, reg2, reg3)

      case Some(Opcode.ShiftArithmeticRight64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.ShiftArithmeticRight64(reg1, reg2, reg3)

      // ========================================================================
      // Three-register bitwise
      // ========================================================================
      case Some(Opcode.And) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.And(reg1, reg2, reg3)

      case Some(Opcode.Xor) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Xor(reg1, reg2, reg3)

      case Some(Opcode.Or) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Or(reg1, reg2, reg3)

      // ========================================================================
      // Three-register multiplication (upper bits)
      // ========================================================================
      case Some(Opcode.MulUpperSignedSigned) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.MulUpperSignedSigned(reg1, reg2, reg3)

      case Some(Opcode.MulUpperUnsignedUnsigned) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.MulUpperUnsignedUnsigned(reg1, reg2, reg3)

      case Some(Opcode.MulUpperSignedUnsigned) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.MulUpperSignedUnsigned(reg1, reg2, reg3)

      // ========================================================================
      // Three-register comparison
      // ========================================================================
      case Some(Opcode.SetLessThanUnsigned) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.SetLessThanUnsigned(reg1, reg2, reg3)

      case Some(Opcode.SetLessThanSigned) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.SetLessThanSigned(reg1, reg2, reg3)

      // ========================================================================
      // Conditional move
      // ========================================================================
      case Some(Opcode.CmovIfZero) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.CmovIfZero(reg1, reg2, reg3)

      case Some(Opcode.CmovIfNotZero) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.CmovIfNotZero(reg1, reg2, reg3)

      // ========================================================================
      // Rotate
      // ========================================================================
      case Some(Opcode.RotateRight32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RotateRight32(reg1, reg2, reg3)

      case Some(Opcode.RotateRight64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RotateRight64(reg1, reg2, reg3)

      // ========================================================================
      // Two-register operations
      // ========================================================================
      case Some(Opcode.MoveReg) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.MoveReg(reg1, reg2)

      case Some(Opcode.CountSetBits32) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.CountSetBits32(reg1, reg2)

      case Some(Opcode.CountSetBits64) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.CountSetBits64(reg1, reg2)

      case Some(Opcode.CountLeadingZeroBits32) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.CountLeadingZeroBits32(reg1, reg2)

      case Some(Opcode.CountLeadingZeroBits64) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.CountLeadingZeroBits64(reg1, reg2)

      case Some(Opcode.CountTrailingZeroBits32) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.CountTrailingZeroBits32(reg1, reg2)

      case Some(Opcode.CountTrailingZeroBits64) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.CountTrailingZeroBits64(reg1, reg2)

      case Some(Opcode.SignExtend8) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.SignExtend8(reg1, reg2)

      case Some(Opcode.SignExtend16) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.SignExtend16(reg1, reg2)

      case Some(Opcode.ZeroExtend16) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.ZeroExtend16(reg1, reg2)

      case Some(Opcode.ReverseByte) =>
        val (reg1, reg2) = readArgsRegs2(chunk)
        Instruction.ReverseByte(reg1, reg2)

      // ========================================================================
      // Three-register bit manipulation (BB extension)
      // ========================================================================
      case Some(Opcode.AndInverted) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.AndInverted(reg1, reg2, reg3)

      case Some(Opcode.OrInverted) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.OrInverted(reg1, reg2, reg3)

      case Some(Opcode.Xnor) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Xnor(reg1, reg2, reg3)

      case Some(Opcode.Maximum) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Maximum(reg1, reg2, reg3)

      case Some(Opcode.MaximumUnsigned) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.MaximumUnsigned(reg1, reg2, reg3)

      case Some(Opcode.Minimum) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.Minimum(reg1, reg2, reg3)

      case Some(Opcode.MinimumUnsigned) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.MinimumUnsigned(reg1, reg2, reg3)

      case Some(Opcode.RotateLeft32) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RotateLeft32(reg1, reg2, reg3)

      case Some(Opcode.RotateLeft64) =>
        val (reg1, reg2, reg3) = readArgsRegs3(chunk)
        Instruction.RotateLeft64(reg1, reg2, reg3)

      // ========================================================================
      // Unknown opcode
      // ========================================================================
      case None => Instruction.Panic
      case _ => Instruction.Panic

    (instruction, skip)

  // ============================================================================
  // Helper functions for reading arguments
  // ============================================================================

  /**
   * Read chunk of bytes from code.
   */
  private def readChunk(code: Array[Byte], start: Int, length: Int): Long =
    var result = 0L
    var i = 0
    while i < length && start + i < code.length do
      result = result | ((code(start + i).toLong & 0xFF) << (i * 8))
      i += 1
    result

  /**
   * Read a 64-bit little-endian value from code.
   */
  private def readLong(code: Array[Byte], start: Int): Long =
    var result = 0L
    var i = 0
    while i < 8 && start + i < code.length do
      result = result | ((code(start + i).toLong & 0xFF) << (i * 8))
      i += 1
    result

  /**
   * Get instruction skip from bitmask.
   */
  private def getSkip(bitmask: Array[Byte], codeLength: Int, offset: Int): Int =
    var currentOffset = offset + 1
    var skip = 1
    while currentOffset < codeLength && skip < 24 do
      val byteIndex = currentOffset >> 3
      val bitIndex = currentOffset & 7
      if byteIndex < bitmask.length then
        val bit = (bitmask(byteIndex) >> bitIndex) & 1
        if bit == 1 then
          return skip
      currentOffset += 1
      skip += 1
    skip

  /**
   * Read a single immediate value.
   */
  private def readImm(chunk: Long, length: Int): Long =
    signExtend(chunk, length * 8)

  /**
   * Read an offset (adds instructionOffset to make absolute address).
   */
  private def readArgsOffset(chunk: Long, instructionOffset: Int, length: Int): Long =
    val imm = readImm(chunk, length)
    (instructionOffset + imm.toInt).toLong & 0xFFFFFFFFL

  /**
   * Read register + immediate.
   * Register is in byte 0 (lower 4 bits), immediate starts at byte 1.
   */
  private def readArgsRegImm(chunk: Long, length: Int): (Int, Long) =
    val reg = (chunk & 0xF).toInt
    // Immediate starts at bit 8 (byte 1), so shift by 8 bits
    val imm = signExtend(chunk >>> 8, (length * 8) - 8)
    (reg % 13, imm)

  /**
   * Read register + 64-bit immediate.
   * Register is in byte 0 (lower 4 bits), immediate starts at byte 1.
   */
  private def readArgsRegImm64(chunk: Long, length: Int): (Int, Long) =
    val reg = (chunk & 0xF).toInt
    // Immediate starts at bit 8 (byte 1)
    val imm = chunk >>> 8
    (reg % 13, imm)

  /**
   * Read two registers.
   * First register (dst) is in lower nibble, second (src) is in upper nibble.
   */
  private def readArgsRegs2(chunk: Long): (Int, Int) =
    val reg1 = (chunk & 0xF).toInt        // lower nibble = dst
    val reg2 = ((chunk >> 4) & 0xF).toInt // upper nibble = src
    (reg1 % 13, reg2 % 13)

  /**
   * Read two registers + immediate.
   * reg1 is the lower nibble, reg2 is the upper nibble (per Kotlin reference).
   */
  private def readArgsRegs2Imm(chunk: Long, length: Int): (Int, Int, Long) =
    val reg1 = (chunk & 0xF).toInt
    val reg2 = ((chunk >> 4) & 0xF).toInt
    val imm = signExtend(chunk >>> 8, (length * 8) - 8)
    (reg1 % 13, reg2 % 13, imm)

  /**
   * Read two registers + offset (for branch instructions).
   */
  private def readArgsRegs2Offset(chunk: Long, instructionOffset: Int, length: Int): (Int, Int, Long) =
    val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, length)
    (reg1, reg2, (instructionOffset + imm.toInt).toLong & 0xFFFFFFFFL)

  /**
   * Read three registers.
   */
  private def readArgsRegs3(chunk: Long): (Int, Int, Int) =
    val reg2 = (chunk & 0xF).toInt
    val reg3 = ((chunk >> 4) & 0xF).toInt
    val reg1 = ((chunk >> 8) & 0xF).toInt
    (reg1 % 13, reg2 % 13, reg3 % 13)

  /**
   * Read two immediates.
   * Uses TABLE_1 logic: aux from lower 3 bits of first byte determines split.
   */
  private def readArgsImmImm(chunk: Long, length: Int): (Long, Long) =
    val aux = (chunk & 0x7).toInt  // lower 3 bits of first byte

    // Calculate byte counts for each immediate (mimics LookupTable.build with offset=1)
    val imm1Bytes = math.min(4, aux)
    val imm2Bytes = math.max(0, math.min(4, length - imm1Bytes - 1))

    val imm1Bits = imm1Bytes * 8
    val imm2Bits = imm2Bytes * 8

    // Immediates start at bit 8 (after first byte containing aux)
    val shiftedChunk = chunk >>> 8
    val imm1 = signExtend(shiftedChunk, imm1Bits)
    val imm2 = signExtend(shiftedChunk >>> imm1Bits, imm2Bits)
    (imm1, imm2)

  /**
   * Read register + immediate + offset.
   * Uses lookup table logic: aux (bits 4-6) determines the split between imm1 and imm2.
   * - imm1 gets min(4, aux) bytes
   * - imm2 gets (skip - imm1Bytes - 1) bytes, clamped to 0-4
   */
  private def readArgsRegImmOffset(chunk: Long, instructionOffset: Int, length: Int): (Int, Long, Long) =
    val reg = (chunk & 0xF).toInt
    val aux = ((chunk >> 4) & 0x7).toInt

    // Calculate byte counts for each immediate (mimics LookupTable.build with offset=1)
    val imm1Bytes = math.min(4, aux)
    val imm2Bytes = math.max(0, math.min(4, length - imm1Bytes - 1))

    val imm1Bits = imm1Bytes * 8
    val imm2Bits = imm2Bytes * 8

    // imm1 starts at bit 8 (after reg + aux byte)
    val shiftedChunk = chunk >>> 8
    val imm1 = signExtend(shiftedChunk, imm1Bits)
    val imm2 = signExtend(shiftedChunk >>> imm1Bits, imm2Bits)

    val absoluteOffset = (instructionOffset + imm2.toInt).toLong & 0xFFFFFFFFL
    (reg % 13, imm1, absoluteOffset)

  /**
   * Read register + two immediates.
   * Uses TABLE_1 logic (offset=1): aux bits determine split.
   */
  private def readArgsRegImmImm(chunk: Long, length: Int): (Int, Long, Long) =
    val reg = (chunk & 0xF).toInt
    val aux = ((chunk >> 4) & 0x7).toInt

    // Calculate byte counts for each immediate (mimics LookupTable.build with offset=1)
    val imm1Bytes = math.min(4, aux)
    val imm2Bytes = math.max(0, math.min(4, length - imm1Bytes - 1))

    val imm1Bits = imm1Bytes * 8
    val imm2Bits = imm2Bytes * 8

    // Immediates start at bit 8 (after reg + aux byte)
    val shiftedChunk = chunk >>> 8
    val imm1 = signExtend(shiftedChunk, imm1Bits)
    val imm2 = signExtend(shiftedChunk >>> imm1Bits, imm2Bits)
    (reg % 13, imm1, imm2)

  /**
   * Read two registers + two immediates.
   * Uses TABLE_2 logic (offset=2): reg1 and reg2 take 1 byte total, aux bits determine split.
   */
  private def readArgsRegs2Imm2(chunk: Long, length: Int): (Int, Int, Long, Long) =
    val reg1 = (chunk & 0xF).toInt
    val reg2 = ((chunk >> 4) & 0xF).toInt
    val aux = ((chunk >> 8) & 0x7).toInt

    // Calculate byte counts for each immediate (mimics LookupTable.build with offset=2)
    val imm1Bytes = math.min(4, aux)
    val imm2Bytes = math.max(0, math.min(4, length - imm1Bytes - 2))

    val imm1Bits = imm1Bytes * 8
    val imm2Bits = imm2Bytes * 8

    // Immediates start at bit 16 (after 2 bytes for regs + aux)
    val shiftedChunk = chunk >>> 16
    val imm1 = signExtend(shiftedChunk, imm1Bits)
    val imm2 = signExtend(shiftedChunk >>> imm1Bits, imm2Bits)
    (reg1 % 13, reg2 % 13, imm1, imm2)

  /**
   * Sign-extend a value from the given number of bits.
   */
  private def signExtend(value: Long, bits: Int): Long =
    if bits <= 0 then 0L
    else if bits >= 64 then value
    else
      val signBit = 1L << (bits - 1)
      if (value & signBit) != 0 then
        value | (-1L << bits)
      else
        value & ((1L << bits) - 1)
