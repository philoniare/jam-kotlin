package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuaranteeSignature(
    @SerialName("validator_index")
    val validatorIndex: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val signature: JamByteArray
) : Encodable {
    companion object {
        const val SIGNATURE_SIZE = 64 // Ed25519 signature
        const val SIZE = 2 + SIGNATURE_SIZE

        fun fromBytes(data: ByteArray, offset: Int = 0): GuaranteeSignature {
            val validatorIndex = decodeFixedWidthInteger(data, offset, 2, false)
            val signature = JamByteArray(data.copyOfRange(offset + 2, offset + 2 + SIGNATURE_SIZE))
            return GuaranteeSignature(validatorIndex, signature)
        }
    }

    override fun encode(): ByteArray {
        val validatorIndexBytes = encodeFixedWidthInteger(validatorIndex, 2, false)
        val signatureBytes = signature.bytes
        return validatorIndexBytes + signatureBytes
    }
}
