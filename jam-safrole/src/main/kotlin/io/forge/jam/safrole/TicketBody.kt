package io.forge.jam.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.serializers.ByteArrayHexSerializer
import io.forge.jam.core.toHex
import kotlinx.serialization.Serializable

@Serializable
data class TicketBody(
    @Serializable(with = ByteArrayHexSerializer::class)
    val id: ByteArray,
    val attempt: Long
) : Encodable {
    override fun toString(): String {
        return "TicketBody(" +
            "id=${id.toHex()}, " +
            "attempt=[${attempt}]" +
            ")"
    }

    override fun encode(): ByteArray {
        val attemptBytes = encodeFixedWidthInteger(attempt, 1, false)
        return id + attemptBytes
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
