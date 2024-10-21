package io.forge.jam.core

data class Header(
    val parent: ByteArray,
    val parentStateRoot: ByteArray,
    val extrinsicHash: ByteArray,
    val slot: Int,
    val epochMark: EpochMark,
    val ticketsMark: Any?,
    val offendersMark: List<ByteArray>,
    val authorIndex: Int,
    val entropySource: ByteArray,
    val seal: ByteArray
) {
    fun encode(): ByteArray {
        val parentBytes = parent
        val parentStateRootBytes = parentStateRoot
        val extrinsicHashBytes = extrinsicHash
        val slotBytes = slot.toLEBytes()
        val epochMarkBytes = epochMark.encode()
        val ticketsMarkBytes = byteArrayOf(0)
        val offendersMarkBytes = offendersMark.fold(byteArrayOf(offendersMark.size.toByte())) { acc, offender ->
            acc + offender
        }
        val authorIndexBytes = authorIndex.toLEBytes()
        val entropySourceBytes = entropySource
        val sealBytes = seal
        return parentBytes +
            parentStateRootBytes +
            extrinsicHashBytes +
            slotBytes +
            epochMarkBytes +
            ticketsMarkBytes +
            offendersMarkBytes +
            authorIndexBytes +
            entropySourceBytes +
            sealBytes
    }
}

