package io.forge.jam.safrole.preimage

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreimageCase(
    val input: PreimageInput,
    @SerialName("pre_state")
    val preState: PreimageState,
    val output: PreimageOutput,
    @SerialName("post_state")
    val postState: PreimageState
) : Encodable {
    override fun encode(): ByteArray {
        val inputBytes = input.encode()
        val preStateBytes = preState.encode()
        val outputBytes = output.encode()
        val postStateBytes = postState.encode()
        return inputBytes + preStateBytes + outputBytes + postStateBytes
    }
}


