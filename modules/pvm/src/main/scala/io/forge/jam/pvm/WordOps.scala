package io.forge.jam.pvm

import spire.math.{UInt, ULong}
import types.*

/**
 * Type class for width-polymorphic word operations.
 * 
 * With WordOps[W], we write ONE handler that works for both widths:
 * {{{
 *   def add[W <: Width: WordOps](d: Reg[W], s1: RegImm[W], s2: RegImm[W]): Handler = ...
 * }}}
 */
trait WordOps[W <: Width]:
  /** The underlying word type (UInt for W32, ULong for W64) */
  type Word
  
  // ══════════════════════════════════════════════════════════════════════════
  // Arithmetic Operations (wrapping semantics)
  // ══════════════════════════════════════════════════════════════════════════
  
  def add(a: Word, b: Word): Word
  def sub(a: Word, b: Word): Word
  def mul(a: Word, b: Word): Word
  
  /** Unsigned division. Returns max value if divisor is 0 */
  def divu(a: Word, b: Word): Word
  
  /** Signed division. Returns -1 if divisor is 0, handles MIN_VALUE / -1 */
  def divs(a: Word, b: Word): Word
  
  /** Unsigned remainder. Returns dividend if divisor is 0 */
  def remu(a: Word, b: Word): Word
  
  /** Signed remainder. Returns dividend if divisor is 0 */
  def rems(a: Word, b: Word): Word
  
  // ══════════════════════════════════════════════════════════════════════════
  // Bitwise Operations
  // ══════════════════════════════════════════════════════════════════════════
  
  def and(a: Word, b: Word): Word
  def or(a: Word, b: Word): Word
  def xor(a: Word, b: Word): Word
  def not(a: Word): Word
  
  def andNot(a: Word, b: Word): Word  // a & ~b
  def orNot(a: Word, b: Word): Word   // a | ~b
  def xnor(a: Word, b: Word): Word    // ~(a ^ b)
  
  // ══════════════════════════════════════════════════════════════════════════
  // Shift and Rotate Operations
  // ══════════════════════════════════════════════════════════════════════════
  
  /** Logical shift left */
  def shl(a: Word, shift: Int): Word
  
  /** Logical shift right (zero-fill) */
  def shr(a: Word, shift: Int): Word
  
  /** Arithmetic shift right (sign-extend) */
  def sar(a: Word, shift: Int): Word
  
  /** Rotate left */
  def rotl(a: Word, shift: Int): Word
  
  /** Rotate right */
  def rotr(a: Word, shift: Int): Word
  
  // ══════════════════════════════════════════════════════════════════════════
  // Bit Counting Operations
  // ══════════════════════════════════════════════════════════════════════════
  
  /** Count leading zero bits */
  def clz(a: Word): Int
  
  /** Count trailing zero bits */
  def ctz(a: Word): Int
  
  /** Count set bits (population count) */
  def popcnt(a: Word): Int
  
  // ══════════════════════════════════════════════════════════════════════════
  // Comparison Operations (return 1 or 0)
  // ══════════════════════════════════════════════════════════════════════════
  
  def ltU(a: Word, b: Word): Word  // Unsigned less than
  def ltS(a: Word, b: Word): Word  // Signed less than
  def gtU(a: Word, b: Word): Word  // Unsigned greater than
  def gtS(a: Word, b: Word): Word  // Signed greater than
  
  // ══════════════════════════════════════════════════════════════════════════
  // Min/Max Operations
  // ══════════════════════════════════════════════════════════════════════════
  
  def minU(a: Word, b: Word): Word  // Unsigned min
  def minS(a: Word, b: Word): Word  // Signed min
  def maxU(a: Word, b: Word): Word  // Unsigned max
  def maxS(a: Word, b: Word): Word  // Signed max
  
  // ══════════════════════════════════════════════════════════════════════════
  // Conversions
  // ══════════════════════════════════════════════════════════════════════════
  
  /** Convert from 64-bit (truncating for 32-bit) */
  def fromLong(v: Long): Word
  
  /** Convert from ULong (truncating for 32-bit) */
  def fromULong(v: ULong): Word
  
  /** Convert to signed Long (sign-extended for 32-bit) */
  def toSignedLong(w: Word): Long
  
  /** Convert to ULong (zero-extended for 32-bit) */
  def toULong(w: Word): ULong
  
  /** Byte count for this width */
  def byteCount: Int
  
  /** Shift mask (31 for 32-bit, 63 for 64-bit) */
  def shiftMask: Int
  
  /** Zero value */
  def zero: Word
  
  /** One value */
  def one: Word
  
  /** All bits set (max unsigned value) */
  def allOnes: Word

