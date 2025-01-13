package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.encodeList
import kotlinx.serialization.Serializable

@Serializable
data class PreimageInput(
    val preimages: List<PreimageExtrinsic>,
    val slot: Long,
) : Encodable {
    override fun encode(): ByteArray {
        val guaranteesBytes = encodeList(preimages)
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        return guaranteesBytes + slotBytes
    }
}
