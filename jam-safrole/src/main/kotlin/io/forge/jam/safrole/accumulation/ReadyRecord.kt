package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.WorkReport
import io.forge.jam.core.decodeCompactInteger
import io.forge.jam.core.encodeList
import io.forge.jam.core.serializers.JamByteArrayListHexSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ReadyRecord(
    val report: WorkReport,
    @Serializable(with = JamByteArrayListHexSerializer::class)
    val dependencies: List<JamByteArray>,
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<ReadyRecord, Int> {
            var currentOffset = offset

            // report - variable size
            val (report, reportBytes) = WorkReport.fromBytes(data, currentOffset)
            currentOffset += reportBytes

            // dependencies - compact length + 32-byte hashes
            val (depsLength, depsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += depsLengthBytes
            val dependencies = mutableListOf<JamByteArray>()
            for (i in 0 until depsLength.toInt()) {
                dependencies.add(JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32)))
                currentOffset += 32
            }

            return Pair(ReadyRecord(report, dependencies), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        return report.encode() + encodeList(dependencies)
    }
}
