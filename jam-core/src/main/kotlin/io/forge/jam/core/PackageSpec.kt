package io.forge.jam.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageSpec(
    @Serializable(with = ByteArrayHexSerializer::class)
    val hash: ByteArray,
    val len: Long,
    @Serializable(with = ByteArrayHexSerializer::class)
    @SerialName("erasure_root")
    val erasureRoot: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    @SerialName("exports_root")
    val exportsRoot: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val hashBytes = hash
        val lenBytes = encodeFixedWidthInteger(len, 4, false)
        val erasureRootBytes = erasureRoot
        val exportsRootBytes = exportsRoot
        return hashBytes + lenBytes + erasureRootBytes + exportsRootBytes
    }
}
