package io.forge.jam.core.types

import io.forge.jam.core.{JamBytes, codec}
import io.forge.jam.core.codec.{JamEncoder, JamDecoder, encode, decodeAs}
import io.forge.jam.core.primitives.Hash
import io.forge.jam.core.json.JsonHelpers.parseHex
import io.circe.Decoder

/**
 * Historical types.
 */
object history:

  /**
   * A reported work package with its hash and exports root.
   * Fixed size: 64 bytes (32-byte hash + 32-byte exports root)
   *
   * Used by: History, Report STFs
   */
  final case class ReportedWorkPackage(
    hash: Hash,
    exportsRoot: Hash
  )

  object ReportedWorkPackage:
    val Size: Int = Hash.Size * 2 // 64 bytes

    given JamEncoder[ReportedWorkPackage] with
      def encode(a: ReportedWorkPackage): JamBytes =
        JamBytes(a.hash.bytes ++ a.exportsRoot.bytes)

    given JamDecoder[ReportedWorkPackage] with
      def decode(bytes: JamBytes, offset: Int): (ReportedWorkPackage, Int) =
        val arr = bytes.toArray
        val hash = Hash(arr.slice(offset, offset + Hash.Size))
        val exportsRoot = Hash(arr.slice(offset + Hash.Size, offset + Size))
        (ReportedWorkPackage(hash, exportsRoot), Size)

    given Decoder[ReportedWorkPackage] =
      Decoder.instance { cursor =>
        for
          hashHex <- cursor.get[String]("hash")
          exportsRootHex <- cursor.get[String]("exports_root")
        yield ReportedWorkPackage(Hash(parseHex(hashHex)), Hash(parseHex(exportsRootHex)))
      }

  /**
   * Historical block entry containing header hash, beefy root, state root,
   * and reported work packages.
   */
  final case class HistoricalBeta(
    headerHash: Hash,
    beefyRoot: Hash,
    stateRoot: Hash,
    reported: List[ReportedWorkPackage]
  )

  object HistoricalBeta:
    given JamEncoder[HistoricalBeta] with
      def encode(a: HistoricalBeta): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.headerHash.bytes
        builder ++= a.beefyRoot.bytes
        builder ++= a.stateRoot.bytes
        builder ++= a.reported.encode
        builder.result()

    given JamDecoder[HistoricalBeta] with
      def decode(bytes: JamBytes, offset: Int): (HistoricalBeta, Int) =
        val arr = bytes.toArray
        var pos = offset
        val headerHash = Hash(arr.slice(pos, pos + Hash.Size))
        pos += Hash.Size
        val beefyRoot = Hash(arr.slice(pos, pos + Hash.Size))
        pos += Hash.Size
        val stateRoot = Hash(arr.slice(pos, pos + Hash.Size))
        pos += Hash.Size
        val (reported, reportedBytes) = bytes.decodeAs[List[ReportedWorkPackage]](pos)
        pos += reportedBytes
        (HistoricalBeta(headerHash, beefyRoot, stateRoot, reported), pos - offset)

    given Decoder[HistoricalBeta] =
      Decoder.instance { cursor =>
        for
          headerHash <- cursor.get[String]("header_hash").map(h => Hash(parseHex(h)))
          beefyRoot <- cursor.get[String]("beefy_root").map(h => Hash(parseHex(h)))
          stateRoot <- cursor.get[String]("state_root").map(h => Hash(parseHex(h)))
          reported <- cursor.get[List[ReportedWorkPackage]]("reported")
        yield HistoricalBeta(headerHash, beefyRoot, stateRoot, reported)
      }

  /**
   * Merkle Mountain Range (MMR) with optional peaks.
   * Peaks are ordered by tree height, with None representing empty positions.
   */
  final case class HistoricalMmr(
    peaks: List[Option[Hash]]
  )

  object HistoricalMmr:
    val empty: HistoricalMmr = HistoricalMmr(List.empty)

    given JamEncoder[HistoricalMmr] with
      def encode(a: HistoricalMmr): JamBytes = a.peaks.encode

    given JamDecoder[HistoricalMmr] with
      def decode(bytes: JamBytes, offset: Int): (HistoricalMmr, Int) =
        val (peaks, consumed) = bytes.decodeAs[List[Option[Hash]]](offset)
        (HistoricalMmr(peaks), consumed)

    given Decoder[HistoricalMmr] =
      Decoder.instance { cursor =>
        cursor.get[List[Option[String]]]("peaks").map { peaks =>
          HistoricalMmr(peaks.map(_.map(h => Hash(parseHex(h)))))
        }
      }

  /**
   * Container for historical beta entries and MMR.
   */
  final case class HistoricalBetaContainer(
    history: List[HistoricalBeta] = List.empty,
    mmr: HistoricalMmr = HistoricalMmr.empty
  )

  object HistoricalBetaContainer:
    given JamEncoder[HistoricalBetaContainer] with
      def encode(a: HistoricalBetaContainer): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.history.encode
        builder ++= a.mmr.encode
        builder.result()

    given JamDecoder[HistoricalBetaContainer] with
      def decode(bytes: JamBytes, offset: Int): (HistoricalBetaContainer, Int) =
        var pos = offset
        val (history, historyBytes) = bytes.decodeAs[List[HistoricalBeta]](pos)
        pos += historyBytes
        val (mmr, mmrBytes) = bytes.decodeAs[HistoricalMmr](pos)
        pos += mmrBytes
        (HistoricalBetaContainer(history, mmr), pos - offset)

    given Decoder[HistoricalBetaContainer] =
      Decoder.instance { cursor =>
        for
          history <- cursor.get[List[HistoricalBeta]]("history")
          mmr <- cursor.get[HistoricalMmr]("mmr")
        yield HistoricalBetaContainer(history, mmr)
      }
