package io.forge.jam.core

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SegmentRootLookup(
    @SerialName("work_package_hash")
    @Serializable(with = ByteArrayHexSerializer::class)
    val workPackageHash: ByteArray,
    @SerialName("segmentTreeRoot")
    @Serializable(with = ByteArrayHexSerializer::class)
    val segmentTreeRoot: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        return workPackageHash + segmentTreeRoot
    }
}
