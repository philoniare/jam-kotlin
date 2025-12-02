package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkPackage(
    @SerialName("auth_code_host")
    val authCodeHost: Long,
    @SerialName("auth_code_hash")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val authCodeHash: JamByteArray,
    val context: Context,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val authorization: JamByteArray,
    @SerialName("authorizer_config")
    @Serializable(with = JamByteArrayHexSerializer::class)
    val authorizerConfig: JamByteArray,
    val items: List<WorkItem>,
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0): Pair<WorkPackage, Int> {
            var currentOffset = offset

            // authCodeHost - 4 bytes
            val authCodeHost = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // authCodeHash - 32 bytes
            val authCodeHash = JamByteArray(data.copyOfRange(currentOffset, currentOffset + 32))
            currentOffset += 32

            // context - variable size
            val (context, contextBytes) = Context.fromBytes(data, currentOffset)
            currentOffset += contextBytes

            // authorization - compact length prefix + bytes
            val (authorizationLength, authorizationLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += authorizationLengthBytes
            val authorization = JamByteArray(data.copyOfRange(currentOffset, currentOffset + authorizationLength.toInt()))
            currentOffset += authorizationLength.toInt()

            // authorizerConfig - compact length prefix + bytes
            val (authorizerConfigLength, authorizerConfigLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += authorizerConfigLengthBytes
            val authorizerConfig = JamByteArray(data.copyOfRange(currentOffset, currentOffset + authorizerConfigLength.toInt()))
            currentOffset += authorizerConfigLength.toInt()

            // items - compact length prefix + variable-size items
            val (items, itemsBytes) = decodeList(data, currentOffset) { d, o ->
                WorkItem.fromBytes(d, o)
            }
            currentOffset += itemsBytes

            return Pair(
                WorkPackage(authCodeHost, authCodeHash, context, authorization, authorizerConfig, items),
                currentOffset - offset
            )
        }
    }

    override fun encode(): ByteArray {
        val authCodeHostBytes = encodeFixedWidthInteger(authCodeHost, 4, false)
        val authorizationLengthBytes = encodeCompactInteger(authorization.size.toLong())
        val authorizerConfigLengthBytes = encodeCompactInteger(authorizerConfig.size.toLong())
        val workItemsBytes = encodeList(items)
        return authCodeHostBytes + authCodeHash.bytes + context.encode() + authorizationLengthBytes + authorization.bytes + authorizerConfigLengthBytes + authorizerConfig.bytes + workItemsBytes
    }
}

