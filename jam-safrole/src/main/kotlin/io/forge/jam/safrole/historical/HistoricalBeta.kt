package io.forge.jam.safrole.historical

import io.forge.jam.core.ReportedWorkPackage
import io.forge.jam.core.serializers.ByteArrayHexSerializer
import io.forge.jam.core.toHex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalBeta(
    @SerialName("header_hash")
    @Serializable(with = ByteArrayHexSerializer::class)
    val hash: ByteArray,

    val mmr: HistoricalMmr,

    @SerialName("state_root")
    @Serializable(with = ByteArrayHexSerializer::class)
    val stateRoot: ByteArray,

    @SerialName("reported")
    val reported: List<ReportedWorkPackage>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HistoricalBeta

        if (!hash.contentEquals(other.hash)) return false
        if (!stateRoot.contentEquals(other.stateRoot)) return false

        if (mmr != other.mmr) return false
        if (reported != other.reported) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.contentHashCode()
        result = 31 * result + mmr.hashCode()
        result = 31 * result + stateRoot.contentHashCode()
        result = 31 * result + reported.hashCode()
        return result
    }

    override fun toString(): String {
        return "\nHistoricalBeta(headerHash=${hash.toHex()}, mmr=$mmr, stateRoot=${stateRoot.toHex()}, reported=[$reported])"
    }
}
