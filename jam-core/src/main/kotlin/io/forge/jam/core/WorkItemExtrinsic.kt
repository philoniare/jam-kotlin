package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class WorkItemExtrinsic(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val hash: JamByteArray,
    val len: Long
) : Encodable {
    companion object {
        const val SIZE = 32 + 4 // 32-byte hash + 4-byte len

        fun fromBytes(data: ByteArray, offset: Int = 0): WorkItemExtrinsic {
            val hash = JamByteArray(data.copyOfRange(offset, offset + 32))
            val len = decodeFixedWidthInteger(data, offset + 32, 4, false)
            return WorkItemExtrinsic(hash, len)
        }
    }

    override fun encode(): ByteArray {
        val lenBytes = encodeFixedWidthInteger(len, 4, false)
        return hash.bytes + lenBytes
    }
}
