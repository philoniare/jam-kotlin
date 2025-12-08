package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.decodeCompactInteger
import io.forge.jam.core.decodeFixedWidthInteger
import io.forge.jam.core.encodeCompactInteger
import io.forge.jam.core.encodeFixedWidthInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceStatisticsEntry(val id: Long, val record: ServiceActivityRecord) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<ServiceStatisticsEntry, Int> {
            var currentOffset = offset

            val id = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            val (record, recordBytes) = ServiceActivityRecord.fromBytes(data, currentOffset)
            currentOffset += recordBytes

            return Pair(ServiceStatisticsEntry(id, record), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        return encodeFixedWidthInteger(id, 4, false) + record.encode()
    }
}

@Serializable
data class ServiceActivityRecord(
        @SerialName("provided_count") val providedCount: Int = 0,
        @SerialName("provided_size") val providedSize: Long = 0,
        @SerialName("refinement_count") val refinementCount: Long = 0,
        @SerialName("refinement_gas_used") val refinementGasUsed: Long = 0,
        val imports: Long = 0,
        @SerialName("extrinsic_count") val extrinsicCount: Long = 0,
        @SerialName("extrinsic_size") val extrinsicSize: Long = 0,
        val exports: Long = 0,
        @SerialName("accumulate_count") val accumulateCount: Long = 0,
        @SerialName("accumulate_gas_used") val accumulateGasUsed: Long = 0
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<ServiceActivityRecord, Int> {
            var currentOffset = offset

            val (providedCount, providedCountBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += providedCountBytes

            val (providedSize, providedSizeBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += providedSizeBytes

            val (refinementCount, refinementCountBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += refinementCountBytes

            val (refinementGasUsed, refinementGasUsedBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += refinementGasUsedBytes

            val (imports, importsBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += importsBytes

            val (extrinsicCount, extrinsicCountBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += extrinsicCountBytes

            val (extrinsicSize, extrinsicSizeBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += extrinsicSizeBytes

            val (exports, exportsBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += exportsBytes

            val (accumulateCount, accumulateCountBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += accumulateCountBytes

            val (accumulateGasUsed, accumulateGasUsedBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += accumulateGasUsedBytes

            return Pair(
                ServiceActivityRecord(
                    providedCount.toInt(),
                    providedSize,
                    refinementCount,
                    refinementGasUsed,
                    imports,
                    extrinsicCount,
                    extrinsicSize,
                    exports,
                    accumulateCount,
                    accumulateGasUsed
                ),
                currentOffset - offset
            )
        }
    }

    override fun encode(): ByteArray {
        return encodeCompactInteger(providedCount.toLong()) +
                encodeCompactInteger(providedSize) +
                encodeCompactInteger(refinementCount) +
                encodeCompactInteger(refinementGasUsed) +
                encodeCompactInteger(imports) +
                encodeCompactInteger(extrinsicCount) +
                encodeCompactInteger(extrinsicSize) +
                encodeCompactInteger(exports) +
                encodeCompactInteger(accumulateCount) +
                encodeCompactInteger(accumulateGasUsed)
    }
}
