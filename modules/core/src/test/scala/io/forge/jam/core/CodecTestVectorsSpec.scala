package io.forge.jam.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.Decoder
import io.circe.parser.*
import io.forge.jam.core.json.decoders.given
import io.forge.jam.core.json.simpletypes.given
import io.forge.jam.core.json.complextypes.given
import io.forge.jam.core.codec.{JamEncoder, JamDecoder}
import io.forge.jam.core.types.header.Header
import io.forge.jam.core.types.workpackage.{WorkPackage, WorkReport}
import io.forge.jam.core.types.workitem.WorkItem
import io.forge.jam.core.types.workresult.WorkResult
import io.forge.jam.core.types.context.Context
import io.forge.jam.core.types.block.{Block, Extrinsic}
import io.forge.jam.core.types.extrinsic.{AssuranceExtrinsic, Preimage, GuaranteeExtrinsic, Dispute}
import io.forge.jam.core.types.tickets.TicketEnvelope
import scala.io.Source
import java.nio.file.{Files, Paths}

class CodecTestVectorsSpec extends AnyFlatSpec with Matchers:

  private val basePath = "jamtestvectors/codec"

  // Test case with both encoder and decoder
  private case class TestCase[A](
    name: String,
    jsonDecoder: Decoder[A],
    encoder: JamEncoder[A],
    binaryDecoder: ChainConfig => JamDecoder[A]
  )

  // List-based extrinsic test cases with version byte prefix
  private case class ListTestCase[A](
    name: String,
    versionByte: Byte,
    jsonDecoder: Decoder[List[A]],
    encoder: JamEncoder[A],
    binaryDecoder: ChainConfig => JamDecoder[A]
  )

  private val testCases: List[TestCase[?]] = List(
    TestCase("header_0", summon[Decoder[Header]], summon[JamEncoder[Header]], c => Header.decoder(c)),
    TestCase("header_1", summon[Decoder[Header]], summon[JamEncoder[Header]], c => Header.decoder(c)),
    TestCase("refine_context", summon[Decoder[Context]], summon[JamEncoder[Context]], _ => summon[JamDecoder[Context]]),
    TestCase("work_item", summon[Decoder[WorkItem]], summon[JamEncoder[WorkItem]], _ => summon[JamDecoder[WorkItem]]),
    TestCase("work_result_0", summon[Decoder[WorkResult]], summon[JamEncoder[WorkResult]], _ => summon[JamDecoder[WorkResult]]),
    TestCase("work_result_1", summon[Decoder[WorkResult]], summon[JamEncoder[WorkResult]], _ => summon[JamDecoder[WorkResult]]),
    TestCase("work_package", summon[Decoder[WorkPackage]], summon[JamEncoder[WorkPackage]], _ => summon[JamDecoder[WorkPackage]]),
    TestCase("work_report", summon[Decoder[WorkReport]], summon[JamEncoder[WorkReport]], _ => summon[JamDecoder[WorkReport]]),
    TestCase("extrinsic", summon[Decoder[Extrinsic]], summon[JamEncoder[Extrinsic]], c => Extrinsic.decoder(c)),
    TestCase("block", summon[Decoder[Block]], summon[JamEncoder[Block]], c => Block.decoder(c)),
    TestCase("disputes_extrinsic", summon[Decoder[Dispute]], summon[JamEncoder[Dispute]], c => Dispute.decoder(c.votesPerVerdict))
  )

  private val listTestCases: List[ListTestCase[?]] = List(
    ListTestCase("assurances_extrinsic", 0x02, summon[Decoder[List[AssuranceExtrinsic]]], summon[JamEncoder[AssuranceExtrinsic]], c => AssuranceExtrinsic.decoder(c.coresCount)),
    ListTestCase("tickets_extrinsic", 0x03, summon[Decoder[List[TicketEnvelope]]], summon[JamEncoder[TicketEnvelope]], _ => summon[JamDecoder[TicketEnvelope]]),
    ListTestCase("preimages_extrinsic", 0x03, summon[Decoder[List[Preimage]]], summon[JamEncoder[Preimage]], _ => summon[JamDecoder[Preimage]]),
    ListTestCase("guarantees_extrinsic", 0x01, summon[Decoder[List[GuaranteeExtrinsic]]], summon[JamEncoder[GuaranteeExtrinsic]], _ => summon[JamDecoder[GuaranteeExtrinsic]])
  )

  private def loadJson(path: String): String =
    val source = Source.fromFile(path)
    try source.mkString
    finally source.close()

  private def loadBinary(path: String): JamBytes =
    JamBytes(Files.readAllBytes(Paths.get(path)))

  private def runTestsForConfig(configName: String): Unit =
    val config = if configName == "tiny" then ChainConfig.TINY else ChainConfig.FULL
    val configPath = s"$basePath/$configName"
    testCases.foreach { tc =>
      runTest(configPath, config, tc)
    }
    listTestCases.foreach { tc =>
      runListTest(configPath, config, tc)
    }

  private def runTest[A](configPath: String, config: ChainConfig, tc: TestCase[A]): Unit =
    val jsonFile = s"$configPath/${tc.name}.json"
    val binFile = s"$configPath/${tc.name}.bin"
    val json = loadJson(jsonFile)
    val expectedBinary = loadBinary(binFile)
    val decoder = tc.binaryDecoder(config)

    // Test 1: JSON -> encode -> matches .bin
    decode[A](json)(using tc.jsonDecoder) match
      case Right(parsed) =>
        val encoded = tc.encoder.encode(parsed)
        withClue(s"Encoding mismatch for ${tc.name}:\nExpected ${expectedBinary.length} bytes, got ${encoded.length} bytes\n") {
          encoded shouldBe expectedBinary
        }

        // Test 2: .bin -> decode -> should equal JSON-decoded value
        val (decoded, bytesConsumed) = decoder.decode(expectedBinary, 0)
        withClue(s"Decode consumed wrong number of bytes for ${tc.name}:\n") {
          bytesConsumed shouldBe expectedBinary.length
        }
        withClue(s"JSON-decoded != binary-decoded for ${tc.name}:\n") {
          decoded shouldBe parsed
        }

      case Left(error) =>
        fail(s"Failed to parse $jsonFile: $error")

  private def runListTest[A](configPath: String, config: ChainConfig, tc: ListTestCase[A]): Unit =
    val jsonFile = s"$configPath/${tc.name}.json"
    val binFile = s"$configPath/${tc.name}.bin"
    val json = loadJson(jsonFile)
    val expectedBinary = loadBinary(binFile)
    val decoder = tc.binaryDecoder(config)

    decode[List[A]](json)(using tc.jsonDecoder) match
      case Right(items) =>
        // Test 1: JSON -> encode -> matches .bin
        val encodedItems = items.map(tc.encoder.encode)
        val concatenated = encodedItems.foldLeft(JamBytes.empty)(_ ++ _)
        val encoded = JamBytes(Array(tc.versionByte)) ++ concatenated
        withClue(s"Encoding mismatch for ${tc.name}:\nExpected ${expectedBinary.length} bytes, got ${encoded.length} bytes\n") {
          encoded shouldBe expectedBinary
        }

        // Test 2: .bin -> decode each item -> should equal JSON-decoded items
        var pos = 1 // skip version byte
        val decodedItems = items.indices.map { _ =>
          val (item, consumed) = decoder.decode(expectedBinary, pos)
          pos += consumed
          item
        }.toList
        withClue(s"JSON-decoded != binary-decoded for ${tc.name}:\n") {
          decodedItems shouldBe items
        }

      case Left(error) =>
        fail(s"Failed to parse $jsonFile: $error")

  "Codec TINY" should "encode and decode all test vectors correctly" in {
    runTestsForConfig("tiny")
  }

  "Codec FULL" should "encode and decode all test vectors correctly" in {
    runTestsForConfig("full")
  }
