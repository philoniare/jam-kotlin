package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Result(
    val service: Int,
    val codeHash: ByteArray,
    val payloadHash: ByteArray,
    val gasRatio: Int,
    val result: ExecutionResult
) : Encodable {
    override fun encode(): ByteArray {
        val serviceBytes = service.toLEBytes()
        val codeHashBytes = codeHash
        val payloadHashBytes = payloadHash
        val gasRatioBytes = gasRatio.toLEBytes()
        val resultBytes = result.encode()
        return serviceBytes + codeHashBytes + payloadHashBytes + gasRatioBytes + resultBytes
    }
}
