package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceExtrinsic(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val anchor: JamByteArray,        // 32-byte anchor (hash)
    @Serializable(with = JamByteArrayHexSerializer::class)
    val bitfield: JamByteArray,      // Bitfield indicating core assurance
    @SerialName("validator_index")
    val validatorIndex: Int,      // Validator index (encoded as two bytes)
    @Serializable(with = JamByteArrayHexSerializer::class)
    val signature: JamByteArray      // Ed25519 signature of the assurance
) : Encodable {
    companion object {
        const val SIGNATURE_SIZE = 64 // Ed25519 signature

        fun size(coresCount: Int): Int {
            val bitfieldSize = (coresCount + 7) / 8 // Round up to bytes
            return 32 + bitfieldSize + 2 + SIGNATURE_SIZE
        }

        fun fromBytes(data: ByteArray, offset: Int = 0, coresCount: Int): AssuranceExtrinsic {
            val anchor = JamByteArray(data.copyOfRange(offset, offset + 32))
            val bitfieldSize = (coresCount + 7) / 8
            val bitfield = JamByteArray(data.copyOfRange(offset + 32, offset + 32 + bitfieldSize))
            val validatorIndex = (data[offset + 32 + bitfieldSize].toInt() and 0xFF) or
                ((data[offset + 33 + bitfieldSize].toInt() and 0xFF) shl 8)
            val signature = JamByteArray(data.copyOfRange(offset + 34 + bitfieldSize, offset + 34 + bitfieldSize + SIGNATURE_SIZE))
            return AssuranceExtrinsic(anchor, bitfield, validatorIndex, signature)
        }
    }

    override fun encode(): ByteArray {
        // Encode validatorIndex as 2 bytes (little-endian)
        val validatorIndexBytes = ByteArray(2)
        validatorIndexBytes[0] = (validatorIndex and 0xFF).toByte()
        validatorIndexBytes[1] = ((validatorIndex shr 8) and 0xFF).toByte()

        // Concatenate anchor, bitfield, validatorIndexBytes, signature
        return anchor.bytes + bitfield.bytes + validatorIndexBytes + signature.bytes
    }
}
