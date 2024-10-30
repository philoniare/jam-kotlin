package io.forge.jam.core

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Preimage(
    val requester: Long,
    @Serializable(with = ByteArrayHexSerializer::class)
    val blob: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val requesterBytes = encodeFixedWidthInteger(requester)
        val blobBytes = blob
        return requesterBytes + blobBytes
    }
}
