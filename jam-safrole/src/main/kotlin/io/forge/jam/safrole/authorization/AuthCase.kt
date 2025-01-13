package io.forge.jam.safrole.authorization

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthCase(
    val input: AuthInput,
    @SerialName("pre_state")
    val preState: AuthState,
    val output: AuthOutput? = null,
    @SerialName("post_state")
    val postState: AuthState
) : Encodable {
    override fun encode(): ByteArray {
        val inputBytes = input.encode()
        val preStateBytes = preState.encode()
        val outputBytes = byteArrayOf(0)
        val postStateBytes = postState.encode()
        return inputBytes + preStateBytes + outputBytes + postStateBytes
    }
}


