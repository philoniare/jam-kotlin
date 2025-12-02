package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkItem(
    val service: Long,
    @SerialName("code_hash")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val codeHash: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val payload: JamByteArray,
    @SerialName("refine_gas_limit")
    val refineGasLimit: Long,
    @SerialName("accumulate_gas_limit")
    val accumulateGasLimit: Long,
    @SerialName("import_segments")
    val importSegments: List<WorkItemImportSegment>,
    val extrinsic: List<WorkItemExtrinsic>,
    @SerialName("export_count")
    val exportCount: Long
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<WorkItem, Int> {
            var currentOffset = offset

            // service - 4 bytes
            val service = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // codeHash - 32 bytes
            val codeHash = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32

            // refineGasLimit - 8 bytes
            val refineGasLimit = decodeFixedWidthInteger(data, currentOffset, 8, false)
            currentOffset += 8

            // accumulateGasLimit - 8 bytes
            val accumulateGasLimit = decodeFixedWidthInteger(data, currentOffset, 8, false)
            currentOffset += 8

            // exportCount - 2 bytes
            val exportCount = decodeFixedWidthInteger(data, currentOffset, 2, false)
            currentOffset += 2

            // payload - compact length prefix + bytes
            val (payloadLength, payloadLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += payloadLengthBytes
            val payload = JamByteArray(data.copyOfRange(currentOffset, currentOffset + payloadLength.toInt()))
            currentOffset += payloadLength.toInt()

            // importSegments - compact length prefix + fixed-size items
            val (importSegmentsLength, importSegmentsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += importSegmentsLengthBytes
            val importSegments = decodeFixedList(data, currentOffset, importSegmentsLength.toInt(), WorkItemImportSegment.SIZE) { d, o ->
                WorkItemImportSegment.fromBytes(d, o)
            }
            currentOffset += importSegmentsLength.toInt() * WorkItemImportSegment.SIZE

            // extrinsic - compact length prefix + fixed-size items
            val (extrinsicLength, extrinsicLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += extrinsicLengthBytes
            val extrinsic = decodeFixedList(data, currentOffset, extrinsicLength.toInt(), WorkItemExtrinsic.SIZE) { d, o ->
                WorkItemExtrinsic.fromBytes(d, o)
            }
            currentOffset += extrinsicLength.toInt() * WorkItemExtrinsic.SIZE

            return Pair(
                WorkItem(service, codeHash, payload, refineGasLimit, accumulateGasLimit, importSegments, extrinsic, exportCount),
                currentOffset - offset
            )
        }
    }

    override fun encode(): ByteArray {
        val serviceBytes = encodeFixedWidthInteger(service, 4, false)
        val refineGasLimitBytes = encodeFixedWidthInteger(refineGasLimit, 8, false)
        val accumulateGasLimitBytes = encodeFixedWidthInteger(accumulateGasLimit, 8, false)
        val exportCountBytes = encodeFixedWidthInteger(exportCount, 2, false)
        val payloadLengthBytes = encodeCompactInteger(payload.size.toLong())
        val importSegmentsBytes = encodeList(importSegments)
        val extrinsicBytes = encodeList(extrinsic)
        return serviceBytes + codeHash.bytes + refineGasLimitBytes + accumulateGasLimitBytes + exportCountBytes + payloadLengthBytes + payload.bytes + importSegmentsBytes + extrinsicBytes
    }
}
