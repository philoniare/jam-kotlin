package io.forge.jam.core.encoding

import io.forge.jam.core.Block
import io.forge.jam.core.ChainConfig
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BlockTest {

    private fun testEncodeBlock(configPath: String) {
        val (inputBlock, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<Block>(configPath, "block")

        assertContentEquals(
            expectedOutputBytes,
            inputBlock.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeBlock(configPath: String, config: ChainConfig) {
        val (inputBlock, binaryData) = TestFileLoader.loadTestDataFromTestVectors<Block>(configPath, "block")

        val (decodedBlock, bytesConsumed) = Block.fromBytes(
            binaryData,
            0,
            config.validatorCount,
            config.epochLength,
            config.coresCount,
            config.votesPerVerdict
        )

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputBlock.header.slot, decodedBlock.header.slot, "Slot mismatch")
        assertEquals(inputBlock.header.parent.toHex(), decodedBlock.header.parent.toHex(), "Parent mismatch")
        assertEquals(inputBlock.header.authorIndex, decodedBlock.header.authorIndex, "Author index mismatch")
        assertEquals(
            inputBlock.extrinsic.tickets.size,
            decodedBlock.extrinsic.tickets.size,
            "Tickets count mismatch"
        )

        // Verify round-trip: decoded block encodes back to original binary
        assertContentEquals(
            binaryData,
            decodedBlock.encode(),
            "Round-trip encoding mismatch"
        )
    }

    @Test
    fun testEncodeBlockTiny() {
        testEncodeBlock("codec/tiny")
    }

    @Test
    fun testEncodeBlockFull() {
        testEncodeBlock("codec/full")
    }

    @Test
    fun testDecodeBlockTiny() {
        testDecodeBlock("codec/tiny", ChainConfig.TINY)
    }

    @Test
    fun testDecodeBlockFull() {
        testDecodeBlock("codec/full", ChainConfig.FULL)
    }
}
