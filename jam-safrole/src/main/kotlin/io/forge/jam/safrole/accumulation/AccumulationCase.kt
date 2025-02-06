package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccumulationCase(
    val input: AccumulationInput,
    @SerialName("pre_state")
    val preState: AccumulationState,
    val output: AccumulationOutput,
    @SerialName("post_state")
    val postState: AccumulationState
) : Encodable {
    override fun encode(): ByteArray {
        val inputBytes = input.encode()
        val preStateBytes = preState.encode()
        val outputBytes = output.encode()
        val postStateBytes = postState.encode()
        return inputBytes + preStateBytes + outputBytes + postStateBytes
    }
}
