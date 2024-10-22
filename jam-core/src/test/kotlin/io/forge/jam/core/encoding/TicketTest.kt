package io.forge.jam.core.encoding

import io.forge.jam.core.Ticket
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TicketTest {
    @Test
    fun testEncodeTicket() {
        // Load JSON data from resources using the class loader
        val (inputTickets, expectedOutputBytes) = TestFileLoader.loadTestData<List<Ticket>>("tickets_extrinsic")

        val encodedTickets = inputTickets.map { ticket ->
            val extrinsic =
                Ticket(ticket.attempt, ticket.signature)
            extrinsic.encode()
        }

        // Process each ticket
        val versionByte = byteArrayOf(0x03)
        val concatenatedEncodedTickets = versionByte + encodedTickets.reduce { acc, bytes -> acc + bytes }

        // Compare the concatenated encoded bytes with the expected output bytes
        assertContentEquals(
            expectedOutputBytes,
            concatenatedEncodedTickets,
            "Encoded bytes do not match expected output"
        )
    }
}


