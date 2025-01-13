package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PreimageErrorCode : Encodable {
    @SerialName("preimage_unneeded")
    PREIMAGE_UNNEEDED;

    override fun encode(): ByteArray {
        return byteArrayOf(ordinal.toByte())
    }
}
