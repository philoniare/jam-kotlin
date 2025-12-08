package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class TicketEnvelope(
    val attempt: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val signature: JamByteArray
) : Encodable {
    companion object {
        const val SIGNATURE_SIZE = 784 // Ring VRF signature size
        const val SIZE = 1 + SIGNATURE_SIZE // 1 byte attempt + signature

        fun fromBytes(data: ByteArray, offset: Int = 0): TicketEnvelope {
            val attempt = decodeFixedWidthInteger(data, offset, 1, false)
            val signature = JamByteArray(data.copyOfRange(offset + 1, offset + 1 + SIGNATURE_SIZE))
            return TicketEnvelope(attempt, signature)
        }
    }

    override fun encode(): ByteArray {
        val attemptBytes = encodeFixedWidthInteger(attempt, 1, false)
        val signatureBytes = signature.bytes
        return attemptBytes + signatureBytes
    }
}
