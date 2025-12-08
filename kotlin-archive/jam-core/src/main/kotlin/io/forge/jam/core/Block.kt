package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Block(
    val header: Header,
    val extrinsic: Extrinsic
) {
    companion object {
        fun fromBytes(
            data: ByteArray,
            offset: Int = 0,
            validatorCount: Int,
            epochLength: Int,
            coresCount: Int,
            votesPerVerdict: Int
        ): Pair<Block, Int> {
            var currentOffset = offset

            // header - variable size
            val (header, headerBytes) = Header.fromBytes(data, currentOffset, validatorCount, epochLength)
            currentOffset += headerBytes

            // extrinsic - variable size
            val (extrinsic, extrinsicBytes) = Extrinsic.fromBytes(data, currentOffset, coresCount, votesPerVerdict)
            currentOffset += extrinsicBytes

            return Pair(Block(header, extrinsic), currentOffset - offset)
        }
    }

    fun encode(): ByteArray {
        val headerBytes = header.encode()
        val extrinsicBytes = extrinsic.encode()
        return headerBytes + extrinsicBytes
    }
}
