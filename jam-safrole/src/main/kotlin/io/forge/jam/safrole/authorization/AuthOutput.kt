package io.forge.jam.safrole.authorization

import io.forge.jam.core.Encodable
import kotlinx.serialization.Serializable

@Serializable
data class AuthOutput(val id: Long) : Encodable {
    override fun encode(): ByteArray {
        return byteArrayOf(0)
    }
}
