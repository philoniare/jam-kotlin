package io.forge.jam.safrole.assurance

import io.forge.jam.core.*
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceInput(
        val assurances: List<AssuranceExtrinsic>,
        val slot: Long,
        @Serializable(with = JamByteArrayHexSerializer::class) val parent: JamByteArray
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, coresCount: Int): Pair<AssuranceInput, Int> {
            var currentOffset = offset

            // assurances - compact length prefix + variable-size list
            val (assurancesLength, assurancesLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += assurancesLengthBytes
            val assurances = mutableListOf<AssuranceExtrinsic>()
            val assuranceSize = AssuranceExtrinsic.size(coresCount)
            for (i in 0 until assurancesLength.toInt()) {
                assurances.add(AssuranceExtrinsic.fromBytes(data, currentOffset, coresCount))
                currentOffset += assuranceSize
            }

            // slot - 4 bytes
            val slot = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // parent - 32 bytes
            val parent = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32

            return Pair(AssuranceInput(assurances, slot, parent), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        // AssurancesExtrinsic uses compact integer for length prefix
        val assurancesLengthBytes = encodeCompactInteger(assurances.size.toLong())
        val assurancesBytes = encodeList(assurances, includeLength = false)
        return assurancesLengthBytes + assurancesBytes + slotBytes + parent.bytes
    }
}
