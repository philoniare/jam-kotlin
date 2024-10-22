package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class TicketMark(
    @Serializable(with = ByteArrayHexSerializer::class)
    val id: ByteArray,
    val attempt: Long,
) : Encodable {
    override fun encode(): ByteArray {
        val attemptBytes = encodeFixedWidthInteger(attempt, 1, false)
        return id + attemptBytes
    }
}

