package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

/**
 * Short-form validator key used in EpochMark.
 * Contains only bandersnatch and ed25519 keys.
 */
@Serializable
data class EpochValidatorKey(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val bandersnatch: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val ed25519: JamByteArray
) : Encodable {
    companion object {
        const val SIZE = 64 // 32 + 32

        fun fromBytes(data: ByteArray, offset: Int = 0): EpochValidatorKey {
            val bandersnatch = JamByteArray(data.copyOfRange(offset, offset + 32))
            val ed25519 = JamByteArray(data.copyOfRange(offset + 32, offset + 64))
            return EpochValidatorKey(bandersnatch, ed25519)
        }
    }

    override fun encode(): ByteArray {
        return bandersnatch.bytes + ed25519.bytes
    }
}
