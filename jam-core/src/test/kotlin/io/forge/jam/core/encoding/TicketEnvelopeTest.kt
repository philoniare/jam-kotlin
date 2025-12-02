package io.forge.jam.core.encoding

import io.forge.jam.core.TicketEnvelope
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TicketEnvelopeTest {

    private fun testEncodeTicket(configPath: String) {
        val (inputTickets, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<List<TicketEnvelope>>(configPath, "tickets_extrinsic")

        val encodedTickets = inputTickets.map { ticket ->
            val extrinsic =
                TicketEnvelope(ticket.attempt, ticket.signature)
            extrinsic.encode()
        }

        val versionByte = byteArrayOf(0x03)
        val concatenatedEncodedTickets = versionByte + encodedTickets.reduce { acc, bytes -> acc + bytes }

        assertContentEquals(
            expectedOutputBytes,
            concatenatedEncodedTickets,
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeTicket(configPath: String) {
        val (inputTickets, _) = TestFileLoader.loadTestDataFromTestVectors<List<TicketEnvelope>>(configPath, "tickets_extrinsic")

        // Test each ticket individually
        for (inputTicket in inputTickets) {
            val encoded = inputTicket.encode()
            val decodedTicket = TicketEnvelope.fromBytes(encoded, 0)

            assertEquals(inputTicket.attempt, decodedTicket.attempt, "Attempt mismatch")
            assertEquals(inputTicket.signature.toHex(), decodedTicket.signature.toHex(), "Signature mismatch")

            assertContentEquals(encoded, decodedTicket.encode(), "Round-trip encoding mismatch")
        }
    }

    @Test
    fun testEncodeTicketTiny() {
        testEncodeTicket("codec/tiny")
    }

    @Test
    fun testEncodeTicketFull() {
        testEncodeTicket("codec/full")
    }

    @Test
    fun testDecodeTicketTiny() {
        testDecodeTicket("codec/tiny")
    }

    @Test
    fun testDecodeTicketFull() {
        testDecodeTicket("codec/full")
    }
}
