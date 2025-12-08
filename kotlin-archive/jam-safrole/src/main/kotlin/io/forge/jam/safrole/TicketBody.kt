package io.forge.jam.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class TicketBody(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val id: JamByteArray,
    val attempt: Long
) : Encodable {
    companion object {
        const val SIZE = 33 // 32 bytes id + 1 byte attempt

        fun fromBytes(data: ByteArray, offset: Int = 0): TicketBody {
            val id = JamByteArray(data.copyOfRange(offset, offset + 32))
            val attempt = data[offset + 32].toLong() and 0xFF
            return TicketBody(id, attempt)
        }
    }
    override fun toString(): String {
        return "TicketBody(" +
            "id=${id.toHex()}, " +
            "attempt=[${attempt}]" +
            ")"
    }

    override fun encode(): ByteArray {
        val attemptBytes = encodeFixedWidthInteger(attempt, 1, false)
        return id.bytes + attemptBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TicketBody) return false
        return id.contentEquals(other.id) && attempt == other.attempt
    }

    override fun hashCode(): Int {
        var result = id.contentHashCode()
        result = 31 * result + attempt.hashCode()
        return result
    }
}
