package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Fault(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val target: JamByteArray,
    val vote: Boolean,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val key: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val signature: JamByteArray
) : Encodable {
    companion object {
        const val SIZE = 32 + 1 + 32 + 64 // target + vote + key + signature

        fun fromBytes(data: ByteArray, offset: Int = 0): Fault {
            val target = JamByteArray(data.copyOfRange(offset, offset + 32))
            val vote = data[offset + 32].toInt() != 0
            val key = JamByteArray(data.copyOfRange(offset + 33, offset + 65))
            val signature = JamByteArray(data.copyOfRange(offset + 65, offset + 129))
            return Fault(target, vote, key, signature)
        }
    }

    override fun encode(): ByteArray {
        val voteByte = byteArrayOf(vote.toByte())
        return target.bytes + voteByte + key.bytes + signature.bytes
    }
}
