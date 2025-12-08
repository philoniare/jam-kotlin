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
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, coresCount: Int, validatorsCount: Int): Pair<AssuranceCase, Int> {
            var currentOffset = offset

            val (input, inputBytes) = AssuranceInput.fromBytes(data, currentOffset, coresCount)
            currentOffset += inputBytes

            val (preState, preStateBytes) = AssuranceState.fromBytes(data, currentOffset, coresCount, validatorsCount)
            currentOffset += preStateBytes

            val (output, outputBytes) = AssuranceOutput.fromBytes(data, currentOffset)
            currentOffset += outputBytes

            val (postState, postStateBytes) = AssuranceState.fromBytes(data, currentOffset, coresCount, validatorsCount)
            currentOffset += postStateBytes

            return Pair(AssuranceCase(input, preState, output, postState), currentOffset - offset)
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


