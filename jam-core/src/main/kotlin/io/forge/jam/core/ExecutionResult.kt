package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
sealed class ExecutionResult : Encodable {
    data class Ok(val data: ByteArray) : ExecutionResult() {
        override fun encode(): ByteArray {
            val tag = byteArrayOf(0x00)
            val dataLengthBytes = data.size.toLEBytes()
            val dataBytes = data
            return tag + dataLengthBytes + dataBytes
        }
    }

    object Panic : ExecutionResult() {
        override fun encode(): ByteArray {
            val tag = byteArrayOf(0x01)
            return tag
        }
    }
}
