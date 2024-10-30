package io.forge.jam.core

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionResult(
    @Serializable(with = ByteArrayHexSerializer::class)
    val ok: ByteArray? = null,
    val panic: Boolean? = null,
) : Encodable {
    override fun encode(): ByteArray {
        if (ok != null) {
            val lengthBytes = encodeFixedWidthInteger(ok.size, 1, false)
            return byteArrayOf(0) + lengthBytes + ok
        } else {
            return byteArrayOf(2)
        }
    }
}
