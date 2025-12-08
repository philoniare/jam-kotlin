package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import io.forge.jam.core.serializers.JamByteArrayListHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Header(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val parent: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("parent_state_root")
    val parentStateRoot: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("extrinsic_hash")
    val extrinsicHash: JamByteArray,
    val slot: Long,
    @SerialName("epoch_mark")
    val epochMark: EpochMark?,
    @SerialName("tickets_mark")
    val ticketsMark: List<TicketMark>?,
    @SerialName("offenders_mark")
    @Serializable(with = JamByteArrayListHexSerializer::class)
    val offendersMark: List<JamByteArray>,
    @SerialName("author_index")
    val authorIndex: Long,
    @SerialName("entropy_source")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val entropySource: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val seal: JamByteArray
) : Encodable {
    companion object {
        const val ENTROPY_SOURCE_SIZE = 96 // IETF VRF signature
        const val SEAL_SIZE = 96 // Ed25519 signature (64 bytes) + VRF output (32 bytes)

        fun fromBytes(data: ByteArray, offset: Int = 0, validatorCount: Int, epochLength: Int): Pair<Header, Int> {
            var currentOffset = offset

            // Fixed-size fields at the beginning
            val parent = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val parentStateRoot = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val extrinsicHash = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val slot = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // epochMark - optional
            val epochMarkFlag = data[currentOffset].toInt() and 0xFF
            currentOffset += 1
            val epochMark = if (epochMarkFlag == 1) {
                val (mark, markSize) = EpochMark.fromBytes(data, currentOffset, validatorCount)
                currentOffset += markSize
                mark
            } else null

            // ticketsMark - optional list of EPOCH_LENGTH items
            val ticketsMarkFlag = data[currentOffset].toInt() and 0xFF
            currentOffset += 1
            val ticketsMark = if (ticketsMarkFlag == 1) {
                val marks = decodeFixedList(data, currentOffset, epochLength, TicketMark.SIZE) { d, o ->
                    TicketMark.fromBytes(d, o)
                }
                currentOffset += epochLength * TicketMark.SIZE
                marks
            } else null

            // authorIndex - 2 bytes
            val authorIndex = decodeFixedWidthInteger(data, currentOffset, 2, false)
            currentOffset += 2

            // entropySource - 96 bytes
            val entropySource = JamByteArray(data.copyOfRange(currentOffset, currentOffset + ENTROPY_SOURCE_SIZE))
            currentOffset += ENTROPY_SOURCE_SIZE

            // offendersMark - variable length list of 32-byte hashes
            val (offendersLength, offendersLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += offendersLengthBytes
            val offendersMark = mutableListOf<JamByteArray>()
            for (i in 0 until offendersLength.toInt()) {
                offendersMark.add(JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32)))
                currentOffset += 32
            }

            // seal - 96 bytes
            val seal = JamByteArray(data.copyOfRange(currentOffset, currentOffset + SEAL_SIZE))
            currentOffset += SEAL_SIZE

            return Pair(
                Header(parent, parentStateRoot, extrinsicHash, slot, epochMark, ticketsMark, offendersMark, authorIndex, entropySource, seal),
                currentOffset - offset
            )
        }
    }
    override fun encode(): ByteArray {
        val parentBytes = parent.bytes
        val parentStateRootBytes = parentStateRoot.bytes
        val extrinsicHashBytes = extrinsicHash.bytes
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        val epochMarkBytes =
            if (epochMark != null) byteArrayOf(1) + epochMark.encode() else byteArrayOf(0)
        val ticketsMarkBytes =
            if (ticketsMark != null) byteArrayOf(1) + encodeList(ticketsMark, false) else byteArrayOf(0)
        val authorIndexBytes = encodeFixedWidthInteger(authorIndex, 2, false)
        val entropySourceBytes = entropySource.bytes
        val offendersMarkLengthBytes = encodeCompactInteger(offendersMark.size.toLong())
        val offendersMarkBytes =
            offendersMark.fold(offendersMarkLengthBytes) { acc, offender ->
                acc + offender.bytes
            }
        val sealBytes = seal.bytes
        return parentBytes +
            parentStateRootBytes +
            extrinsicHashBytes +
            slotBytes +
            epochMarkBytes +
            ticketsMarkBytes +
            authorIndexBytes +
            entropySourceBytes +
            offendersMarkBytes +
            sealBytes
    }
}

