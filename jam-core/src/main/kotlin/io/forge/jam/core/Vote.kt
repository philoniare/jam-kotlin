package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Vote(
    val vote: Boolean,
    val index: Long,
    @Serializable(with = ByteArrayHexSerializer::class)
    val signature: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val voteByte = byteArrayOf(if (vote) 1.toByte() else 0.toByte())
        val indexBytes = encodeCompactInteger(index)
        val signatureBytes = signature
        return voteByte + indexBytes + signatureBytes
    }
}
