package io.forge.jam.pvm.engine

/**
 * Optimized 128-bit multiplication using primitive arithmetic.
 * Avoids BigInt allocations for the upper-word multiplication operations.
 */
object UInt128:

  /**
   * Compute the upper 64 bits of unsigned 64-bit × 64-bit multiplication.
   * Uses the classic schoolbook multiplication approach.
   */
  def mulUpperUnsigned(a: Long, b: Long): Long =
    val aLow = a & 0xFFFFFFFFL
    val aHigh = a >>> 32
    val bLow = b & 0xFFFFFFFFL
    val bHigh = b >>> 32

    // low * low (contributes to lower 64 bits, but carry goes up)
    val lowLow = aLow * bLow
    // high * high (contributes to upper 64 bits directly)
    val highHigh = aHigh * bHigh
    // cross terms (split between lower and upper)
    val lowHigh = aLow * bHigh
    val highLow = aHigh * bLow

    // lowLow's upper 32 bits
    val lowLowHigh = lowLow >>> 32

    // Middle sum: lowLowHigh + (lowHigh & mask) + (highLow & mask)
    // This can overflow into bit 64
    val mid = lowLowHigh + (lowHigh & 0xFFFFFFFFL) + (highLow & 0xFFFFFFFFL)
    val midCarry = mid >>> 32

    // Upper result
    highHigh + (lowHigh >>> 32) + (highLow >>> 32) + midCarry

  /**
   * Compute the upper 64 bits of signed 64-bit × signed 64-bit multiplication.
   */
  def mulUpperSignedSigned(a: Long, b: Long): Long =
    var result = mulUpperUnsigned(a, b)
    if a < 0 then result -= b
    if b < 0 then result -= a
    result

  /**
   * Compute the upper 64 bits of signed × unsigned multiplication.
   */
  def mulUpperSignedUnsigned(a: Long, b: Long): Long =
    var result = mulUpperUnsigned(a, b)
    if a < 0 then result -= b
    result
