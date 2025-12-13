package io.forge.jam.protocol.history

import io.forge.jam.core.{JamBytes, codec, ChainConfig, constants}
import io.forge.jam.core.codec.{JamEncoder, JamDecoder, encode, decodeAs}
import io.forge.jam.core.primitives.Hash
import io.forge.jam.core.types.history.{ReportedWorkPackage, HistoricalBeta, HistoricalMmr, HistoricalBetaContainer}
import io.forge.jam.core.json.JsonHelpers.parseHash
import io.circe.Decoder

/**
 * Types for the History State Transition Function.
 * The History STF tracks block history using a Merkle Mountain Range (MMR)
 * for efficient beefy root calculation.
 */
object HistoryTypes:
  // Re-export core types for backward compatibility
  export io.forge.jam.core.types.history.ReportedWorkPackage
  export io.forge.jam.core.types.history.HistoricalBeta
  export io.forge.jam.core.types.history.HistoricalMmr
  export io.forge.jam.core.types.history.HistoricalBetaContainer

  /**
   * Historical state containing the beta container.
   */
  final case class HistoricalState(
    beta: HistoricalBetaContainer
  )

  object HistoricalState:
    given JamEncoder[HistoricalState] with
      def encode(a: HistoricalState): JamBytes =
        summon[JamEncoder[HistoricalBetaContainer]].encode(a.beta)

    given JamDecoder[HistoricalState] with
      def decode(bytes: JamBytes, offset: Int): (HistoricalState, Int) =
        val (beta, consumed) = summon[JamDecoder[HistoricalBetaContainer]].decode(bytes, offset)
        (HistoricalState(beta), consumed)

    given Decoder[HistoricalState] =
      Decoder.instance { cursor =>
        cursor.get[HistoricalBetaContainer]("beta").map(HistoricalState(_))
      }

  /**
   * Input to the History STF.
   */
  final case class HistoricalInput(
    headerHash: Hash,
    parentStateRoot: Hash,
    accumulateRoot: Hash,
    workPackages: List[ReportedWorkPackage]
  ):
    /**
     * Validate this input against the given chain configuration.
     * @param config The chain configuration (for max cores validation).
     * @throws IllegalArgumentException if validation fails.
     */
    def validate(config: ChainConfig = ChainConfig.FULL): Unit =
      require(headerHash.size == constants.HashSize, "Header hash must be 32 bytes")
      require(parentStateRoot.size == constants.HashSize, "Parent state root must be 32 bytes")
      require(accumulateRoot.size == constants.HashSize, "Accumulate root must be 32 bytes")
      require(workPackages.size <= config.coresCount, s"Too many work packages (max ${config.coresCount})")
      workPackages.zipWithIndex.foreach { (pkg, index) =>
        require(pkg.hash.size == constants.HashSize, s"Work package $index hash must be 32 bytes")
        require(pkg.exportsRoot.size == constants.HashSize, s"Work package $index exports root must be 32 bytes")
      }

  object HistoricalInput:
    given JamEncoder[HistoricalInput] with
      def encode(a: HistoricalInput): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.headerHash.bytes
        builder ++= a.parentStateRoot.bytes
        builder ++= a.accumulateRoot.bytes
        builder ++= codec.encodeCompactInteger(a.workPackages.length.toLong)
        for pkg <- a.workPackages do
          builder ++= summon[JamEncoder[ReportedWorkPackage]].encode(pkg)
        builder.result()

    given JamDecoder[HistoricalInput] with
      def decode(bytes: JamBytes, offset: Int): (HistoricalInput, Int) =
        val arr = bytes.toArray
        var pos = offset
        val headerHash = Hash(arr.slice(pos, pos + Hash.Size))
        pos += Hash.Size
        val parentStateRoot = Hash(arr.slice(pos, pos + Hash.Size))
        pos += Hash.Size
        val accumulateRoot = Hash(arr.slice(pos, pos + Hash.Size))
        pos += Hash.Size
        val (length, lengthBytes) = codec.decodeCompactInteger(arr, pos)
        pos += lengthBytes
        val workPackages = (0 until length.toInt).map { _ =>
          val (pkg, consumed) = summon[JamDecoder[ReportedWorkPackage]].decode(bytes, pos)
          pos += consumed
          pkg
        }.toList
        (HistoricalInput(headerHash, parentStateRoot, accumulateRoot, workPackages), pos - offset)

    given Decoder[HistoricalInput] =
      Decoder.instance { cursor =>
        for
          headerHashHex <- cursor.get[String]("header_hash")
          parentStateRootHex <- cursor.get[String]("parent_state_root")
          accumulateRootHex <- cursor.get[String]("accumulate_root")
          workPackages <- cursor.get[List[ReportedWorkPackage]]("work_packages")
          headerHash <- parseHash(headerHashHex)
          parentStateRoot <- parseHash(parentStateRootHex)
          accumulateRoot <- parseHash(accumulateRootHex)
        yield HistoricalInput(headerHash, parentStateRoot, accumulateRoot, workPackages)
      }

  /**
   * Test case for History STF containing input, pre-state, and post-state.
   */
  final case class HistoricalCase(
    input: HistoricalInput,
    preState: HistoricalState,
    postState: HistoricalState
  )

  object HistoricalCase:
    given JamEncoder[HistoricalCase] with
      def encode(a: HistoricalCase): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= summon[JamEncoder[HistoricalInput]].encode(a.input)
        builder ++= summon[JamEncoder[HistoricalState]].encode(a.preState)
        builder ++= summon[JamEncoder[HistoricalState]].encode(a.postState)
        builder.result()

    given JamDecoder[HistoricalCase] with
      def decode(bytes: JamBytes, offset: Int): (HistoricalCase, Int) =
        var pos = offset
        val (input, inputBytes) = summon[JamDecoder[HistoricalInput]].decode(bytes, pos)
        pos += inputBytes
        val (preState, preStateBytes) = summon[JamDecoder[HistoricalState]].decode(bytes, pos)
        pos += preStateBytes
        val (postState, postStateBytes) = summon[JamDecoder[HistoricalState]].decode(bytes, pos)
        pos += postStateBytes
        (HistoricalCase(input, preState, postState), pos - offset)

    given Decoder[HistoricalCase] =
      Decoder.instance { cursor =>
        for
          input <- cursor.get[HistoricalInput]("input")
          preState <- cursor.get[HistoricalState]("pre_state")
          postState <- cursor.get[HistoricalState]("post_state")
        yield HistoricalCase(input, preState, postState)
      }
