package io.forge.jam.protocol.statistics

import io.forge.jam.core.{ChainConfig, JamBytes, codec}
import io.forge.jam.core.codec.{JamEncoder, JamDecoder, encodeFixedList, decodeFixedList, listDecoder, encode, decodeAs}
import io.forge.jam.core.types.epoch.ValidatorKey
import io.forge.jam.core.types.extrinsic.{Preimage, AssuranceExtrinsic, Dispute, GuaranteeExtrinsic}
import io.forge.jam.core.types.tickets.TicketEnvelope
import io.circe.Decoder
import spire.math.{UShort, UInt}

/**
 * Types for the Statistics State Transition Function.
 *
 * The Statistics STF tracks validator performance statistics including:
 * blocks authored, tickets submitted, preimages, guarantees, and assurances.
 *
 * v0.7.0+ format includes:
 * - accumulator: current epoch validator stats (fixed-size array)
 * - previous: previous epoch validator stats (fixed-size array)
 * - core: per-core statistics (fixed-size array)
 * - service: per-service statistics (sorted map)
 */
object StatisticsTypes:

  /**
   * Statistics counter for a single validator.
   * Each field is a 4-byte unsigned integer (u32).
   * Fixed size: 24 bytes (6 * 4 bytes)
   */
  final case class StatCount(
    blocks: Long,
    tickets: Long,
    preImages: Long,
    preImagesSize: Long,
    guarantees: Long,
    assurances: Long
  )

  object StatCount:
    val Size: Int = 24 // 6 * 4 bytes

    def zero: StatCount = StatCount(0, 0, 0, 0, 0, 0)

    given JamEncoder[StatCount] with
      def encode(a: StatCount): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= codec.encodeU32LE(UInt(a.blocks.toInt))
        builder ++= codec.encodeU32LE(UInt(a.tickets.toInt))
        builder ++= codec.encodeU32LE(UInt(a.preImages.toInt))
        builder ++= codec.encodeU32LE(UInt(a.preImagesSize.toInt))
        builder ++= codec.encodeU32LE(UInt(a.guarantees.toInt))
        builder ++= codec.encodeU32LE(UInt(a.assurances.toInt))
        builder.result()

    given JamDecoder[StatCount] with
      def decode(bytes: JamBytes, offset: Int): (StatCount, Int) =
        val arr = bytes.toArray
        var pos = offset
        val blocks = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        val tickets = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        val preImages = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        val preImagesSize = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        val guarantees = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        val assurances = codec.decodeU32LE(arr, pos).toLong
        (StatCount(blocks, tickets, preImages, preImagesSize, guarantees, assurances), Size)

    given Decoder[StatCount] =
      Decoder.instance { cursor =>
        for
          blocks <- cursor.get[Long]("blocks")
          tickets <- cursor.get[Long]("tickets")
          preImages <- cursor.get[Long]("pre_images")
          preImagesSize <- cursor.get[Long]("pre_images_size")
          guarantees <- cursor.get[Long]("guarantees")
          assurances <- cursor.get[Long]("assurances")
        yield StatCount(blocks, tickets, preImages, preImagesSize, guarantees, assurances)
      }

  /**
   * Per-core statistics.
   * All fields are compact-encoded unsigned integers.
   */
  final case class CoreStatistics(
    dataSize: Long = 0,        // d: total incoming data size
    assuranceCount: Long = 0,  // p: total number of assurances
    importsCount: Long = 0,    // i: total imports from Segments DA
    extrinsicsCount: Long = 0, // x: total extrinsics count
    extrinsicsSize: Long = 0,  // z: total extrinsics size
    exportsCount: Long = 0,    // e: total exports to Segments DA
    packageSize: Long = 0,     // b: total package data length
    gasUsed: Long = 0          // u: total gas used during refinement
  )

  object CoreStatistics:
    def zero: CoreStatistics = CoreStatistics()

    given JamEncoder[CoreStatistics] with
      def encode(a: CoreStatistics): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= codec.encodeCompactInteger(a.dataSize)
        builder ++= codec.encodeCompactInteger(a.assuranceCount)
        builder ++= codec.encodeCompactInteger(a.importsCount)
        builder ++= codec.encodeCompactInteger(a.extrinsicsCount)
        builder ++= codec.encodeCompactInteger(a.extrinsicsSize)
        builder ++= codec.encodeCompactInteger(a.exportsCount)
        builder ++= codec.encodeCompactInteger(a.packageSize)
        builder ++= codec.encodeCompactInteger(a.gasUsed)
        builder.result()

    given JamDecoder[CoreStatistics] with
      def decode(bytes: JamBytes, offset: Int): (CoreStatistics, Int) =
        val arr = bytes.toArray
        var pos = offset
        val (dataSize, ds1) = codec.decodeCompactInteger(arr, pos)
        pos += ds1
        val (assuranceCount, ds2) = codec.decodeCompactInteger(arr, pos)
        pos += ds2
        val (importsCount, ds3) = codec.decodeCompactInteger(arr, pos)
        pos += ds3
        val (extrinsicsCount, ds4) = codec.decodeCompactInteger(arr, pos)
        pos += ds4
        val (extrinsicsSize, ds5) = codec.decodeCompactInteger(arr, pos)
        pos += ds5
        val (exportsCount, ds6) = codec.decodeCompactInteger(arr, pos)
        pos += ds6
        val (packageSize, ds7) = codec.decodeCompactInteger(arr, pos)
        pos += ds7
        val (gasUsed, ds8) = codec.decodeCompactInteger(arr, pos)
        pos += ds8
        (CoreStatistics(dataSize, assuranceCount, importsCount, extrinsicsCount, extrinsicsSize, exportsCount, packageSize, gasUsed), pos - offset)

  /**
   * Count and gas tuple for service stats.
   */
  final case class CountAndGas(
    count: Long = 0,
    gasUsed: Long = 0
  )

  object CountAndGas:
    given JamEncoder[CountAndGas] with
      def encode(a: CountAndGas): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= codec.encodeCompactInteger(a.count)
        builder ++= codec.encodeCompactInteger(a.gasUsed)
        builder.result()

    given JamDecoder[CountAndGas] with
      def decode(bytes: JamBytes, offset: Int): (CountAndGas, Int) =
        val arr = bytes.toArray
        var pos = offset
        val (count, ds1) = codec.decodeCompactInteger(arr, pos)
        pos += ds1
        val (gasUsed, ds2) = codec.decodeCompactInteger(arr, pos)
        pos += ds2
        (CountAndGas(count, gasUsed), pos - offset)

  /**
   * Preimages count and size tuple.
   */
  final case class PreimagesAndSize(
    count: Long = 0,
    size: Long = 0
  )

  object PreimagesAndSize:
    given JamEncoder[PreimagesAndSize] with
      def encode(a: PreimagesAndSize): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= codec.encodeCompactInteger(a.count)
        builder ++= codec.encodeCompactInteger(a.size)
        builder.result()

    given JamDecoder[PreimagesAndSize] with
      def decode(bytes: JamBytes, offset: Int): (PreimagesAndSize, Int) =
        val arr = bytes.toArray
        var pos = offset
        val (count, ds1) = codec.decodeCompactInteger(arr, pos)
        pos += ds1
        val (size, ds2) = codec.decodeCompactInteger(arr, pos)
        pos += ds2
        (PreimagesAndSize(count, size), pos - offset)

  /**
   * Per-service statistics.
   */
  final case class ServiceStatistics(
    preimages: PreimagesAndSize = PreimagesAndSize(),  // p
    refines: CountAndGas = CountAndGas(),              // r
    importsCount: Long = 0,                            // i
    extrinsicsCount: Long = 0,                         // x
    extrinsicsSize: Long = 0,                          // z
    exportsCount: Long = 0,                            // e
    accumulates: CountAndGas = CountAndGas(),          // a
    transfers: CountAndGas = CountAndGas()             // t
  )

  object ServiceStatistics:
    def zero: ServiceStatistics = ServiceStatistics()

    given JamEncoder[ServiceStatistics] with
      def encode(a: ServiceStatistics): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.preimages.encode
        builder ++= a.refines.encode
        builder ++= codec.encodeCompactInteger(a.importsCount)
        builder ++= codec.encodeCompactInteger(a.extrinsicsCount)
        builder ++= codec.encodeCompactInteger(a.extrinsicsSize)
        builder ++= codec.encodeCompactInteger(a.exportsCount)
        builder ++= a.accumulates.encode
        builder ++= a.transfers.encode
        builder.result()

    given JamDecoder[ServiceStatistics] with
      def decode(bytes: JamBytes, offset: Int): (ServiceStatistics, Int) =
        var pos = offset
        val (preimages, ps1) = bytes.decodeAs[PreimagesAndSize](pos)
        pos += ps1
        val (refines, rs1) = bytes.decodeAs[CountAndGas](pos)
        pos += rs1
        val arr = bytes.toArray
        val (importsCount, ds1) = codec.decodeCompactInteger(arr, pos)
        pos += ds1
        val (extrinsicsCount, ds2) = codec.decodeCompactInteger(arr, pos)
        pos += ds2
        val (extrinsicsSize, ds3) = codec.decodeCompactInteger(arr, pos)
        pos += ds3
        val (exportsCount, ds4) = codec.decodeCompactInteger(arr, pos)
        pos += ds4
        val (accumulates, as1) = bytes.decodeAs[CountAndGas](pos)
        pos += as1
        val (transfers, ts1) = bytes.decodeAs[CountAndGas](pos)
        pos += ts1
        (ServiceStatistics(preimages, refines, importsCount, extrinsicsCount, extrinsicsSize, exportsCount, accumulates, transfers), pos - offset)

  /**
   * Service statistics entry (service ID + stats).
   */
  final case class ServiceStatisticsEntry(
    serviceId: Long,
    stats: ServiceStatistics
  )

  object ServiceStatisticsEntry:
    given JamEncoder[ServiceStatisticsEntry] with
      def encode(a: ServiceStatisticsEntry): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= codec.encodeU32LE(UInt(a.serviceId.toInt))
        builder ++= a.stats.encode
        builder.result()

    given JamDecoder[ServiceStatisticsEntry] with
      def decode(bytes: JamBytes, offset: Int): (ServiceStatisticsEntry, Int) =
        val arr = bytes.toArray
        var pos = offset
        val serviceId = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        val (stats, ss1) = bytes.decodeAs[ServiceStatistics](pos)
        pos += ss1
        (ServiceStatisticsEntry(serviceId, stats), pos - offset)

  /**
   * Full activity statistics (v0.7.0+ format).
   * Contains validator stats (accumulator, previous), core stats, and service stats.
   */
  final case class ActivityStatistics(
    accumulator: List[StatCount],          // current epoch validator stats
    previous: List[StatCount],             // previous epoch validator stats
    core: List[CoreStatistics],            // per-core statistics
    service: List[ServiceStatisticsEntry]  // per-service statistics (sorted by service ID)
  )

  object ActivityStatistics:
    def empty(validatorCount: Int, coreCount: Int): ActivityStatistics =
      ActivityStatistics(
        accumulator = List.fill(validatorCount)(StatCount.zero),
        previous = List.fill(validatorCount)(StatCount.zero),
        core = List.fill(coreCount)(CoreStatistics.zero),
        service = List.empty
      )

    given JamEncoder[ActivityStatistics] with
      def encode(a: ActivityStatistics): JamBytes =
        val builder = JamBytes.newBuilder
        // accumulator - fixed-size array (no length prefix)
        builder ++= encodeFixedList(a.accumulator)
        // previous - fixed-size array (no length prefix)
        builder ++= encodeFixedList(a.previous)
        // core - fixed-size array (no length prefix)
        builder ++= encodeFixedList(a.core)
        // service - compact length prefix + sorted entries
        val sortedService = a.service.sortBy(_.serviceId)
        builder ++= codec.encodeCompactInteger(sortedService.size.toLong)
        for entry <- sortedService do
          builder ++= entry.encode
        builder.result()

    def decoder(validatorCount: Int, coreCount: Int): JamDecoder[ActivityStatistics] = new JamDecoder[ActivityStatistics]:
      def decode(bytes: JamBytes, offset: Int): (ActivityStatistics, Int) =
        var pos = offset
        // accumulator - fixed-size
        val (accumulator, acc1) = decodeFixedList[StatCount](bytes, pos, validatorCount)
        pos += acc1
        // previous - fixed-size
        val (previous, prev1) = decodeFixedList[StatCount](bytes, pos, validatorCount)
        pos += prev1
        // core - fixed-size
        val (core, core1) = decodeFixedList[CoreStatistics](bytes, pos, coreCount)
        pos += core1
        // service - compact length prefix
        val arr = bytes.toArray
        val (serviceCount, sc1) = codec.decodeCompactInteger(arr, pos)
        pos += sc1
        val service = (0 until serviceCount.toInt).map { _ =>
          val (entry, es1) = bytes.decodeAs[ServiceStatisticsEntry](pos)
          pos += es1
          entry
        }.toList
        (ActivityStatistics(accumulator, previous, core, service), pos - offset)

  /**
   * Extrinsic data relevant to statistics.
   */
  final case class StatExtrinsic(
    tickets: List[TicketEnvelope],
    preimages: List[Preimage],
    guarantees: List[GuaranteeExtrinsic],
    assurances: List[AssuranceExtrinsic],
    disputes: Dispute
  )

  object StatExtrinsic:
    given JamEncoder[StatExtrinsic] with
      def encode(a: StatExtrinsic): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.tickets.encode
        builder ++= a.preimages.encode
        builder ++= a.guarantees.encode
        builder ++= a.assurances.encode
        builder ++= a.disputes.encode
        builder.result()

    /** Create a decoder that knows the chain config */
    def decoder(config: ChainConfig): JamDecoder[StatExtrinsic] = new JamDecoder[StatExtrinsic]:
      private val coreCount = config.coresCount
      private val validatorCount = config.validatorCount
      def decode(bytes: JamBytes, offset: Int): (StatExtrinsic, Int) =
        val arr = bytes.toArray
        var pos = offset

        // tickets - use listDecoder
        val (tickets, ticketsBytes) = listDecoder[TicketEnvelope].decode(bytes, pos)
        pos += ticketsBytes

        // preimages - use listDecoder
        val (preimages, preimagesBytes) = listDecoder[Preimage].decode(bytes, pos)
        pos += preimagesBytes

        // guarantees - use listDecoder
        val (guarantees, guaranteesBytes) = listDecoder[GuaranteeExtrinsic].decode(bytes, pos)
        pos += guaranteesBytes

        // assurances - need custom decoder due to config-dependent size
        val (assurancesLength, assurancesLengthBytes) = codec.decodeCompactInteger(arr, pos)
        pos += assurancesLengthBytes
        val assuranceDecoder = AssuranceExtrinsic.decoder(coreCount)
        val assurances = (0 until assurancesLength.toInt).map { _ =>
          val (assurance, consumed) = assuranceDecoder.decode(bytes, pos)
          pos += consumed
          assurance
        }.toList

        // disputes - variable size, votesPerVerdict is 2/3 + 1 of validators
        val votesPerVerdict = (validatorCount * 2 / 3) + 1
        val disputeDecoder = Dispute.decoder(votesPerVerdict)
        val (disputes, disputesBytes) = disputeDecoder.decode(bytes, pos)
        pos += disputesBytes

        (StatExtrinsic(tickets, preimages, guarantees, assurances, disputes), pos - offset)

    given Decoder[StatExtrinsic] =
      Decoder.instance { cursor =>
        for
          tickets <- cursor.get[List[TicketEnvelope]]("tickets")
          preimages <- cursor.get[List[Preimage]]("preimages")
          guarantees <- cursor.get[List[GuaranteeExtrinsic]]("guarantees")
          assurances <- cursor.get[List[AssuranceExtrinsic]]("assurances")
          disputes <- cursor.get[Dispute]("disputes")
        yield StatExtrinsic(tickets, preimages, guarantees, assurances, disputes)
      }

  /**
   * Input to the Statistics STF.
   */
  final case class StatInput(
    slot: Long,
    authorIndex: Long,
    extrinsic: StatExtrinsic
  )

  object StatInput:
    given JamEncoder[StatInput] with
      def encode(a: StatInput): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= codec.encodeU32LE(UInt(a.slot.toInt))
        builder ++= codec.encodeU16LE(UShort(a.authorIndex.toInt))
        builder ++= a.extrinsic.encode
        builder.result()

    /** Create a decoder that knows the chain config */
    def decoder(config: ChainConfig): JamDecoder[StatInput] = new JamDecoder[StatInput]:
      def decode(bytes: JamBytes, offset: Int): (StatInput, Int) =
        val arr = bytes.toArray
        var pos = offset
        // slot - 4 bytes (u32)
        val slot = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        // authorIndex - 2 bytes (u16)
        val authorIndex = codec.decodeU16LE(arr, pos).toLong
        pos += 2
        // extrinsic
        val extrinsicDecoder = StatExtrinsic.decoder(config)
        val (extrinsic, extrinsicBytes) = extrinsicDecoder.decode(bytes, pos)
        pos += extrinsicBytes
        (StatInput(slot, authorIndex, extrinsic), pos - offset)

    given Decoder[StatInput] =
      Decoder.instance { cursor =>
        for
          slot <- cursor.get[Long]("slot")
          authorIndex <- cursor.get[Long]("author_index")
          extrinsic <- cursor.get[StatExtrinsic]("extrinsic")
        yield StatInput(slot, authorIndex, extrinsic)
      }

  /**
   * Statistics state containing validator stats and metadata.
   */
  final case class StatState(
    valsCurrStats: List[StatCount],
    valsLastStats: List[StatCount],
    slot: Long,
    currValidators: List[ValidatorKey]
  )

  object StatState:
    given JamEncoder[StatState] with
      def encode(a: StatState): JamBytes =
        val builder = JamBytes.newBuilder
        // valsCurrStats - fixed size list (no length prefix)
        builder ++= encodeFixedList(a.valsCurrStats)
        // valsLastStats - fixed size list (no length prefix)
        builder ++= encodeFixedList(a.valsLastStats)
        // slot - 4 bytes (u32)
        builder ++= codec.encodeU32LE(UInt(a.slot.toInt))
        // currValidators - fixed size list (no length prefix)
        builder ++= encodeFixedList(a.currValidators)
        builder.result()

    /** Create a decoder that knows the chain config */
    def decoder(config: ChainConfig): JamDecoder[StatState] = new JamDecoder[StatState]:
      private val validatorCount = config.validatorCount
      def decode(bytes: JamBytes, offset: Int): (StatState, Int) =
        val arr = bytes.toArray
        var pos = offset
        // valsCurrStats - fixed size list
        val (valsCurrStats, currStatsBytes) = decodeFixedList[StatCount](bytes, pos, validatorCount)
        pos += currStatsBytes
        // valsLastStats - fixed size list
        val (valsLastStats, lastStatsBytes) = decodeFixedList[StatCount](bytes, pos, validatorCount)
        pos += lastStatsBytes
        // slot - 4 bytes (u32)
        val slot = codec.decodeU32LE(arr, pos).toLong
        pos += 4
        // currValidators - fixed size list
        val (currValidators, validatorsBytes) = decodeFixedList[ValidatorKey](bytes, pos, validatorCount)
        pos += validatorsBytes
        (StatState(valsCurrStats, valsLastStats, slot, currValidators), pos - offset)

    given Decoder[StatState] =
      Decoder.instance { cursor =>
        for
          valsCurrStats <- cursor.get[List[StatCount]]("vals_curr_stats")
          valsLastStats <- cursor.get[List[StatCount]]("vals_last_stats")
          slot <- cursor.get[Long]("slot")
          currValidators <- cursor.get[List[ValidatorKey]]("curr_validators")
        yield StatState(valsCurrStats, valsLastStats, slot, currValidators)
      }

  /**
   * Output from the Statistics STF (always null in encoding).
   */
  final case class StatOutput(id: Long)

  object StatOutput:
    given Decoder[Option[StatOutput]] =
      Decoder.decodeOption(Decoder.instance { cursor =>
        cursor.get[Long]("id").map(StatOutput(_))
      })

  /**
   * Test case for Statistics STF containing input, pre-state, and post-state.
   */
  final case class StatCase(
    input: StatInput,
    preState: StatState,
    output: Option[StatOutput],
    postState: StatState
  )

  object StatCase:
    /** Create a config-aware decoder for StatCase */
    def decoder(config: ChainConfig): JamDecoder[StatCase] = new JamDecoder[StatCase]:
      def decode(bytes: JamBytes, offset: Int): (StatCase, Int) =
        var pos = offset
        val inputDecoder = StatInput.decoder(config)
        val (input, inputBytes) = inputDecoder.decode(bytes, pos)
        pos += inputBytes
        val stateDecoder = StatState.decoder(config)
        val (preState, preStateBytes) = stateDecoder.decode(bytes, pos)
        pos += preStateBytes
        // output is always null in encoding
        val (postState, postStateBytes) = stateDecoder.decode(bytes, pos)
        pos += postStateBytes
        (StatCase(input, preState, None, postState), pos - offset)

    given JamEncoder[StatCase] with
      def encode(a: StatCase): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.input.encode
        builder ++= a.preState.encode
        builder ++= a.postState.encode
        builder.result()

    given Decoder[StatCase] =
      Decoder.instance { cursor =>
        for
          input <- cursor.get[StatInput]("input")
          preState <- cursor.get[StatState]("pre_state")
          output <- cursor.get[Option[StatOutput]]("output")
          postState <- cursor.get[StatState]("post_state")
        yield StatCase(input, preState, output, postState)
      }
