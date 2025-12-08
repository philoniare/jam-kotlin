package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.WorkReport
import io.forge.jam.core.decodeCompactInteger
import io.forge.jam.core.decodeFixedWidthInteger
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.encodeList
import kotlinx.serialization.Serializable

@Serializable
data class AccumulationInput(val slot: Long, val reports: List<WorkReport>) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<AccumulationInput, Int> {
            var currentOffset = offset

            // slot - 4 bytes
            val slot = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // reports - compact length + variable-size items
            val (reportsLength, reportsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += reportsLengthBytes
            val reports = mutableListOf<WorkReport>()
            for (i in 0 until reportsLength.toInt()) {
                val (report, reportBytes) = WorkReport.fromBytes(data, currentOffset)
                reports.add(report)
                currentOffset += reportBytes
            }

            return Pair(AccumulationInput(slot, reports), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        val reportBytes = encodeList(reports)
        return slotBytes + reportBytes
    }
}