// ══════════════════════════════════════════════════════════════════════════════
// 32-bit Implementation
// ══════════════════════════════════════════════════════════════════════════════

given WordOps[W32] with
  type Word = UInt
  
  // Arithmetic
  def add(a: UInt, b: UInt): UInt = a + b
  def sub(a: UInt, b: UInt): UInt = a - b
  def mul(a: UInt, b: UInt): UInt = a * b
  
  def divu(a: UInt, b: UInt): UInt =
    if b == UInt(0) then UInt(-1) else a / b
  
  def divs(a: UInt, b: UInt): UInt =
    val as = a.signed
    val bs = b.signed
    if bs == 0 then UInt(-1)
    else if as == Int.MinValue && bs == -1 then a
    else UInt(as / bs)
  
  def remu(a: UInt, b: UInt): UInt =
    if b == UInt(0) then a else a % b
  
  def rems(a: UInt, b: UInt): UInt =
    val as = a.signed
    val bs = b.signed
    if bs == 0 then a
    else if as == Int.MinValue && bs == -1 then UInt(0)
    else UInt(as % bs)
  
  // Bitwise
  def and(a: UInt, b: UInt): UInt = a & b
  def or(a: UInt, b: UInt): UInt = a | b
  def xor(a: UInt, b: UInt): UInt = a ^ b
  def not(a: UInt): UInt = ~a
  def andNot(a: UInt, b: UInt): UInt = a & (~b)
  def orNot(a: UInt, b: UInt): UInt = a | (~b)
  def xnor(a: UInt, b: UInt): UInt = ~(a ^ b)
  
  // Shifts and rotates
  def shl(a: UInt, s: Int): UInt = a << (s & 31)
  def shr(a: UInt, s: Int): UInt = a >>> (s & 31)
  def sar(a: UInt, s: Int): UInt = UInt(a.signed >> (s & 31))
  def rotl(a: UInt, s: Int): UInt = 
    val shift = s & 31
    (a << shift) | (a >>> (32 - shift))
  def rotr(a: UInt, s: Int): UInt = 
    val shift = s & 31
    (a >>> shift) | (a << (32 - shift))
  
  // Bit counting
  def clz(a: UInt): Int = Integer.numberOfLeadingZeros(a.signed)
  def ctz(a: UInt): Int = Integer.numberOfTrailingZeros(a.signed)
  def popcnt(a: UInt): Int = Integer.bitCount(a.signed)
  
  // Comparisons
  def ltU(a: UInt, b: UInt): UInt = if a < b then UInt(1) else UInt(0)
  def ltS(a: UInt, b: UInt): UInt = if a.signed < b.signed then UInt(1) else UInt(0)
  def gtU(a: UInt, b: UInt): UInt = if a > b then UInt(1) else UInt(0)
  def gtS(a: UInt, b: UInt): UInt = if a.signed > b.signed then UInt(1) else UInt(0)
  
  // Min/Max
  def minU(a: UInt, b: UInt): UInt = if a < b then a else b
  def minS(a: UInt, b: UInt): UInt = if a.signed < b.signed then a else b
  def maxU(a: UInt, b: UInt): UInt = if a > b then a else b
  def maxS(a: UInt, b: UInt): UInt = if a.signed > b.signed then a else b
  
  // Conversions
  def fromLong(v: Long): UInt = UInt(v.toInt)
  def fromULong(v: ULong): UInt = UInt(v.toInt)
  def toSignedLong(w: UInt): Long = w.signed.toLong
  def toULong(w: UInt): ULong = ULong(w.toLong)
  
  def byteCount: Int = 4
  def shiftMask: Int = 31
  def zero: UInt = UInt(0)
  def one: UInt = UInt(1)
  def allOnes: UInt = UInt(-1)

