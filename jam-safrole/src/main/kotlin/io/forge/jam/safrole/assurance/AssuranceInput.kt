package io.forge.jam.safrole.assurance

import io.forge.jam.core.*
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceInput(
    val assurances: List<AssuranceExtrinsic>,
    val slot: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val parent: JamByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        return encodeList(assurances) + slotBytes + parent.bytes
    }
}
