package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SegmentRootLookup(
    @SerialName("work_package_hash")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val workPackageHash: JamByteArray,
    @SerialName("segment_tree_root")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val segmentTreeRoot: JamByteArray
) : Encodable {
    companion object {
        const val SIZE = 64 // 32 + 32

        fun fromBytes(data: ByteArray, offset: Int = 0): SegmentRootLookup {
            val workPackageHash = JamByteArray(data.copyOfRange(offset, offset + 32))
            val segmentTreeRoot = JamByteArray(data.copyOfRange(offset + 32, offset + 64))
            return SegmentRootLookup(workPackageHash, segmentTreeRoot)
        }
    }

    override fun encode(): ByteArray {
        return workPackageHash.bytes + segmentTreeRoot.bytes
    }
}
