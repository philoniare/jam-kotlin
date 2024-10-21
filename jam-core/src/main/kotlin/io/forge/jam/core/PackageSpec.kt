package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class PackageSpec(
    val hash: ByteArray,
    val len: Int,
    val erasureRoot: ByteArray,
    val exportsRoot: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val hashBytes = hash
        val lenBytes = len.toLEBytes()
        val erasureRootBytes = erasureRoot
        val exportsRootBytes = exportsRoot
        return hashBytes + lenBytes + erasureRootBytes + exportsRootBytes
    }
}
