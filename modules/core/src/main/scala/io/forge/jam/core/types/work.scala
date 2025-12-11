package io.forge.jam.core.types

import io.forge.jam.core.{JamBytes, codec, encoding}
import io.forge.jam.core.codec.{JamEncoder, JamDecoder}
import io.forge.jam.core.primitives.{Hash, ValidatorIndex, Ed25519Signature}
import spire.math.{UByte, UShort, UInt}

/**
 * Work-related simple types
 */
object work:

  /**
   * Package specification containing hash, length, erasure root, exports root, and exports count.
   * Fixed size: 102 bytes (32 + 4 + 32 + 32 + 2)
   */
  final case class PackageSpec(
    hash: Hash,
    length: UInt,
    erasureRoot: Hash,
    exportsRoot: Hash,
    exportsCount: UShort
  )

  object PackageSpec:
    val Size: Int = Hash.Size + 4 + Hash.Size + Hash.Size + 2 // 102 bytes

    given JamEncoder[PackageSpec] with
      def encode(a: PackageSpec): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.hash.bytes
        builder ++= encoding.encodeU32LE(a.length)
        builder ++= a.erasureRoot.bytes
        builder ++= a.exportsRoot.bytes
        builder ++= encoding.encodeU16LE(a.exportsCount)
        builder.result()

    given JamDecoder[PackageSpec] with
      def decode(bytes: JamBytes, offset: Int): (PackageSpec, Int) =
        val arr = bytes.toArray
        val hash = Hash(arr.slice(offset, offset + Hash.Size))
        val length = encoding.decodeU32LE(arr, offset + Hash.Size)
        val erasureRoot = Hash(arr.slice(offset + 36, offset + 68))
        val exportsRoot = Hash(arr.slice(offset + 68, offset + 100))
        val exportsCount = encoding.decodeU16LE(arr, offset + 100)
        (PackageSpec(hash, length, erasureRoot, exportsRoot, exportsCount), Size)

  /**
   * Execution result - either Ok with output data or Panic.
   *
   * Encoding:
   * - Ok: 0x00 + compact length prefix + data bytes
   * - Panic: 0x02
   */
  enum ExecutionResult:
    case Ok(output: JamBytes)
    case Panic

  object ExecutionResult:
    private val OkTag: Byte = 0x00
    private val PanicTag: Byte = 0x02

    given JamEncoder[ExecutionResult] with
      def encode(a: ExecutionResult): JamBytes = a match
        case ExecutionResult.Ok(output) =>
          val builder = JamBytes.newBuilder
          builder += OkTag
          builder ++= encoding.encodeCompactInteger(output.length.toLong)
          builder ++= output
          builder.result()
        case ExecutionResult.Panic =>
          JamBytes(Array(PanicTag))

    given JamDecoder[ExecutionResult] with
      def decode(bytes: JamBytes, offset: Int): (ExecutionResult, Int) =
        val tag = bytes.signedAt(offset)
        tag match
          case OkTag =>
            val (length, lengthBytes) = encoding.decodeCompactInteger(bytes.toArray, offset + 1)
            val output = bytes.slice(offset + 1 + lengthBytes, offset + 1 + lengthBytes + length.toInt)
            (ExecutionResult.Ok(output), 1 + lengthBytes + length.toInt)
          case PanicTag =>
            (ExecutionResult.Panic, 1)
          case _ =>
            // Unknown tag, treat as Panic for robustness
            (ExecutionResult.Panic, 1)

  /**
   * A vote containing validator index and Ed25519 signature.
   * Fixed size: 67 bytes (1 byte vote + 2 bytes index + 64 bytes signature)
   */
  final case class Vote(
    vote: Boolean,
    validatorIndex: ValidatorIndex,
    signature: Ed25519Signature
  )

  object Vote:
    val Size: Int = 1 + 2 + Ed25519Signature.Size // 67 bytes

    given JamEncoder[Vote] with
      def encode(a: Vote): JamBytes =
        val builder = JamBytes.newBuilder
        builder += (if a.vote then 1.toByte else 0.toByte)
        builder ++= encoding.encodeU16LE(a.validatorIndex.value)
        builder ++= a.signature.bytes
        builder.result()

    given JamDecoder[Vote] with
      def decode(bytes: JamBytes, offset: Int): (Vote, Int) =
        val arr = bytes.toArray
        val vote = arr(offset) != 0
        val validatorIndex = ValidatorIndex(encoding.decodeU16LE(arr, offset + 1))
        val signature = Ed25519Signature(arr.slice(offset + 3, offset + 3 + Ed25519Signature.Size))
        (Vote(vote, validatorIndex, signature), Size)
