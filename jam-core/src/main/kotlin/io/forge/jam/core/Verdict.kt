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
        val ageBytes = encodeCompactInteger(age)
        val votesBytes = encodeList(votes)
        return targetBytes + ageBytes + votesBytes
    }
}
