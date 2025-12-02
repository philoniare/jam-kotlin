package io.forge.jam.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.serializers.JamByteArrayListHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class TicketsOrKeys(
    @Serializable(with = JamByteArrayListHexSerializer::class)
    val keys: List<JamByteArray>? = null,
    val tickets: List<TicketBody>? = null
) : Encodable {


    companion object {
        fun fromKeys(keys: List<JamByteArray>) = TicketsOrKeys(keys = keys)
        fun fromTickets(tickets: List<TicketBody>) = TicketsOrKeys(tickets = tickets)

        /**
         * Decodes TicketsOrKeys from raw bytes.
         * Returns the decoded value and the number of bytes consumed.
         */
        fun fromBytes(data: ByteArray, offset: Int = 0, epochLength: Int): Pair<TicketsOrKeys, Int> {
            if (offset >= data.size) {
                return Pair(TicketsOrKeys(), 0)
            }

            val typeTag = data[offset].toInt() and 0xFF
            var currentOffset = offset + 1

            return when (typeTag) {
                0 -> {
                    // Tickets (Either.left): list of TicketBody
                    val tickets = mutableListOf<TicketBody>()
                    for (i in 0 until epochLength) {
                        if (currentOffset + TicketBody.SIZE <= data.size) {
                            tickets.add(TicketBody.fromBytes(data, currentOffset))
                            currentOffset += TicketBody.SIZE
                        }
                    }
                    Pair(fromTickets(tickets), currentOffset - offset)
                }
                1 -> {
                    // Keys (Either.right): list of 32-byte bandersnatch keys
                    val keys = mutableListOf<JamByteArray>()
                    for (i in 0 until epochLength) {
                        if (currentOffset + 32 <= data.size) {
                            keys.add(JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32)))
                            currentOffset += 32
                        }
                    }
                    Pair(fromKeys(keys), currentOffset - offset)
                }
                else -> Pair(TicketsOrKeys(), 1)
            }
        }
    }

    override fun toString(): String {
        val keyHex = keys?.joinToString(",\n") { it.toHex() } ?: "null"
        val ticketsString = tickets?.joinToString(",") ?: "null"
        return "TicketsOrKeys(keys=$keyHex, tickets=$ticketsString)"
    }

    override fun encode(): ByteArray {
        return if (keys != null) {
            val discriminator = byteArrayOf(1)
            val keysBytes = keys.fold(ByteArray(0)) { acc, byteArray -> acc + byteArray.bytes }
            discriminator + keysBytes
        } else if (tickets != null) {
            val discriminator = byteArrayOf(0)
            val ticketsBytes = tickets.fold(ByteArray(0)) { acc, ticketBody -> acc + ticketBody.encode() }
            discriminator + ticketsBytes
        } else {
            byteArrayOf(0) // Should not happen for valid TicketsOrKeys
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TicketsOrKeys) return false

        // Deep comparison for `keys`
        if (keys != null && other.keys != null) {
            if (keys.size != other.keys.size) return false
            for (i in keys.indices) {
                if (!keys[i].contentEquals(other.keys[i])) return false
            }
        } else if (keys != other.keys) {
            return false
        }

        // Use default equality for `tickets`
        return tickets == other.tickets
    }

    override fun hashCode(): Int {
        var result = keys?.fold(0) { acc, byteArray -> acc * 31 + byteArray.contentHashCode() } ?: 0
        result = 31 * result + (tickets?.hashCode() ?: 0)
        return result
    }
}
