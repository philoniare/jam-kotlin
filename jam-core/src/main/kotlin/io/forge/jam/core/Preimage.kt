package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Preimage(
    val requester: Int,
    @Serializable(with = ByteArrayHexSerializer::class)
    val blob: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val requesterBytes = encodeCompactInteger(requester)
        val blobLengthBytes = encodeCompactInteger(blob.size)
        val blobBytes = blob
        return requesterBytes + blobLengthBytes + blobBytes
    }
}
