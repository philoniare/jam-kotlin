package io.forge.jam.safrole.safrole

import io.forge.jam.core.*
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SafroleInput(
    val slot: Long? = null,

    @Serializable(with = JamByteArrayHexSerializer::class)
    val entropy: JamByteArray = JamByteArray(byteArrayOf(0)),
    val extrinsic: List<TicketEnvelope> = emptyList(),

    val disputes: Dispute? = null
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<SafroleInput, Int> {
            var currentOffset = offset

            // slot - 4 bytes (always present in binary, 0 = null semantically)
            val slot = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // entropy - 32 bytes
            val entropy = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32

            // extrinsic - variable length list of fixed-size TicketEnvelope
            val (extrinsicLength, extrinsicLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += extrinsicLengthBytes
            val extrinsic = decodeFixedList(data, currentOffset, extrinsicLength.toInt(), TicketEnvelope.SIZE) { d, o ->
                TicketEnvelope.fromBytes(d, o)
            }
            currentOffset += extrinsicLength.toInt() * TicketEnvelope.SIZE

            return Pair(SafroleInput(slot, entropy, extrinsic, null), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val slotBytes = slot?.let { encodeFixedWidthInteger(it, 4, false) } ?: byteArrayOf(0)
        val entropyBytes = entropy.bytes
        val extrinsicBytes = encodeListWithCompactLength(extrinsic)
        return slotBytes + entropyBytes + extrinsicBytes
    }
}
