package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val vote: Boolean,
    val index: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val signature: JamByteArray
) : Encodable {
    companion object {
        const val SIGNATURE_SIZE = 64 // Ed25519 signature
        const val SIZE = 1 + 2 + SIGNATURE_SIZE // vote + index + signature

        fun fromBytes(data: ByteArray, offset: Int = 0): Vote {
            val vote = data[offset].toInt() != 0
            val index = decodeFixedWidthInteger(data, offset + 1, 2, false)
            val signature = JamByteArray(data.copyOfRange(offset + 3, offset + 3 + SIGNATURE_SIZE))
            return Vote(vote, index, signature)
        }
    }

    override fun encode(): ByteArray {
        val voteByte = byteArrayOf(if (vote) 1.toByte() else 0.toByte())
        val indexBytes = encodeFixedWidthInteger(index, 2, false)
        val signatureBytes = signature.bytes
        return voteByte + indexBytes + signatureBytes
    }
}
