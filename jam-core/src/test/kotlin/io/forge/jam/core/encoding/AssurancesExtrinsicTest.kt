package io.forge.jam.core.encoding

import io.forge.jam.core.AssuranceExtrinsic
import io.forge.jam.core.ChainConfig
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class AssuranceExtrinsicTest {

    private fun testEncodeAssuranceExtrinsics(configPath: String) {
        val (inputAssurances, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<List<AssuranceExtrinsic>>(configPath, "assurances_extrinsic")

        // Process each assurance
        val encodedAssurances = inputAssurances.map { assurance ->
            val extrinsic =
                AssuranceExtrinsic(assurance.anchor, assurance.bitfield, assurance.validatorIndex, assurance.signature)
            extrinsic.encode()
        }

        // Version byte
        val versionByte = byteArrayOf(0x02.toByte())

        // Concatenate all encoded assurances
        val concatenatedEncodedAssurances = versionByte + encodedAssurances.reduce { acc, bytes -> acc + bytes }

        // Compare the concatenated encoded bytes with the expected output bytes
        assertContentEquals(
            expectedOutputBytes,
            concatenatedEncodedAssurances,
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeAssuranceExtrinsics(configPath: String, config: ChainConfig) {
        val (inputAssurances, _) = TestFileLoader.loadTestDataFromTestVectors<List<AssuranceExtrinsic>>(configPath, "assurances_extrinsic")

        // Test each assurance individually
        for (inputAssurance in inputAssurances) {
            val encoded = inputAssurance.encode()
            val decodedAssurance = AssuranceExtrinsic.fromBytes(encoded, 0, config.coresCount)

            assertEquals(inputAssurance.anchor.toHex(), decodedAssurance.anchor.toHex(), "Anchor mismatch")
            assertEquals(inputAssurance.bitfield.toHex(), decodedAssurance.bitfield.toHex(), "Bitfield mismatch")
            assertEquals(inputAssurance.validatorIndex, decodedAssurance.validatorIndex, "Validator index mismatch")
            assertEquals(inputAssurance.signature.toHex(), decodedAssurance.signature.toHex(), "Signature mismatch")

            assertContentEquals(encoded, decodedAssurance.encode(), "Round-trip encoding mismatch")
        }
    }

    @Test
    fun testEncodeAssuranceExtrinsicsTiny() {
        testEncodeAssuranceExtrinsics("codec/tiny")
    }

    @Test
    fun testEncodeAssuranceExtrinsicsFull() {
        testEncodeAssuranceExtrinsics("codec/full")
    }

    @Test
    fun testDecodeAssuranceExtrinsicsTiny() {
        testDecodeAssuranceExtrinsics("codec/tiny", ChainConfig.TINY)
    }

    @Test
    fun testDecodeAssuranceExtrinsicsFull() {
        testDecodeAssuranceExtrinsics("codec/full", ChainConfig.FULL)
    }
}
