package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class GuaranteeExtrinsic(
    val report: WorkReport,
    val slot: Long,
    val signatures: List<GuaranteeSignature>
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<GuaranteeExtrinsic, Int> {
            var currentOffset = offset

            // report - variable size
            val (report, reportBytes) = WorkReport.fromBytes(data, currentOffset)
            currentOffset += reportBytes

            // slot - 4 bytes
            val slot = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // signatures - variable length list of fixed-size items
            val (signaturesLength, signaturesLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += signaturesLengthBytes
            val signatures = decodeFixedList(data, currentOffset, signaturesLength.toInt(), GuaranteeSignature.SIZE) { d, o ->
                GuaranteeSignature.fromBytes(d, o)
            }
            currentOffset += signaturesLength.toInt() * GuaranteeSignature.SIZE

            return Pair(GuaranteeExtrinsic(report, slot, signatures), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val reportBytes = report.encode()
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        // signatures is SEQUENCE OF ValidatorSignature - variable size, compact integer length
        val signaturesLengthBytes = encodeCompactInteger(signatures.size.toLong())
        val signaturesBytes = encodeList(signatures, includeLength = false)
        return reportBytes + slotBytes + signaturesLengthBytes + signaturesBytes
    }
}
