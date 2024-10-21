package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Verdict(
    @Serializable(with = ByteArrayHexSerializer::class)
    val target: ByteArray,
    val age: Int,
    val votes: List<Vote>
) : Encodable {
    override fun encode(): ByteArray {
        val targetBytes = target
        val ageBytes = age.toLEBytes()
        val votesBytes = encodeList(votes)
        return targetBytes + ageBytes + votesBytes
    }
}
