package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Context(
    val anchor: ByteArray,
    val stateRoot: ByteArray,
    val beefyRoot: ByteArray,
    val lookupAnchor: ByteArray,
    val lookupAnchorSlot: Int,
    val prerequisite: ByteArray? // Assuming optional
) : Encodable {
    override fun encode(): ByteArray {
        val anchorBytes = anchor
        val stateRootBytes = stateRoot
        val beefyRootBytes = beefyRoot
        val lookupAnchorBytes = lookupAnchor
        val lookupAnchorSlotBytes = lookupAnchorSlot.toLEBytes()
        val prerequisiteBytes = prerequisite ?: byteArrayOf()
        return anchorBytes + stateRootBytes + beefyRootBytes + lookupAnchorBytes + lookupAnchorSlotBytes + prerequisiteBytes
    }
}
