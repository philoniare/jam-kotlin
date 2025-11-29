package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Preimage(
    val requester: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val blob: JamByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val requesterBytes = encodeFixedWidthInteger(requester, 4, false)
        val blobBytes = encodeCompactInteger(blob.bytes.size.toLong()) + blob.bytes
        return requesterBytes + blobBytes
    }
}
