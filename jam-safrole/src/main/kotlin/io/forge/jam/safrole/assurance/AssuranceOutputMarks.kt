package io.forge.jam.safrole.assurance

import io.forge.jam.core.Encodable
import io.forge.jam.core.WorkReport
import io.forge.jam.core.decodeCompactInteger
import io.forge.jam.core.encodeList
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceOutputMarks(
    val reported: List<WorkReport>,
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<AssuranceOutputMarks, Int> {
            var currentOffset = offset

            // reported - compact length prefix + variable-size list
            val (reportedLength, reportedLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += reportedLengthBytes
            val reported = mutableListOf<WorkReport>()
            for (i in 0 until reportedLength.toInt()) {
                val (report, reportBytes) = WorkReport.fromBytes(data, currentOffset)
                reported.add(report)
                currentOffset += reportBytes
            }

            return Pair(AssuranceOutputMarks(reported), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        return encodeList(reported)
    }
}
