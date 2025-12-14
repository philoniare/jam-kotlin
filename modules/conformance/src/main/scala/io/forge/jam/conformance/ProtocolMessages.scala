package io.forge.jam.conformance

import io.forge.jam.core.{ChainConfig, JamBytes, codec}
import io.forge.jam.core.codec.{JamEncoder, JamDecoder, encode, decodeAs}
import io.forge.jam.core.primitives.{Hash, Timeslot}
import io.forge.jam.core.types.block.Block
import io.forge.jam.core.types.header.Header
import io.forge.jam.protocol.traces.KeyValue
import spire.math.{UByte, UInt}

/**
 * Feature bits for protocol negotiation.
 */
object Features:
  /** Target has access to block ancestry of up to L items */
  val ANCESTRY: Int = 0x01
  /** Simple forking: forks are supported with max depth 1 */
  val FORKS: Int = 0x02
  /** All supported features for M1 conformance */
  val ALL_M1: Int = ANCESTRY | FORKS

/**
 * Protocol version information.
 */
final case class Version(
  major: UByte,
  minor: UByte,
  patch: UByte
)

object Version:
  val JAM_VERSION: Version = Version(UByte(0), UByte(7), UByte(0))
  val APP_VERSION: Version = Version(UByte(0), UByte(1), UByte(0))

  given JamEncoder[Version] with
    def encode(v: Version): JamBytes =
      JamBytes(Array(v.major.toByte, v.minor.toByte, v.patch.toByte))

  given JamDecoder[Version] with
    def decode(bytes: JamBytes, offset: Int): (Version, Int) =
      val arr = bytes.toArray
      val v = Version(
        UByte(arr(offset)),
        UByte(arr(offset + 1)),
        UByte(arr(offset + 2))
      )
      (v, 3)

/**
 * Peer information exchanged during handshake.
 *
 * @param fuzzVersion Fuzzer protocol version
 * @param fuzzFeatures Supported features bitmask
 * @param jamVersion JAM protocol version
 * @param appVersion Application version
 * @param appName Application name
 */
final case class PeerInfo(
  fuzzVersion: UByte,
  fuzzFeatures: UInt,
  jamVersion: Version,
  appVersion: Version,
  appName: String
)

object PeerInfo:
  val DISCRIMINANT: Int = 0x00

  /** Create target PeerInfo with standard values */
  def forTarget(features: Int = Features.ALL_M1): PeerInfo =
    PeerInfo(
      fuzzVersion = UByte(1),
      fuzzFeatures = UInt(features),
      jamVersion = Version.JAM_VERSION,
      appVersion = Version.APP_VERSION,
      appName = "jam-scala"
    )

  given JamEncoder[PeerInfo] with
    def encode(p: PeerInfo): JamBytes =
      val builder = JamBytes.newBuilder
      // fuzz_version: U8
      builder += p.fuzzVersion.toByte
      // fuzz_features: U32 LE
      builder ++= codec.encodeU32LE(p.fuzzFeatures)
      // jam_version: Version (3 bytes)
      builder ++= p.jamVersion.encode
      // app_version: Version (3 bytes)
      builder ++= p.appVersion.encode
      // app_name: compact length + UTF8 bytes
      val nameBytes = p.appName.getBytes("UTF-8")
      builder ++= codec.encodeCompactInteger(nameBytes.length.toLong)
      builder ++= nameBytes
      builder.result()

  given JamDecoder[PeerInfo] with
    def decode(bytes: JamBytes, offset: Int): (PeerInfo, Int) =
      val arr = bytes.toArray
      var pos = offset

      // fuzz_version: U8
      val fuzzVersion = UByte(arr(pos))
      pos += 1

      // fuzz_features: U32 LE
      val fuzzFeatures = codec.decodeU32LE(arr, pos)
      pos += 4

      // jam_version: Version
      val (jamVersion, jamVersionBytes) = bytes.decodeAs[Version](pos)
      pos += jamVersionBytes

      // app_version: Version
      val (appVersion, appVersionBytes) = bytes.decodeAs[Version](pos)
      pos += appVersionBytes

      // app_name: compact length + UTF8 bytes
      val (nameLength, nameLengthBytes) = codec.decodeCompactInteger(arr, pos)
      pos += nameLengthBytes
      val appName = new String(arr.slice(pos, pos + nameLength.toInt), "UTF-8")
      pos += nameLength.toInt

      (PeerInfo(fuzzVersion, fuzzFeatures, jamVersion, appVersion, appName), pos - offset)

/**
 * Ancestry item containing slot and header hash.
 */
final case class AncestryItem(
  slot: Timeslot,
  headerHash: Hash
)

