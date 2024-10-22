package io.forge.jam.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Header(
    @Serializable(with = ByteArrayHexSerializer::class)
    val parent: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    @SerialName("parent_state_root")
    val parentStateRoot: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    @SerialName("extrinsic_hash")
    val extrinsicHash: ByteArray,
    val slot: Long,
    @SerialName("epoch_mark")
    val epochMark: EpochMark?,
    @SerialName("tickets_mark")
    val ticketsMark: List<TicketMark>?,
    @SerialName("offenders_mark")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val offendersMark: List<ByteArray>,
    @SerialName("author_index")
    val authorIndex: Long,
    @SerialName("entropy_source")
    @Serializable(with = ByteArrayHexSerializer::class)
    val entropySource: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    val seal: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val parentBytes = parent
        val parentStateRootBytes = parentStateRoot
        val extrinsicHashBytes = extrinsicHash
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        val epochMarkBytes =
            if (epochMark != null) byteArrayOf(1) + epochMark.encode() else byteArrayOf(0)
        val ticketsMarkBytes =
            if (ticketsMark != null) byteArrayOf(1) + encodeList(ticketsMark, false) else byteArrayOf(0)
        val offendersMarkBytes =
            offendersMark.fold(encodeFixedWidthInteger(offendersMark.size, 1, false)) { acc, offender ->
                acc + offender
            }
        val authorIndexBytes = encodeFixedWidthInteger(authorIndex, 2, false)
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

