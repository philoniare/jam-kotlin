package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeFixedWidthInteger
import kotlinx.serialization.Serializable

@Serializable
data class AlwaysAccItem(
    val id: Long,
    val gas: Long,
) : Encodable {
    override fun encode(): ByteArray {
        val serviceIdBytes = encodeFixedWidthInteger(id, 4, false)
        val gasBytes = encodeFixedWidthInteger(gas, 8, false)
        return serviceIdBytes + gasBytes
    }
}
