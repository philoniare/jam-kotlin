package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Ticket(
    val attempt: Int,
    val signature: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val attemptBytes = attempt.toLEBytes()
        val signatureBytes = signature
        return attemptBytes + signatureBytes
    }
}
