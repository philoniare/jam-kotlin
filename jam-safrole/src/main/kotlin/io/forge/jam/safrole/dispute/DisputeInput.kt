package io.forge.jam.safrole.safrole

import io.forge.jam.core.Dispute
import io.forge.jam.core.Encodable
import kotlinx.serialization.Serializable

@Serializable
data class DisputeInput(
    val disputes: Dispute? = null
) : Encodable {
    override fun encode(): ByteArray {
        val disputesBytes = disputes?.encode() ?: byteArrayOf(0)
        return disputesBytes
    }
}
