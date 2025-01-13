package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class PreimageExtrinsic(
    val requester: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val blob: JamByteArray,
) : Encodable {
    override fun encode(): ByteArray {
        val requesterBytes = encodeFixedWidthInteger(requester, 4, false)
        return requesterBytes + blob.bytes
    }
}

