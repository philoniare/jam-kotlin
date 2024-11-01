package io.forge.jam.safrole

import io.forge.jam.core.serializers.ByteArrayListHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Psi(
    @SerialName("psi_g")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiG: MutableList<ByteArray>,

    @SerialName("psi_b")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiB: MutableList<ByteArray>,

    @SerialName("psi_w")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiW: MutableList<ByteArray>,

    @SerialName("psi_o")
    @Serializable(with = ByteArrayListHexSerializer::class)
    val psiO: MutableList<ByteArray>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Psi) return false

        if (psiG.size != other.psiG.size || !psiG.indices.all { psiG[it].contentEquals(other.psiG[it]) }) return false
        if (psiB.size != other.psiB.size || !psiB.indices.all { psiB[it].contentEquals(other.psiB[it]) }) return false
        if (psiW.size != other.psiW.size || !psiW.indices.all { psiW[it].contentEquals(other.psiW[it]) }) return false
        if (psiO.size != other.psiO.size || !psiO.indices.all { psiO[it].contentEquals(other.psiO[it]) }) return false

        return true
    }

    override fun hashCode(): Int {
        var result = psiG.fold(1) { acc, byteArray -> 31 * acc + byteArray.contentHashCode() }
        result = 31 * result + psiB.fold(1) { acc, byteArray -> 31 * acc + byteArray.contentHashCode() }
        result = 31 * result + psiW.fold(1) { acc, byteArray -> 31 * acc + byteArray.contentHashCode() }
        result = 31 * result + psiO.fold(1) { acc, byteArray -> 31 * acc + byteArray.contentHashCode() }
        return result
    }
}
