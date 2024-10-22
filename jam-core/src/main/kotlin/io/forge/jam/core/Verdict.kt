package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Verdict(
    @Serializable(with = ByteArrayHexSerializer::class)
    val target: ByteArray,
    val age: Long,
    val votes: List<Vote>
) : Encodable {
    override fun encode(): ByteArray {
        val targetBytes = target
        val ageBytes = encodeFixedWidthInteger(age, 4, false)
        val votesBytes = encodeList(votes, false)
        return targetBytes + ageBytes + votesBytes
    }
}
