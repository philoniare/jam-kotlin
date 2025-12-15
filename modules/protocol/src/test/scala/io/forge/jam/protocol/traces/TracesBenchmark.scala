package io.forge.jam.protocol.traces

import org.scalatest.funsuite.AnyFunSuite
import io.forge.jam.core.{ChainConfig, JamBytes}
import io.forge.jam.core.primitives.Hash
import io.forge.jam.core.types.block.Block
import io.forge.jam.core.types.header.Header
import io.forge.jam.protocol.TestFileLoader
import io.forge.jam.protocol.safrole.SafroleTypes.*
import io.forge.jam.protocol.safrole.SafroleTransition
import io.forge.jam.protocol.state.JamState
import io.circe.Decoder

// Import all JSON decoders from core types
import io.forge.jam.core.JamBytes.given
import io.forge.jam.core.primitives.Hash.given
import io.forge.jam.core.primitives.BandersnatchPublicKey.given
import io.forge.jam.core.primitives.Ed25519PublicKey.given
import io.forge.jam.core.primitives.Ed25519Signature.given
import io.forge.jam.core.types.tickets.{TicketEnvelope, TicketMark}
import io.forge.jam.core.types.tickets.TicketEnvelope.given
import io.forge.jam.core.types.tickets.TicketMark.given
import io.forge.jam.core.types.epoch.{EpochValidatorKey, EpochMark}
import io.forge.jam.core.types.epoch.EpochValidatorKey.given
import io.forge.jam.core.types.epoch.EpochMark.given
import io.forge.jam.core.types.work.{PackageSpec, Vote, ExecutionResult}
import io.forge.jam.core.types.work.PackageSpec.given
import io.forge.jam.core.types.work.Vote.given
import io.forge.jam.core.types.work.ExecutionResult.given
import io.forge.jam.core.types.dispute.{Culprit, Fault, GuaranteeSignature}
import io.forge.jam.core.types.dispute.Culprit.given
import io.forge.jam.core.types.dispute.Fault.given
import io.forge.jam.core.types.dispute.GuaranteeSignature.given
import io.forge.jam.core.types.context.Context
import io.forge.jam.core.types.context.Context.given
import io.forge.jam.core.types.workitem.{WorkItemImportSegment, WorkItemExtrinsic, WorkItem}
import io.forge.jam.core.types.workitem.WorkItemImportSegment.given
import io.forge.jam.core.types.workitem.WorkItemExtrinsic.given
import io.forge.jam.core.types.workitem.WorkItem.given
import io.forge.jam.core.types.workresult.{RefineLoad, WorkResult}
import io.forge.jam.core.types.workresult.RefineLoad.given
import io.forge.jam.core.types.workresult.WorkResult.given
import io.forge.jam.core.types.workpackage.{SegmentRootLookup, WorkPackage, WorkReport}
import io.forge.jam.core.types.workpackage.SegmentRootLookup.given
import io.forge.jam.core.types.workpackage.WorkPackage.given
import io.forge.jam.core.types.workpackage.WorkReport.given
import io.forge.jam.core.types.extrinsic.{Preimage, AssuranceExtrinsic, Verdict, Dispute, GuaranteeExtrinsic}
import io.forge.jam.core.types.extrinsic.Preimage.given
import io.forge.jam.core.types.extrinsic.AssuranceExtrinsic.given
import io.forge.jam.core.types.extrinsic.Verdict.given
import io.forge.jam.core.types.extrinsic.Dispute.given
import io.forge.jam.core.types.extrinsic.GuaranteeExtrinsic.given
import io.forge.jam.core.types.header.Header.given
import io.forge.jam.core.types.block.{Extrinsic => BlockExtrinsic}
import io.forge.jam.core.types.block.Extrinsic.given
import io.forge.jam.core.types.block.Block.given

/**
 * Standalone benchmark for JAM trace processing.
 *
 * Run with: sbt "protocol/testOnly io.forge.jam.protocol.traces.TracesBenchmark"
 *
 * Trace types:
 * - fallback: No work reports. No safrole (uses fallback key mode)
 * - safrole: No work reports. Safrole enabled (uses ticket-based sealing)
 * - storage: At most 5 storage-related work items per report. No Safrole.
 * - storage_light: Like storage but with at most 1 work item per report.
 */