object AncestryItem:
  given JamEncoder[AncestryItem] with
    def encode(a: AncestryItem): JamBytes =
      val builder = JamBytes.newBuilder
      builder ++= codec.encodeU32LE(a.slot.value)
      builder ++= a.headerHash.bytes
      builder.result()

  given JamDecoder[AncestryItem] with
    def decode(bytes: JamBytes, offset: Int): (AncestryItem, Int) =
      val arr = bytes.toArray
      val slot = Timeslot(codec.decodeU32LE(arr, offset))
      val headerHash = Hash(arr.slice(offset + 4, offset + 4 + 32))
      (AncestryItem(slot, headerHash), 4 + 32)

/**
 * Initialize message containing genesis-like header, state, and ancestry.
 */
final case class Initialize(
  header: Header,
  keyvals: List[KeyValue],
  ancestry: List[AncestryItem]
)

object Initialize:
  val DISCRIMINANT: Int = 0x01

  def decoder(config: ChainConfig): JamDecoder[Initialize] = new JamDecoder[Initialize]:
    def decode(bytes: JamBytes, offset: Int): (Initialize, Int) =
      val arr = bytes.toArray
      var pos = offset

      // header: Header (config-dependent)
      val headerDecoder = Header.decoder(config)
      val (header, headerBytes) = headerDecoder.decode(bytes, pos)
      pos += headerBytes

      // keyvals: List[KeyValue] (compact length prefix)
      val (keyvalsLength, keyvalsLengthBytes) = codec.decodeCompactInteger(arr, pos)
      pos += keyvalsLengthBytes
      val keyvals = (0 until keyvalsLength.toInt).map { _ =>
        val (kv, consumed) = bytes.decodeAs[KeyValue](pos)
        pos += consumed
        kv
      }.toList

      // ancestry: List[AncestryItem] (compact length prefix)
      val (ancestryLength, ancestryLengthBytes) = codec.decodeCompactInteger(arr, pos)
      pos += ancestryLengthBytes
      val ancestry = (0 until ancestryLength.toInt).map { _ =>
        val (item, consumed) = bytes.decodeAs[AncestryItem](pos)
        pos += consumed
        item
      }.toList

      (Initialize(header, keyvals, ancestry), pos - offset)

/**
 * StateRoot response message containing 32-byte state root hash.
 */
final case class StateRoot(hash: Hash)

object StateRoot:
  val DISCRIMINANT: Int = 0x02

  given JamEncoder[StateRoot] with
    def encode(s: StateRoot): JamBytes = JamBytes(s.hash.bytes)

  given JamDecoder[StateRoot] with
    def decode(bytes: JamBytes, offset: Int): (StateRoot, Int) =
      val hash = Hash(bytes.toArray.slice(offset, offset + 32))
      (StateRoot(hash), 32)

/**
 * ImportBlock request message wrapping a Block.
 */
final case class ImportBlock(block: Block)

object ImportBlock:
  val DISCRIMINANT: Int = 0x03

  def decoder(config: ChainConfig): JamDecoder[ImportBlock] = new JamDecoder[ImportBlock]:
    def decode(bytes: JamBytes, offset: Int): (ImportBlock, Int) =
      val blockDecoder = Block.decoder(config)
      val (block, consumed) = blockDecoder.decode(bytes, offset)
      (ImportBlock(block), consumed)

/**
 * GetState request message containing header hash.
 */
final case class GetState(headerHash: Hash)

object GetState:
  val DISCRIMINANT: Int = 0x04

  given JamEncoder[GetState] with
    def encode(g: GetState): JamBytes = JamBytes(g.headerHash.bytes)

  given JamDecoder[GetState] with
    def decode(bytes: JamBytes, offset: Int): (GetState, Int) =
      val hash = Hash(bytes.toArray.slice(offset, offset + 32))
      (GetState(hash), 32)

/**
 * State response message containing key-value pairs.
 */
final case class State(keyvals: List[KeyValue])

object State:
  val DISCRIMINANT: Int = 0x05

  given JamEncoder[State] with
    def encode(s: State): JamBytes =
      val builder = JamBytes.newBuilder
      builder ++= codec.encodeCompactInteger(s.keyvals.length.toLong)
      for kv <- s.keyvals do
        builder ++= kv.encode
      builder.result()

  given JamDecoder[State] with
    def decode(bytes: JamBytes, offset: Int): (State, Int) =
      val arr = bytes.toArray
      var pos = offset
      val (length, lengthBytes) = codec.decodeCompactInteger(arr, pos)
      pos += lengthBytes
      val keyvals = (0 until length.toInt).map { _ =>
        val (kv, consumed) = bytes.decodeAs[KeyValue](pos)
        pos += consumed
        kv
      }.toList
      (State(keyvals), pos - offset)

