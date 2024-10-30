package io.forge.jam.safrole

import io.forge.jam.core.serializers.ByteArrayHexSerializer
import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import io.forge.jam.core.toHex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalBeta(
    @SerialName("header_hash")
    @Serializable(with = ByteArrayHexSerializer::class)
    val headerHash: ByteArray,

    val mmr: HistoricalMmr,

    @SerialName("state_root")
    @Serializable(with = ByteArrayHexSerializer::class)
    val stateRoot: ByteArray,

    @SerialName("reported")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val reported: List<ByteArray>
) {
    override fun toString(): String {
        val reportedHex = reported.joinToString(", ") { it.toHex() }
        return "\nHistoricalBeta(headerHash=${headerHash.toHex()}, mmr=$mmr, stateRoot=${stateRoot.toHex()}, reported=[$reportedHex])"
    }
}
