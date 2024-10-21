package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Fault(
    @Serializable(with = ByteArrayHexSerializer::class)
    val target: ByteArray,
    val vote: Boolean,
    @Serializable(with = ByteArrayHexSerializer::class)
    val key: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    val signature: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val voteByte = byteArrayOf(vote.toByte())
        return target + voteByte + key + signature
    }
}
