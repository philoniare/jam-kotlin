package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Culprit(
    @Serializable(with = ByteArrayHexSerializer::class)
    val target: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    val key: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    val signature: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        return target + key + signature
    }
}
