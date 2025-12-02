package io.forge.jam.core.encoding

import io.forge.jam.core.ChainConfig
import io.forge.jam.core.Extrinsic
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ExtrinsicTest {

    private fun testEncodeExtrinsic(configPath: String) {
        val (inputExtrinsic, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<Extrinsic>(configPath, "extrinsic")

        assertContentEquals(
            expectedOutputBytes,
            inputExtrinsic.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeExtrinsic(configPath: String, config: ChainConfig) {
        val (inputExtrinsic, binaryData) = TestFileLoader.loadTestDataFromTestVectors<Extrinsic>(configPath, "extrinsic")

        val (decodedExtrinsic, bytesConsumed) = Extrinsic.fromBytes(
            binaryData,
            0,
            config.coresCount,
            config.votesPerVerdict
        )

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputExtrinsic.tickets.size, decodedExtrinsic.tickets.size, "Tickets count mismatch")
        assertEquals(inputExtrinsic.preimages.size, decodedExtrinsic.preimages.size, "Preimages count mismatch")
        assertEquals(inputExtrinsic.guarantees.size, decodedExtrinsic.guarantees.size, "Guarantees count mismatch")
        assertEquals(inputExtrinsic.assurances.size, decodedExtrinsic.assurances.size, "Assurances count mismatch")

        assertContentEquals(binaryData, decodedExtrinsic.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeExtrinsicTiny() {
        testEncodeExtrinsic("codec/tiny")
    }

    @Test
    fun testEncodeExtrinsicFull() {
        testEncodeExtrinsic("codec/full")
    }

    @Test
    fun testDecodeExtrinsicTiny() {
        testDecodeExtrinsic("codec/tiny", ChainConfig.TINY)
    }

    @Test
    fun testDecodeExtrinsicFull() {
        testDecodeExtrinsic("codec/full", ChainConfig.FULL)
    }
}
