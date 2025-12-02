package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import io.forge.jam.core.serializers.JamByteArrayListHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Context(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val anchor: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("state_root")
    val stateRoot: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("beefy_root")
    val beefyRoot: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("lookup_anchor")
    val lookupAnchor: JamByteArray,
    @SerialName("lookup_anchor_slot")
    val lookupAnchorSlot: Long,
    @Serializable(with = JamByteArrayListHexSerializer::class)
    val prerequisites: List<JamByteArray>
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<Context, Int> {
            var currentOffset = offset
            val anchor = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val stateRoot = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val beefyRoot = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val lookupAnchor = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val lookupAnchorSlot = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // prerequisites - variable length list of 32-byte hashes
            val (prerequisitesLength, prerequisitesLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += prerequisitesLengthBytes
            val prerequisites = mutableListOf<JamByteArray>()
            for (i in 0 until prerequisitesLength.toInt()) {
                prerequisites.add(JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32)))
                currentOffset += 32
            }

            return Pair(Context(anchor, stateRoot, beefyRoot, lookupAnchor, lookupAnchorSlot, prerequisites), currentOffset - offset)
        }
    }
    override fun encode(): ByteArray {
        val anchorBytes = anchor.bytes
        val stateRootBytes = stateRoot.bytes
        val beefyRootBytes = beefyRoot.bytes
        val lookupAnchorBytes = lookupAnchor.bytes
        val lookupAnchorSlotBytes = encodeFixedWidthInteger(lookupAnchorSlot, 4, false)
        // prerequisites is SEQUENCE OF OpaqueHash - variable size, compact integer length
        val prerequisiteLengthBytes = encodeCompactInteger(prerequisites.size.toLong())
        val prerequisiteBytes = encodeList(prerequisites, includeLength = false)
        return anchorBytes + stateRootBytes + beefyRootBytes + lookupAnchorBytes + lookupAnchorSlotBytes + prerequisiteLengthBytes + prerequisiteBytes
    }
}
