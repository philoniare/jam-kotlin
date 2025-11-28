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
    override fun encode(): ByteArray {
        return bandersnatch.bytes + ed25519.bytes
    }
}
