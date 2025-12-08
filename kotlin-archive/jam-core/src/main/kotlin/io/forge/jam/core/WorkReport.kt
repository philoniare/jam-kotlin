package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkReport(
    @SerialName("package_spec")
    val packageSpec: PackageSpec,
    val context: Context,
    @SerialName("core_index")
    val coreIndex: Long,
    @SerialName("authorizer_hash")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val authorizerHash: JamByteArray,
    @SerialName("auth_gas_used")
    val authGasUsed: Long,
    @SerialName("auth_output")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val authOutput: JamByteArray,
    @SerialName("segment_root_lookup")
    val segmentRootLookup: List<SegmentRootLookup>,
    val results: List<WorkResult>
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<WorkReport, Int> {
            var currentOffset = offset

            // packageSpec - fixed size
            val packageSpec = PackageSpec.fromBytes(data, currentOffset)
            currentOffset += PackageSpec.SIZE

            // context - variable size
            val (context, contextBytes) = Context.fromBytes(data, currentOffset)
            currentOffset += contextBytes

            // coreIndex - compact integer
            val (coreIndex, coreIndexBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += coreIndexBytes

            // authorizerHash - 32 bytes
            val authorizerHash = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32

            // authGasUsed - compact integer
            val (authGasUsed, authGasUsedBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += authGasUsedBytes

            // authOutput - variable length byte sequence
            val (authOutputLength, authOutputLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += authOutputLengthBytes
            val authOutput = JamByteArray(data.copyOfRange(currentOffset, currentOffset + authOutputLength.toInt()))
            currentOffset += authOutputLength.toInt()

            // segmentRootLookup - variable length list of fixed-size items
            val (segmentRootLookupLength, segmentRootLookupLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += segmentRootLookupLengthBytes
            val segmentRootLookup = mutableListOf<SegmentRootLookup>()
            for (i in 0 until segmentRootLookupLength.toInt()) {
                segmentRootLookup.add(SegmentRootLookup.fromBytes(data, currentOffset))
                currentOffset += SegmentRootLookup.SIZE
            }

            // results - variable length list of variable-size items
            val (resultsLength, resultsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += resultsLengthBytes
            val results = mutableListOf<WorkResult>()
            for (i in 0 until resultsLength.toInt()) {
                val (result, resultBytes) = WorkResult.fromBytes(data, currentOffset)
                results.add(result)
                currentOffset += resultBytes
            }

            return Pair(
                WorkReport(packageSpec, context, coreIndex, authorizerHash, authGasUsed, authOutput, segmentRootLookup, results),
                currentOffset - offset
            )
        }
    }
    override fun encode(): ByteArray {
        val packageSpecBytes = packageSpec.encode()
        val contextBytes = context.encode()
        // CoreIndex uses compact integer encoding
        val coreIndexBytes = encodeCompactInteger(coreIndex)
        val authorizerHashBytes = authorizerHash.bytes
        // Auth gas used - compact integer encoding
        val authGasUsedBytes = encodeCompactInteger(authGasUsed)
        // Auth output is ByteSequence - variable length with compact integer length prefix
        val authOutputLengthBytes = encodeCompactInteger(authOutput.size.toLong())
        val authOutputBytes = authOutput.bytes
        // Segment root lookup is SEQUENCE OF - variable length with compact integer length prefix
        val segmentRootLookupBytes = encodeCompactInteger(segmentRootLookup.size.toLong())
        val segmentRootLookupListBytes = encodeList(segmentRootLookup, false)

        // Results is SEQUENCE (SIZE(1..16)) - variable size, needs compact integer length
        val resultsLengthBytes = encodeCompactInteger(results.size.toLong())
        val resultsListBytes = encodeList(results, false)
        return packageSpecBytes +
            contextBytes +
            coreIndexBytes +
            authorizerHashBytes +
            authGasUsedBytes +
            authOutputLengthBytes + authOutputBytes +
            segmentRootLookupBytes + segmentRootLookupListBytes +
            resultsLengthBytes + resultsListBytes
    }
}
