package io.forge.jam.safrole.assurance

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssuranceCase(
    val input: AssuranceInput,
    @SerialName("pre_state")
    val preState: AssuranceState,
    val output: AssuranceOutput,
    @SerialName("post_state")
    val postState: AssuranceState
) : Encodable {
    override fun encode(): ByteArray {
        val inputBytes = input.encode()
        val preStateBytes = preState.encode()
        val outputBytes = output.encode()
        val postStateBytes = postState.encode()
        return inputBytes + preStateBytes + outputBytes + postStateBytes
    }
}


