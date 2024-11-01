package io.forge.jam.safrole

import io.forge.jam.core.EpochMark
import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import io.forge.jam.core.toHex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutputMarks(
    @SerialName("epoch_mark")
    val epochMark: EpochMark? = null,
    @SerialName("tickets_mark")
    val ticketsMark: List<TicketBody>? = null,
    @SerialName("offenders_mark")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val offendersMark: List<ByteArray>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutputMarks) return false
        return epochMark == other.epochMark && ticketsMark == other.ticketsMark && offendersMark == other.offendersMark
    }

    override fun hashCode(): Int {
        var result = epochMark?.hashCode() ?: 0
        result = 31 * result + (ticketsMark?.hashCode() ?: 0)
        result = 31 * result + (offendersMark?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        val offendersMarkHex = offendersMark?.joinToString(", ") { it.toHex() } ?: "null"
        return "OutputMarks(epochMark=$epochMark, ticketsMark=$ticketsMark, offendersMark=[$offendersMarkHex])"
    }
}
