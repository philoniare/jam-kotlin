package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionResult(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val ok: JamByteArray? = null,
    val panic: Boolean? = null,
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<ExecutionResult, Int> {
            val tag = data[offset].toInt() and 0xFF
            return when (tag) {
                0 -> {
                    // Ok variant - followed by compact length + data
                    val (length, lengthBytes) = decodeCompactInteger(data, offset + 1)
                    val ok = JamByteArray(data.copyOfRange(offset + 1 + lengthBytes, offset + 1 + lengthBytes + length.toInt()))
                    Pair(ExecutionResult(ok = ok), 1 + lengthBytes + length.toInt())
                }
                2 -> {
                    // Panic variant
                    Pair(ExecutionResult(panic = true), 1)
                }
                else -> {
                    Pair(ExecutionResult(panic = true), 1)
                }
            }
        }
    }

    override fun encode(): ByteArray {
        if (ok != null) {
            val lengthBytes = encodeCompactInteger(ok.bytes.size.toLong())
            return byteArrayOf(0) + lengthBytes + ok.bytes
        } else {
            return byteArrayOf(2)
        }
    }
}
