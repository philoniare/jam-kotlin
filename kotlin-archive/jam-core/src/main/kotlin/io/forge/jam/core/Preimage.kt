package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Preimage(
    val requester: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val blob: JamByteArray
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<Preimage, Int> {
            val requester = decodeFixedWidthInteger(data, offset, 4, false)
            val (blobLength, blobLengthBytes) = decodeCompactInteger(data, offset + 4)
            val blob = JamByteArray(data.copyOfRange(offset + 4 + blobLengthBytes, offset + 4 + blobLengthBytes + blobLength.toInt()))
            return Pair(Preimage(requester, blob), 4 + blobLengthBytes + blobLength.toInt())
        }
    }

    override fun encode(): ByteArray {
        val requesterBytes = encodeFixedWidthInteger(requester, 4, false)
        val blobBytes = encodeCompactInteger(blob.bytes.size.toLong()) + blob.bytes
        return requesterBytes + blobBytes
    }
}
