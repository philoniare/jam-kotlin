package io.forge.jam.safrole.safrole

import io.forge.jam.core.Encodable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SafroleCase(
    val input: SafroleInput,
    @SerialName("pre_state")
    val preState: SafroleState,
    val output: SafroleOutput,
    @SerialName("post_state")
    val postState: SafroleState
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, validatorCount: Int, epochLength: Int): Pair<SafroleCase, Int> {
            var currentOffset = offset

            val (input, inputBytes) = SafroleInput.fromBytes(data, currentOffset)
            currentOffset += inputBytes

            val (preState, preStateBytes) = SafroleState.fromBytes(data, currentOffset, validatorCount, epochLength)
            currentOffset += preStateBytes

            val (output, outputBytes) = SafroleOutput.fromBytes(data, currentOffset, validatorCount, epochLength)
            currentOffset += outputBytes

            val (postState, postStateBytes) = SafroleState.fromBytes(data, currentOffset, validatorCount, epochLength)
            currentOffset += postStateBytes

            return Pair(SafroleCase(input, preState, output, postState), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val inputBytes = input.encode()
        val preStateBytes = preState.encode()
        val outputBytes = output.encode()
        val postStateBytes = postState.encode()
        return inputBytes + preStateBytes + outputBytes + postStateBytes
    }
}
