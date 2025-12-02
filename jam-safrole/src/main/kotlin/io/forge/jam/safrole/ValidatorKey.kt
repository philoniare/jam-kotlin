package io.forge.jam.safrole

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
class ValidatorKey(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val bandersnatch: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val ed25519: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val bls: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val metadata: JamByteArray
) : Encodable {
    companion object {
        const val SIZE = 336 // 32 + 32 + 144 + 128

        fun fromBytes(data: ByteArray, offset: Int = 0): ValidatorKey {
            val bandersnatch = JamByteArray(data.copyOfRange(offset, offset + 32))
            val ed25519 = JamByteArray(data.copyOfRange(offset + 32, offset + 64))
            val bls = JamByteArray(data.copyOfRange(offset + 64, offset + 208))
            val metadata = JamByteArray(data.copyOfRange(offset + 208, offset + 336))
            return ValidatorKey(bandersnatch, ed25519, bls, metadata)
        }
    }
    override fun encode(): ByteArray {
        return bandersnatch.bytes + ed25519.bytes + bls.bytes + metadata.bytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValidatorKey) return false
        return bandersnatch.contentEquals(other.bandersnatch) &&
            ed25519.contentEquals(other.ed25519) &&
            bls.contentEquals(other.bls) &&
            metadata.contentEquals(other.metadata)
    }

    override fun hashCode(): Int {
        var result = bandersnatch.contentHashCode()
        result = 31 * result + ed25519.contentHashCode()
        result = 31 * result + bls.contentHashCode()
        result = 31 * result + metadata.contentHashCode()
        return result
    }

    fun copy(): ValidatorKey {
        return ValidatorKey(
            bandersnatch = bandersnatch.clone(),
            ed25519 = ed25519.clone(),
            bls = bls.clone(),
            metadata = metadata.clone()
        )
    }
}