// ══════════════════════════════════════════════════════════════════════════════
// 64-bit Implementation
// ══════════════════════════════════════════════════════════════════════════════

given WordOps[W64] with
  type Word = ULong
  
  // Arithmetic
  def add(a: ULong, b: ULong): ULong = a + b
  def sub(a: ULong, b: ULong): ULong = a - b
  def mul(a: ULong, b: ULong): ULong = a * b
  
  def divu(a: ULong, b: ULong): ULong =
    if b == ULong(0) then ULong(-1L) else a / b
  
  def divs(a: ULong, b: ULong): ULong =
    val as = a.signed
    val bs = b.signed
    if bs == 0L then ULong(-1L)
    else if as == Long.MinValue && bs == -1L then a
    else ULong(as / bs)
  
  def remu(a: ULong, b: ULong): ULong =
    if b == ULong(0) then a else a % b
  
  def rems(a: ULong, b: ULong): ULong =
    val as = a.signed
    val bs = b.signed
    if bs == 0L then a
    else if as == Long.MinValue && bs == -1L then ULong(0)
    else ULong(as % bs)
  
  // Bitwise
  def and(a: ULong, b: ULong): ULong = a & b
  def or(a: ULong, b: ULong): ULong = a | b
  def xor(a: ULong, b: ULong): ULong = a ^ b
  def not(a: ULong): ULong = ~a
  def andNot(a: ULong, b: ULong): ULong = a & (~b)
  def orNot(a: ULong, b: ULong): ULong = a | (~b)
  def xnor(a: ULong, b: ULong): ULong = ~(a ^ b)
  
  // Shifts and rotates
  def shl(a: ULong, s: Int): ULong = a << (s & 63)
  def shr(a: ULong, s: Int): ULong = a >>> (s & 63)
  def sar(a: ULong, s: Int): ULong = ULong(a.signed >> (s & 63))
  def rotl(a: ULong, s: Int): ULong = 
    val shift = s & 63
    (a << shift) | (a >>> (64 - shift))
  def rotr(a: ULong, s: Int): ULong = 
    val shift = s & 63
    (a >>> shift) | (a << (64 - shift))
  
  // Bit counting
  def clz(a: ULong): Int = java.lang.Long.numberOfLeadingZeros(a.signed)
  def ctz(a: ULong): Int = java.lang.Long.numberOfTrailingZeros(a.signed)
  def popcnt(a: ULong): Int = java.lang.Long.bitCount(a.signed)
  
  // Comparisons
  def ltU(a: ULong, b: ULong): ULong = if a < b then ULong(1) else ULong(0)
  def ltS(a: ULong, b: ULong): ULong = if a.signed < b.signed then ULong(1) else ULong(0)
  def gtU(a: ULong, b: ULong): ULong = if a > b then ULong(1) else ULong(0)
  def gtS(a: ULong, b: ULong): ULong = if a.signed > b.signed then ULong(1) else ULong(0)
  
  // Min/Max
  def minU(a: ULong, b: ULong): ULong = if a < b then a else b
  def minS(a: ULong, b: ULong): ULong = if a.signed < b.signed then a else b
  def maxU(a: ULong, b: ULong): ULong = if a > b then a else b
  def maxS(a: ULong, b: ULong): ULong = if a.signed > b.signed then a else b
  
  // Conversions
  def fromLong(v: Long): ULong = ULong(v)
  def fromULong(v: ULong): ULong = v
  def toSignedLong(w: ULong): Long = w.signed
  def toULong(w: ULong): ULong = w
  
  def byteCount: Int = 8
  def shiftMask: Int = 63
  def zero: ULong = ULong(0)
  def one: ULong = ULong(1)
  def allOnes: ULong = ULong(-1L)

// ══════════════════════════════════════════════════════════════════════════════
// Convenience object for accessing type class instances
// ══════════════════════════════════════════════════════════════════════════════

object WordOps:
  def apply[W <: Width](using ops: WordOps[W]): WordOps[W] = ops
  
  /** Get the 32-bit instance */
  def ops32: WordOps[W32] = summon[WordOps[W32]]
  
  /** Get the 64-bit instance */
  def ops64: WordOps[W64] = summon[WordOps[W64]]
