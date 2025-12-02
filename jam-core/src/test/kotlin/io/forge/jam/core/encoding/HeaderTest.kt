package io.forge.jam.core.encoding

import io.forge.jam.core.ChainConfig
import io.forge.jam.core.Header
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class HeaderTest {

    private fun testEncodeHeaderEpochMark(configPath: String) {
        val (inputHeader, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<Header>(configPath, "header_0")

        assertContentEquals(
            expectedOutputBytes,
            inputHeader.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testEncodeHeaderTicketsMark(configPath: String) {
        val (inputHeader, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<Header>(configPath, "header_1")

        assertContentEquals(
            expectedOutputBytes,
            inputHeader.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeHeaderEpochMark(configPath: String, config: ChainConfig) {
        val (inputHeader, binaryData) = TestFileLoader.loadTestDataFromTestVectors<Header>(configPath, "header_0")

        val (decodedHeader, bytesConsumed) = Header.fromBytes(
            binaryData,
            0,
            config.validatorCount,
            config.epochLength
        )

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputHeader.slot, decodedHeader.slot, "Slot mismatch")
        assertEquals(inputHeader.parent.toHex(), decodedHeader.parent.toHex(), "Parent mismatch")
        assertEquals(inputHeader.authorIndex, decodedHeader.authorIndex, "Author index mismatch")
        assertEquals(inputHeader.epochMark != null, decodedHeader.epochMark != null, "EpochMark presence mismatch")

        assertContentEquals(binaryData, decodedHeader.encode(), "Round-trip encoding mismatch")
    }

    private fun testDecodeHeaderTicketsMark(configPath: String, config: ChainConfig) {
        val (inputHeader, binaryData) = TestFileLoader.loadTestDataFromTestVectors<Header>(configPath, "header_1")

        val (decodedHeader, bytesConsumed) = Header.fromBytes(
            binaryData,
            0,
            config.validatorCount,
            config.epochLength
        )

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputHeader.slot, decodedHeader.slot, "Slot mismatch")
        assertEquals(inputHeader.ticketsMark != null, decodedHeader.ticketsMark != null, "TicketsMark presence mismatch")

        assertContentEquals(binaryData, decodedHeader.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeHeaderEpochMarkTiny() {
        testEncodeHeaderEpochMark("codec/tiny")
    }

    @Test
    fun testEncodeHeaderEpochMarkFull() {
        testEncodeHeaderEpochMark("codec/full")
    }

    @Test
    fun testEncodeHeaderTicketsMarkTiny() {
        testEncodeHeaderTicketsMark("codec/tiny")
    }

    @Test
    fun testEncodeHeaderTicketsMarkFull() {
        testEncodeHeaderTicketsMark("codec/full")
    }

    @Test
    fun testDecodeHeaderEpochMarkTiny() {
        testDecodeHeaderEpochMark("codec/tiny", ChainConfig.TINY)
    }

    @Test
    fun testDecodeHeaderEpochMarkFull() {
        testDecodeHeaderEpochMark("codec/full", ChainConfig.FULL)
    }

    @Test
    fun testDecodeHeaderTicketsMarkTiny() {
        testDecodeHeaderTicketsMark("codec/tiny", ChainConfig.TINY)
    }

    @Test
    fun testDecodeHeaderTicketsMarkFull() {
        testDecodeHeaderTicketsMark("codec/full", ChainConfig.FULL)
    }
}
