package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkItemImportSegment(
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("tree_root")
    val treeRoot: JamByteArray,
    val index: Long
) : Encodable {
    companion object {
        const val SIZE = 32 + 2 // 32-byte tree root + 2-byte index

        fun fromBytes(data: ByteArray, offset: Int = 0): WorkItemImportSegment {
            val treeRoot = JamByteArray(data.copyOfRange(offset, offset + 32))
            val index = decodeFixedWidthInteger(data, offset + 32, 2, false)
            return WorkItemImportSegment(treeRoot, index)
        }
    }

    override fun encode(): ByteArray {
        val indexBytes = encodeFixedWidthInteger(index, 2, false)
        return treeRoot.bytes + indexBytes
    }
}
