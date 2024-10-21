package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val vote: Boolean,
    val index: Int,
    @Serializable(with = ByteArrayHexSerializer::class)
    val signature: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val voteByte = byteArrayOf(vote.toByte())
        val indexBytes = index.toLEBytes()
        val signatureBytes = signature
        return voteByte + indexBytes + signatureBytes
    }
}