/**
 * Error response message with UTF8 string.
 */
final case class Error(message: String)

object Error:
  val DISCRIMINANT: Int = 0xFF

  given JamEncoder[Error] with
    def encode(e: Error): JamBytes =
      val msgBytes = e.message.getBytes("UTF-8")
      val builder = JamBytes.newBuilder
      builder ++= codec.encodeCompactInteger(msgBytes.length.toLong)
      builder ++= msgBytes
      builder.result()

  given JamDecoder[Error] with
    def decode(bytes: JamBytes, offset: Int): (Error, Int) =
      val arr = bytes.toArray
      val (length, lengthBytes) = codec.decodeCompactInteger(arr, offset)
      val message = new String(arr.slice(offset + lengthBytes, offset + lengthBytes + length.toInt), "UTF-8")
      (Error(message), lengthBytes + length.toInt)

/**
 * Sealed trait representing all protocol messages.
 */
sealed trait ProtocolMessage

object ProtocolMessage:
  final case class PeerInfoMsg(info: PeerInfo) extends ProtocolMessage
  final case class InitializeMsg(init: Initialize) extends ProtocolMessage
  final case class StateRootMsg(root: StateRoot) extends ProtocolMessage
  final case class ImportBlockMsg(importBlock: ImportBlock) extends ProtocolMessage
  final case class GetStateMsg(getState: GetState) extends ProtocolMessage
  final case class StateMsg(state: State) extends ProtocolMessage
  final case class ErrorMsg(error: Error) extends ProtocolMessage

  /**
   * Encode a protocol message with discriminant prefix.
   */
  def encodeMessage(msg: ProtocolMessage): JamBytes =
    msg match
      case PeerInfoMsg(info) =>
        JamBytes(Array(PeerInfo.DISCRIMINANT.toByte)) ++ info.encode
      case StateRootMsg(root) =>
        JamBytes(Array(StateRoot.DISCRIMINANT.toByte)) ++ root.encode
      case StateMsg(state) =>
        JamBytes(Array(State.DISCRIMINANT.toByte)) ++ state.encode
      case ErrorMsg(error) =>
        JamBytes(Array(Error.DISCRIMINANT.toByte)) ++ error.encode
      case _ =>
        throw new IllegalArgumentException(s"Cannot encode message type: $msg")

  /**
   * Encode a protocol message with 32-bit LE length prefix.
   */
  def encodeWithFrame(msg: ProtocolMessage): JamBytes =
    val body = encodeMessage(msg)
    val lengthPrefix = JamBytes(codec.encodeU32LE(UInt(body.length)))
    lengthPrefix ++ body

  /**
   * Decode a protocol message from bytes (without length prefix).
   * The discriminant byte indicates message type.
   */
  def decodeMessage(bytes: JamBytes, offset: Int, config: ChainConfig): (ProtocolMessage, Int) =
    val discriminant = bytes.toArray(offset) & 0xFF
    val bodyOffset = offset + 1

    discriminant match
      case PeerInfo.DISCRIMINANT =>
        val (info, consumed) = bytes.decodeAs[PeerInfo](bodyOffset)
        (PeerInfoMsg(info), 1 + consumed)

      case Initialize.DISCRIMINANT =>
        val decoder = Initialize.decoder(config)
        val (init, consumed) = decoder.decode(bytes, bodyOffset)
        (InitializeMsg(init), 1 + consumed)

      case StateRoot.DISCRIMINANT =>
        val (root, consumed) = bytes.decodeAs[StateRoot](bodyOffset)
        (StateRootMsg(root), 1 + consumed)

      case ImportBlock.DISCRIMINANT =>
        val decoder = ImportBlock.decoder(config)
        val (importBlock, consumed) = decoder.decode(bytes, bodyOffset)
        (ImportBlockMsg(importBlock), 1 + consumed)

      case GetState.DISCRIMINANT =>
        val (getState, consumed) = bytes.decodeAs[GetState](bodyOffset)
        (GetStateMsg(getState), 1 + consumed)

      case State.DISCRIMINANT =>
        val (state, consumed) = bytes.decodeAs[State](bodyOffset)
        (StateMsg(state), 1 + consumed)

      case Error.DISCRIMINANT =>
        val (error, consumed) = bytes.decodeAs[Error](bodyOffset)
        (ErrorMsg(error), 1 + consumed)

      case other =>
        throw new IllegalArgumentException(s"Unknown message discriminant: 0x${other.toHexString}")
