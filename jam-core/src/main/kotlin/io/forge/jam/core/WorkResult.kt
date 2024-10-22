package io.forge.jam.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkResult(
    val service: Long,
    @SerialName("code_hash")
    @Serializable(with = ByteArrayHexSerializer::class)
    val codeHash: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    @SerialName("payload_hash")
    val payloadHash: ByteArray,
    @SerialName("gas_ratio")
    val gasRatio: Long,
    val result: ExecutionResult
) : Encodable {
    override fun encode(): ByteArray {
        val serviceBytes = encodeFixedWidthInteger(service, 4, false)
        val gasRatioBytes = encodeFixedWidthInteger(gasRatio, 8, false)
        return serviceBytes + codeHash + payloadHash + gasRatioBytes + result.encode()
    }
}

