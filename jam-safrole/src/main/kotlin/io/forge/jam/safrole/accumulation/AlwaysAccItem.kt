package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.decodeFixedWidthInteger
import io.forge.jam.core.encodeFixedWidthInteger
import kotlinx.serialization.Serializable

@Serializable
data class AlwaysAccItem(
    val id: Long,
    val gas: Long,
) : Encodable {
    companion object {
        const val SIZE = 12 // 4 bytes for id + 8 bytes for gas

        fun fromBytes(data: ByteArray, offset: Int = 0): AlwaysAccItem {
            val id = decodeFixedWidthInteger(data, offset, 4, false)
            val gas = decodeFixedWidthInteger(data, offset + 4, 8, false)
            return AlwaysAccItem(id, gas)
        }
    }

    override fun encode(): ByteArray {
        val serviceIdBytes = encodeFixedWidthInteger(id, 4, false)
        val gasBytes = encodeFixedWidthInteger(gas, 8, false)
        return serviceIdBytes + gasBytes
    }
}
