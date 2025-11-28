package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeFixedWidthInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceStatisticsEntry(
    val id: Long,
    val record: ServiceActivityRecord
) : Encodable {
    override fun encode(): ByteArray {
        return encodeFixedWidthInteger(id, 4, false) + record.encode()
    }
}

@Serializable
data class ServiceActivityRecord(
    @SerialName("provided_count")
    val providedCount: Int = 0,
    @SerialName("provided_size")
    val providedSize: Long = 0,
    @SerialName("refinement_count")
    val refinementCount: Long = 0,
    @SerialName("refinement_gas_used")
    val refinementGasUsed: Long = 0,
    val imports: Long = 0,
    @SerialName("extrinsic_count")
    val extrinsicCount: Long = 0,
    @SerialName("extrinsic_size")
    val extrinsicSize: Long = 0,
    val exports: Long = 0,
    @SerialName("accumulate_count")
    val accumulateCount: Long = 0,
    @SerialName("accumulate_gas_used")
    val accumulateGasUsed: Long = 0,
    @SerialName("on_transfers_count")
    val onTransfersCount: Long = 0,
    @SerialName("on_transfers_gas_used")
    val onTransfersGasUsed: Long = 0
) : Encodable {
    override fun encode(): ByteArray {
        return encodeFixedWidthInteger(providedCount.toLong(), 2, false) +
            encodeFixedWidthInteger(providedSize, 4, false) +
            encodeFixedWidthInteger(refinementCount, 4, false) +
            encodeFixedWidthInteger(refinementGasUsed, 8, false) +
            encodeFixedWidthInteger(imports, 4, false) +
            encodeFixedWidthInteger(extrinsicCount, 4, false) +
            encodeFixedWidthInteger(extrinsicSize, 4, false) +
            encodeFixedWidthInteger(exports, 4, false) +
            encodeFixedWidthInteger(accumulateCount, 4, false) +
            encodeFixedWidthInteger(accumulateGasUsed, 8, false) +
            encodeFixedWidthInteger(onTransfersCount, 4, false) +
            encodeFixedWidthInteger(onTransfersGasUsed, 8, false)
    }
}
