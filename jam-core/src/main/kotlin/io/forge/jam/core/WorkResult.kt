package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefineLoad(
    @SerialName("gas_used")
    val gasUsed: Long,
    val imports: Long,
    @SerialName("extrinsic_count")
    val extrinsicCount: Long,
    @SerialName("extrinsic_size")
    val extrinsicSize: Long,
    val exports: Long
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<RefineLoad, Int> {
            var currentOffset = offset
            val (gasUsed, gasUsedBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += gasUsedBytes
            val (imports, importsBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += importsBytes
            val (extrinsicCount, extrinsicCountBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += extrinsicCountBytes
            val (extrinsicSize, extrinsicSizeBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += extrinsicSizeBytes
            val (exports, exportsBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += exportsBytes
            return Pair(RefineLoad(gasUsed, imports, extrinsicCount, extrinsicSize, exports), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        // All fields use compact integer encoding
        return encodeCompactInteger(gasUsed) +
            encodeCompactInteger(imports) +
            encodeCompactInteger(extrinsicCount) +
            encodeCompactInteger(extrinsicSize) +
            encodeCompactInteger(exports)
    }
}

@Serializable
data class WorkResult(
    @SerialName("service_id")
    val serviceId: Long,
    @SerialName("code_hash")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val codeHash: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("payload_hash")
    val payloadHash: JamByteArray,
    @SerialName("accumulate_gas")
    val accumulateGas: Long,
    val result: ExecutionResult,
    @SerialName("refine_load")
    val refineLoad: RefineLoad
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<WorkResult, Int> {
            var currentOffset = offset
            val serviceId = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4
            val codeHash = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val payloadHash = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32
            val accumulateGas = decodeFixedWidthInteger(data, currentOffset, 8, false)
            currentOffset += 8
            val (result, resultBytes) = ExecutionResult.fromBytes(data, currentOffset)
            currentOffset += resultBytes
            val (refineLoad, refineLoadBytes) = RefineLoad.fromBytes(data, currentOffset)
            currentOffset += refineLoadBytes
            return Pair(WorkResult(serviceId, codeHash, payloadHash, accumulateGas, result, refineLoad), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val serviceBytes = encodeFixedWidthInteger(serviceId, 4, false)
        val gasBytes = encodeFixedWidthInteger(accumulateGas, 8, false)
        return serviceBytes + codeHash.bytes + payloadHash.bytes + gasBytes + result.encode() + refineLoad.encode()
    }
}

