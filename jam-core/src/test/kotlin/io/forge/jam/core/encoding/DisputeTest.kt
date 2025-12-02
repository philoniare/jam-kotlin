package io.forge.jam.core.encoding

import io.forge.jam.core.ChainConfig
import io.forge.jam.core.Dispute
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DisputeTest {

    private fun testEncodeDispute(configPath: String) {
        val (inputDispute, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<Dispute>(configPath, "disputes_extrinsic")

        val encodedDispute = inputDispute.encode()

        assertContentEquals(
            expectedOutputBytes,
            encodedDispute,
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeDispute(configPath: String, config: ChainConfig) {
        val (inputDispute, binaryData) = TestFileLoader.loadTestDataFromTestVectors<Dispute>(configPath, "disputes_extrinsic")

        val (decodedDispute, bytesConsumed) = Dispute.fromBytes(binaryData, 0, config.votesPerVerdict)

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputDispute.verdicts.size, decodedDispute.verdicts.size, "Verdicts count mismatch")
        assertEquals(inputDispute.culprits.size, decodedDispute.culprits.size, "Culprits count mismatch")
        assertEquals(inputDispute.faults.size, decodedDispute.faults.size, "Faults count mismatch")

        assertContentEquals(binaryData, decodedDispute.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeDisputeTiny() {
        testEncodeDispute("codec/tiny")
    }

    @Test
    fun testEncodeDisputeFull() {
        testEncodeDispute("codec/full")
    }

    @Test
    fun testDecodeDisputeTiny() {
        testDecodeDispute("codec/tiny", ChainConfig.TINY)
    }

    @Test
    fun testDecodeDisputeFull() {
        testDecodeDispute("codec/full", ChainConfig.FULL)
    }
}
