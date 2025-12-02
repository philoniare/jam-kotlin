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
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, coresCount: Int, epochLength: Int): Pair<AccumulationCase, Int> {
            var currentOffset = offset

            val (input, inputBytes) = AccumulationInput.fromBytes(data, currentOffset)
            currentOffset += inputBytes

            val (preState, preStateBytes) = AccumulationState.fromBytes(data, currentOffset, coresCount, epochLength)
            currentOffset += preStateBytes

            val (output, outputBytes) = AccumulationOutput.fromBytes(data, currentOffset)
            currentOffset += outputBytes

            val (postState, postStateBytes) = AccumulationState.fromBytes(data, currentOffset, coresCount, epochLength)
            currentOffset += postStateBytes

            return Pair(AccumulationCase(input, preState, output, postState), currentOffset - offset)
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
