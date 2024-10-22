package io.forge.jam.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class WorkAuthorizer(
    @SerialName("code_hash")
    @Serializable(with = ByteArrayHexSerializer::class)
    val codeHash: ByteArray,
    @Serializable(with = ByteArrayHexSerializer::class)
    val params: ByteArray,
) : Encodable {
    override fun encode(): ByteArray {
        val paramsLengthBytes = encodeFixedWidthInteger(params.size, 1, false)
        return codeHash + paramsLengthBytes + params
    }
}

