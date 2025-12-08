package io.forge.jam.pvm

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spire.math.{UInt, ULong}
import types.*

class WordOpsSpec extends AnyFlatSpec with Matchers:

  "WordOps[W32]" should "perform correct unsigned arithmetic" in {
    val ops = summon[WordOps[W32]]
    
    // Basic arithmetic
    ops.add(UInt(5), UInt(3)) shouldBe UInt(8)
    ops.sub(UInt(10), UInt(3)) shouldBe UInt(7)
    ops.mul(UInt(6), UInt(7)) shouldBe UInt(42)
    
    // Wrapping behavior
    ops.add(UInt(-1), UInt(1)) shouldBe UInt(0)  // Overflow wraps
    ops.sub(UInt(0), UInt(1)) shouldBe UInt(-1)   // Underflow wraps
  }
  
  it should "handle division edge cases correctly" in {
    val ops = summon[WordOps[W32]]
    
    // Division by zero returns max value (unsigned) or -1 (signed)
    ops.divu(UInt(10), UInt(0)) shouldBe UInt(-1)
    ops.divs(UInt(10), UInt(0)) shouldBe UInt(-1)
    
    // Remainder by zero returns dividend
    ops.remu(UInt(10), UInt(0)) shouldBe UInt(10)
    ops.rems(UInt(10), UInt(0)) shouldBe UInt(10)
    
    // Normal division
    ops.divu(UInt(10), UInt(3)) shouldBe UInt(3)
    ops.remu(UInt(10), UInt(3)) shouldBe UInt(1)
  }
  
  it should "perform correct bitwise operations" in {
    val ops = summon[WordOps[W32]]
    
    ops.and(UInt(0xFF00), UInt(0x0FF0)) shouldBe UInt(0x0F00)
    ops.or(UInt(0xFF00), UInt(0x00FF)) shouldBe UInt(0xFFFF)
    ops.xor(UInt(0xFFFF), UInt(0xFF00)) shouldBe UInt(0x00FF)
    ops.not(UInt(0)) shouldBe UInt(-1)
  }
  
  it should "perform correct shift operations" in {
    val ops = summon[WordOps[W32]]
    
    ops.shl(UInt(1), 4) shouldBe UInt(16)
    ops.shr(UInt(16), 2) shouldBe UInt(4)
    
    // Shift amount is masked to 31
    ops.shl(UInt(1), 33) shouldBe UInt(2)  // 33 & 31 = 1
  }
  
  it should "count bits correctly" in {
    val ops = summon[WordOps[W32]]
    
    ops.clz(UInt(1)) shouldBe 31
    ops.clz(UInt(0x80000000)) shouldBe 0
    ops.ctz(UInt(8)) shouldBe 3
    ops.popcnt(UInt(0xFF)) shouldBe 8
  }

  "WordOps[W64]" should "perform correct 64-bit arithmetic" in {
    val ops = summon[WordOps[W64]]
    
    ops.add(ULong(0x100000000L), ULong(1)) shouldBe ULong(0x100000001L)
    ops.mul(ULong(0x100000000L), ULong(2)) shouldBe ULong(0x200000000L)
  }
  
  it should "handle 64-bit edge cases" in {
    val ops = summon[WordOps[W64]]
    
    // Division by zero
    ops.divu(ULong(10), ULong(0)) shouldBe ULong(-1L)
    
    // Shift masking (63 bits)
    ops.shl(ULong(1), 65) shouldBe ULong(2)  // 65 & 63 = 1
  }

  "Register types" should "respect bounds" in {
    // Valid registers
    Reg[W64](0)
    Reg[W64](12)
    
    // Invalid registers should throw
    an[IllegalArgumentException] should be thrownBy {
      Reg[W64](13)
    }
    an[IllegalArgumentException] should be thrownBy {
      Reg[W64](-1)
    }
  }

  "RegImm" should "fold correctly" in {
    val regVal: RegImm[W64] = RegImm.reg(Reg[W64](5))
    val immVal: RegImm[W64] = RegImm.imm(-42L)
    
    regVal.fold(r => s"reg ${r.index}", v => s"imm $v") shouldBe "reg 5"
    immVal.fold(r => s"reg ${r.index}", v => s"imm $v") shouldBe "imm -42"
  }
