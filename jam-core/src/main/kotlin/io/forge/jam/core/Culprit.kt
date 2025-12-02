package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Culprit(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val target: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val key: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val signature: JamByteArray
) : Encodable {
    companion object {
        const val SIZE = 32 + 32 + 64 // target + key + signature

        fun fromBytes(data: ByteArray, offset: Int = 0): Culprit {
            val target = JamByteArray(data.copyOfRange(offset, offset + 32))
            val key = JamByteArray(data.copyOfRange(offset + 32, offset + 64))
            val signature = JamByteArray(data.copyOfRange(offset + 64, offset + 128))
            return Culprit(target, key, signature)
        }
    }

    override fun encode(): ByteArray {
        return target.bytes + key.bytes + signature.bytes
    }
}
