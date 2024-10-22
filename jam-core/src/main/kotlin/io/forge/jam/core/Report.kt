package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val packageSpec: PackageSpec,
    val context: Context,
    val coreIndex: Int,
    val authorizerHash: ByteArray,
    val authOutput: ByteArray,
    val results: List<WorkResult>
) : Encodable {
    override fun encode(): ByteArray {
        val packageSpecBytes = packageSpec.encode()
        val contextBytes = context.encode()
        val coreIndexBytes = coreIndex.toLEBytes()
        val authorizerHashBytes = authorizerHash
        val authOutputLengthBytes = authOutput.size.toLEBytes()
        val authOutputBytes = authOutput
        val resultsBytes = encodeList(results)
        return packageSpecBytes + contextBytes + coreIndexBytes + authorizerHashBytes + authOutputLengthBytes + authOutputBytes + resultsBytes
    }
}
