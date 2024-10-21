package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Preimage(
    val requester: Int,
    val blob: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val requesterBytes = requester.toLEBytes()
        val blobLengthBytes = blob.size.toLEBytes()
        val blobBytes = blob
        return requesterBytes + blobLengthBytes + blobBytes
    }
}
