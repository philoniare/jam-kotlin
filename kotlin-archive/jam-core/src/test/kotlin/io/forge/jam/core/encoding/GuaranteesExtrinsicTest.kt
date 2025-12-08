package io.forge.jam.core.encoding

import io.forge.jam.core.GuaranteeExtrinsic
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GuaranteesExtrinsicTest {

    private fun testEncodeGuaranteesExtrinsics(configPath: String) {
        val (inputGuarantees, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<List<GuaranteeExtrinsic>>(configPath, "guarantees_extrinsic")

        val encodedGuarantees = inputGuarantees.map { guarantee ->
            val extrinsic =
                GuaranteeExtrinsic(guarantee.report, guarantee.slot, guarantee.signatures)
            extrinsic.encode()
        }

        val versionByte = byteArrayOf(0x01.toByte())
        val concatenatedEncodedAssurances = versionByte + encodedGuarantees.reduce { acc, bytes -> acc + bytes }

        assertContentEquals(
            expectedOutputBytes,
            concatenatedEncodedAssurances,
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeGuaranteesExtrinsics(configPath: String) {
        val (inputGuarantees, _) = TestFileLoader.loadTestDataFromTestVectors<List<GuaranteeExtrinsic>>(configPath, "guarantees_extrinsic")

        // Test each guarantee individually
        for (inputGuarantee in inputGuarantees) {
            val encoded = inputGuarantee.encode()
            val (decodedGuarantee, bytesConsumed) = GuaranteeExtrinsic.fromBytes(encoded, 0)

            assertEquals(encoded.size, bytesConsumed, "Bytes consumed should match encoded size")
            assertEquals(inputGuarantee.slot, decodedGuarantee.slot, "Slot mismatch")
            assertEquals(inputGuarantee.signatures.size, decodedGuarantee.signatures.size, "Signatures count mismatch")
            assertEquals(inputGuarantee.report.packageSpec.hash.toHex(), decodedGuarantee.report.packageSpec.hash.toHex(), "Report package spec hash mismatch")

            assertContentEquals(encoded, decodedGuarantee.encode(), "Round-trip encoding mismatch")
        }
    }

    @Test
    fun testEncodeGuaranteesExtrinsicsTiny() {
        testEncodeGuaranteesExtrinsics("codec/tiny")
    }

    @Test
    fun testEncodeGuaranteesExtrinsicsFull() {
        testEncodeGuaranteesExtrinsics("codec/full")
    }

    @Test
    fun testDecodeGuaranteesExtrinsicsTiny() {
        testDecodeGuaranteesExtrinsics("codec/tiny")
    }

    @Test
    fun testDecodeGuaranteesExtrinsicsFull() {
        testDecodeGuaranteesExtrinsics("codec/full")
    }
}
