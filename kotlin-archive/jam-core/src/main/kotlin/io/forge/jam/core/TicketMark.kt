package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class TicketMark(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val id: JamByteArray,
    val attempt: Long,
) : Encodable {
    companion object {
        const val SIZE = 33 // 32 bytes id + 1 byte attempt

        fun fromBytes(data: ByteArray, offset: Int = 0): TicketMark {
            val id = JamByteArray(data.copyOfRange(offset, offset + 32))
            val attempt = decodeFixedWidthInteger(data, offset + 32, 1, false)
            return TicketMark(id, attempt)
        }
    }

    override fun encode(): ByteArray {
        val attemptBytes = encodeFixedWidthInteger(attempt, 1, false)
        return id.bytes + attemptBytes
    }
}

