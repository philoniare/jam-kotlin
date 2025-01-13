package io.forge.jam.safrole.authorization

import io.forge.jam.core.Encodable
import io.forge.jam.core.JamByteArray
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Auth(
    val core: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("auth_hash")
    val authHash: JamByteArray
) : Encodable {
    override fun encode(): ByteArray {
        val coreBytes = encodeFixedWidthInteger(core, 2, false)
        return coreBytes + authHash.bytes
    }
}
