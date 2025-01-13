package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class PreimageHistoryKey(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val hash: JamByteArray,
    val length: Long
) : Encodable {
    override fun encode(): ByteArray {
        val lengthBytes = encodeFixedWidthInteger(length, 4, false)
        return hash.bytes + lengthBytes
    }
}