class TracesBenchmark extends AnyFunSuite:
  import TracesBenchmarkApp.*

  test("benchmark all traces") {
    TracesBenchmarkApp.runBenchmark()
  }

object TracesBenchmarkApp:
  // Suppress all logging before any logger initialization
  System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "OFF")
  System.setProperty("logback.statusListenerClass", "ch.qos.logback.core.status.NopStatusListener")

  val config: ChainConfig = ChainConfig.TINY
  lazy val importer: BlockImporter = new BlockImporter(config)

  // Create decoders using the imported givens
  given Decoder[TraceStep] = TraceStep.decoder
  given Decoder[Genesis] = Genesis.decoder

  case class BenchmarkResult(
    traceName: String,
    totalSteps: Int,
    totalTimeMs: Long,
    avgTimePerStepMs: Double,
    stepsPerSecond: Double
  )

  private def suppressLogging(): Unit =
    val rootLogger = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    rootLogger match
      case logback: ch.qos.logback.classic.Logger =>
        logback.setLevel(ch.qos.logback.classic.Level.OFF)
      case _ => // ignore

  def runBenchmark(): Unit =
    suppressLogging()

    if !TestFileLoader.canLocateTestVectors then
      println("ERROR: Test vectors not available")
      return

    val traces = List("fallback", "safrole", "storage", "storage_light")

    // Warm-up run (first run is slower due to JIT)
    println("Warming up...")
    benchmarkTrace("fallback", warmup = true)

    // Actual benchmark
    val results = traces.flatMap(t => benchmarkTrace(t, warmup = false))

    println("\n" + "=" * 70)
    println("BENCHMARK RESULTS (Safrole STF only)")
    println("=" * 70)
    println(f"${"Trace"}%-15s ${"Steps"}%8s ${"Total(ms)"}%12s ${"Avg(ms)"}%12s ${"Steps/sec"}%12s")
    println("-" * 70)

    for result <- results do
      println(f"${result.traceName}%-15s ${result.totalSteps}%8d ${result.totalTimeMs}%12d ${result.avgTimePerStepMs}%12.2f ${result.stepsPerSecond}%12.2f")

    println("=" * 70)

  def benchmarkTrace(traceName: String, warmup: Boolean = false): Option[BenchmarkResult] =
    val stepsResult = TestFileLoader.getTraceStepFilenames(traceName)

    stepsResult match
      case Left(_) =>
        if !warmup then println(s"SKIP: $traceName trace not available")
        None
      case Right(stepNames) if stepNames.isEmpty =>
        if !warmup then println(s"SKIP: $traceName has no steps")
        None
      case Right(stepNames) =>
        if !warmup then print(s"$traceName: ")

        val startTime = System.nanoTime()
        var processedSteps = 0
        var errors = 0

        for stepName <- stepNames do
          val stepResult = TestFileLoader.loadJsonFromTestVectors[TraceStep](s"traces/$traceName", stepName)
          stepResult match
            case Left(_) =>
              errors += 1
            case Right(step) =>
              // Run Safrole STF (core state transition)
              val fullPreState = FullJamState.fromKeyvals(step.preState.keyvals, config)
              val jamState = JamState.fromFullJamState(fullPreState, config)
              val safroleInput = InputExtractor.extractSafroleInput(step.block)

              val (_, safroleOutput) = SafroleTransition.stf(safroleInput, jamState, config)

              if safroleOutput.isRight then
                processedSteps += 1
              else
                errors += 1

        val endTime = System.nanoTime()
        val totalTimeMs = (endTime - startTime) / 1_000_000
        val avgTimeMs = if processedSteps > 0 then totalTimeMs.toDouble / processedSteps else 0.0
        val stepsPerSec = if totalTimeMs > 0 then processedSteps.toDouble * 1000 / totalTimeMs else 0.0

        if !warmup then
          if errors > 0 then
            println(s"${processedSteps} steps, ${errors} errors, ${totalTimeMs}ms")
          else
            println(s"${processedSteps} steps, ${totalTimeMs}ms")

        Some(BenchmarkResult(
          traceName = traceName,
          totalSteps = processedSteps,
          totalTimeMs = totalTimeMs,
          avgTimePerStepMs = avgTimeMs,
          stepsPerSecond = stepsPerSec
        ))
