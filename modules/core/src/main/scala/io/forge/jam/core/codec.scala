package io.forge.jam.core

import spire.math.{UByte, UShort, UInt, ULong}

/**
 * Type class based JAM codec system.
 */
object codec:

  // ============================================================================
  // Type Class Traits
  // ============================================================================

  /**
   * Type class for encoding values to JAM binary format.
   */
  trait JamEncoder[A]:
    def encode(a: A): JamBytes

  /**
   * Type class for decoding values from JAM binary format.
   * Returns the decoded value and number of bytes consumed.
   */
  trait JamDecoder[A]:
    def decode(bytes: JamBytes, offset: Int): (A, Int)

  // ============================================================================
  // Extension Methods
  // ============================================================================

  extension [A](a: A)
    /**
     * Encode this value to JAM binary format using the implicit encoder.
     */
    def encode(using enc: JamEncoder[A]): JamBytes = enc.encode(a)

  extension (bytes: JamBytes)
    /**
     * Decode a value from JAM binary format using the implicit decoder.
     */
    def decodeAs[A](offset: Int = 0)(using dec: JamDecoder[A]): (A, Int) =
      dec.decode(bytes, offset)

  // ============================================================================
  // Primitive Type Encoders
  // ============================================================================

  given JamEncoder[UByte] with
    def encode(a: UByte): JamBytes =
      JamBytes(encoding.encodeU8(a))

  given JamEncoder[UShort] with
    def encode(a: UShort): JamBytes =
      JamBytes(encoding.encodeU16LE(a))

  given JamEncoder[UInt] with
    def encode(a: UInt): JamBytes =
      JamBytes(encoding.encodeU32LE(a))

  given JamEncoder[ULong] with
    def encode(a: ULong): JamBytes =
      JamBytes(encoding.encodeU64LE(a))

  /**
   * JamBytes encoder with compact length prefix followed by raw bytes.
   */
  given JamEncoder[JamBytes] with
    def encode(a: JamBytes): JamBytes =
      val lengthPrefix = JamBytes(encoding.encodeCompactInteger(a.length.toLong))
      lengthPrefix ++ a

  // ============================================================================
  // Primitive Type Decoders
  // ============================================================================

  given JamDecoder[UByte] with
    def decode(bytes: JamBytes, offset: Int): (UByte, Int) =
      (encoding.decodeU8(bytes.toArray, offset), 1)

  given JamDecoder[UShort] with
    def decode(bytes: JamBytes, offset: Int): (UShort, Int) =
      (encoding.decodeU16LE(bytes.toArray, offset), 2)

  given JamDecoder[UInt] with
    def decode(bytes: JamBytes, offset: Int): (UInt, Int) =
      (encoding.decodeU32LE(bytes.toArray, offset), 4)

  given JamDecoder[ULong] with
    def decode(bytes: JamBytes, offset: Int): (ULong, Int) =
      (encoding.decodeU64LE(bytes.toArray, offset), 8)

  /**
   * JamBytes decoder: reads compact length prefix, then that many bytes.
   */
  given JamDecoder[JamBytes] with
    def decode(bytes: JamBytes, offset: Int): (JamBytes, Int) =
      val arr = bytes.toArray
      val (length, lengthBytes) = encoding.decodeCompactInteger(arr, offset)
      val data = bytes.slice(offset + lengthBytes, offset + lengthBytes + length.toInt)
      (data, lengthBytes + length.toInt)

  // ============================================================================
  // Compact Integer Codec
  // ============================================================================

  /**
   * Wrapper type for compact integer encoding.
   * Use this when you need compact integer encoding instead of fixed-width.
   */
  opaque type CompactInt = Long

  object CompactInt:
    def apply(v: Long): CompactInt = v
    def apply(v: Int): CompactInt = v.toLong

  extension (c: CompactInt)
    def value: Long = c
    def toInt: Int = c.toInt

  given JamEncoder[CompactInt] with
    def encode(a: CompactInt): JamBytes =
      JamBytes(encoding.encodeCompactInteger(a))

  given JamDecoder[CompactInt] with
    def decode(bytes: JamBytes, offset: Int): (CompactInt, Int) =
      val (value, consumed) = encoding.decodeCompactInteger(bytes.toArray, offset)
      (CompactInt(value), consumed)

  // ============================================================================
  // Optional Type Codec
  // ============================================================================

  /**
   * Encoder for Option[A] using 0/1 prefix byte pattern.
   */
  given optionEncoder[A](using enc: JamEncoder[A]): JamEncoder[Option[A]] with
    def encode(a: Option[A]): JamBytes = a match
      case None => JamBytes(Array[Byte](0))
      case Some(v) => JamBytes(Array[Byte](1)) ++ enc.encode(v)

  /**
   * Decoder for Option[A] using 0/1 prefix byte pattern.
   */
  given optionDecoder[A](using dec: JamDecoder[A]): JamDecoder[Option[A]] with
    def decode(bytes: JamBytes, offset: Int): (Option[A], Int) =
      val prefix = bytes(offset).toInt
      if prefix == 0 then
        (None, 1)
      else
        val (value, consumed) = dec.decode(bytes, offset + 1)
        (Some(value), 1 + consumed)

  // ============================================================================
  // List Type Codec
  // ============================================================================

  /**
   * Encoder for List[A] using compact integer length prefix.
   */
  given listEncoder[A](using enc: JamEncoder[A]): JamEncoder[List[A]] with
    def encode(a: List[A]): JamBytes =
      val builder = JamBytes.newBuilder
      builder ++= encoding.encodeCompactInteger(a.length.toLong)
      for elem <- a do
        builder ++= enc.encode(elem).toArray
      builder.result()

  /**
   * Decoder for List[A] using compact integer length prefix.
   */
  given listDecoder[A](using dec: JamDecoder[A]): JamDecoder[List[A]] with
    def decode(bytes: JamBytes, offset: Int): (List[A], Int) =
      val arr = bytes.toArray
      val (length, lengthBytes) = encoding.decodeCompactInteger(arr, offset)
      var pos = offset + lengthBytes
      val result = scala.collection.mutable.ListBuffer[A]()
      for _ <- 0 until length.toInt do
        val (elem, consumed) = dec.decode(bytes, pos)
        result += elem
        pos += consumed
      (result.toList, pos - offset)

  // ============================================================================
  // Fixed-Size Array Encoding Utilities
  // ============================================================================

  /**
   * Encode a fixed-size JamBytes (no length prefix).
   */
  def encodeFixedBytes(bytes: JamBytes): JamBytes = bytes

  /**
   * Decode a fixed-size JamBytes.
   */
  def decodeFixedBytes(bytes: JamBytes, offset: Int, size: Int): (JamBytes, Int) =
    (bytes.slice(offset, offset + size), size)
