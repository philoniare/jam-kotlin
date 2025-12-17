package io.forge.jam.pvm

/**
 * Byte encoding/decoding utilities for little-endian memory operations.
 */
object ByteOps:

  // ============================================================================
  // Little-Endian Decoding (bytes to value)
  // ============================================================================

  /**
   * Converts 2 little-endian bytes to an unsigned 16-bit value.
   */
  inline def bytesToU16LE(b0: Byte, b1: Byte): Int =
    (b0 & 0xFF) | ((b1 & 0xFF) << 8)

  /**
   * Converts 4 little-endian bytes to an unsigned 32-bit value.
   */
  inline def bytesToU32LE(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
    (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24)

  /**
   * Converts a byte array to an unsigned 32-bit value (little-endian).
   */
  def bytesToU32LE(bytes: Array[Byte]): Int =
    require(bytes.length >= 4, "Need at least 4 bytes")
    bytesToU32LE(bytes(0), bytes(1), bytes(2), bytes(3))

  /**
   * Converts a byte array to an unsigned 64-bit value (little-endian).
   */
  def bytesToU64LE(bytes: Array[Byte]): Long =
    require(bytes.length >= 8, "Need at least 8 bytes")
    var value = 0L
    var i = 0
    while i < 8 do
      value |= (bytes(i) & 0xFFL) << (i * 8)
      i += 1
    value

  /**
   * Reads an unsigned 64-bit value from bytes using a byte-at-a-time function.
   */
  inline def readU64LE(readByte: Int => Byte): Long =
    var value = 0L
    var i = 0
    while i < 8 do
      value |= (readByte(i) & 0xFFL) << (i * 8)
      i += 1
    value

  /**
   * Reads an unsigned 32-bit value from bytes using a byte-at-a-time function.
   */
  inline def readU32LE(readByte: Int => Byte): Int =
    var value = 0
    var i = 0
    while i < 4 do
      value |= (readByte(i) & 0xFF) << (i * 8)
      i += 1
    value

  /**
   * Reads an unsigned 16-bit value from bytes using a byte-at-a-time function.
   */
  inline def readU16LE(readByte: Int => Byte): Int =
    (readByte(0) & 0xFF) | ((readByte(1) & 0xFF) << 8)

  // ============================================================================
  // Little-Endian Encoding (value to bytes)
  // ============================================================================

  /**
   * Converts a 32-bit value to 4 little-endian bytes.
   */
  def u32ToBytesLE(value: Int): Array[Byte] =
    Array(
      (value & 0xFF).toByte,
      ((value >> 8) & 0xFF).toByte,
      ((value >> 16) & 0xFF).toByte,
      ((value >> 24) & 0xFF).toByte
    )

  /**
   * Converts a 64-bit value to 8 little-endian bytes.
   */
  def u64ToBytesLE(value: Long): Array[Byte] =
    val bytes = new Array[Byte](8)
    var i = 0
    while i < 8 do
      bytes(i) = ((value >> (i * 8)) & 0xFF).toByte
      i += 1
    bytes

  /**
   * Writes a 64-bit value as little-endian bytes using a byte-at-a-time function.
   */
  inline def writeU64LE(value: Long, writeByte: (Int, Byte) => Unit): Unit =
    var i = 0
    while i < 8 do
      writeByte(i, ((value >> (i * 8)) & 0xFF).toByte)
      i += 1

  /**
   * Writes a 32-bit value as little-endian bytes using a byte-at-a-time function.
   */
  inline def writeU32LE(value: Int, writeByte: (Int, Byte) => Unit): Unit =
    var i = 0
    while i < 4 do
      writeByte(i, ((value >> (i * 8)) & 0xFF).toByte)
      i += 1

  /**
   * Writes a 16-bit value as little-endian bytes using a byte-at-a-time function.
   */
  inline def writeU16LE(value: Int, writeByte: (Int, Byte) => Unit): Unit =
    writeByte(0, (value & 0xFF).toByte)
    writeByte(1, ((value >> 8) & 0xFF).toByte)
