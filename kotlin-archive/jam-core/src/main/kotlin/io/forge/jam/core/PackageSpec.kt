package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageSpec(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val hash: JamByteArray,
    val length: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("erasure_root")
    val erasureRoot: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("exports_root")
    val exportsRoot: JamByteArray,
    @SerialName("exports_count")
    val exportsCount: Long
) : Encodable {
    companion object {
        const val SIZE = 32 + 4 + 32 + 32 + 2 // hash + length + erasureRoot + exportsRoot + exportsCount

        fun fromBytes(data: ByteArray, offset: Int = 0): PackageSpec {
            val hash = JamByteArray(data.copyOfRange(offset, offset + 32))
            val length = decodeFixedWidthInteger(data, offset + 32, 4, false)
            val erasureRoot = JamByteArray(data.copyOfRange(offset + 36, offset + 68))
            val exportsRoot = JamByteArray(data.copyOfRange(offset + 68, offset + 100))
            val exportsCount = decodeFixedWidthInteger(data, offset + 100, 2, false)
            return PackageSpec(hash, length, erasureRoot, exportsRoot, exportsCount)
        }
    }
    override fun encode(): ByteArray {
        val hashBytes = hash.bytes
        val lenBytes = encodeFixedWidthInteger(length, 4, false)
        val erasureRootBytes = erasureRoot.bytes
        val exportsRootBytes = exportsRoot.bytes
        val exportsCountBytes = encodeFixedWidthInteger(exportsCount, 2, false)
        return hashBytes + lenBytes + erasureRootBytes + exportsRootBytes + exportsCountBytes
    }
}
