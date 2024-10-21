package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class GuaranteeSignature(
    val validatorIndex: Int,
    val signature: ByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val validatorIndexBytes = validatorIndex.toLEBytes()
        val signatureBytes = signature
        return validatorIndexBytes + signatureBytes
    }
}
