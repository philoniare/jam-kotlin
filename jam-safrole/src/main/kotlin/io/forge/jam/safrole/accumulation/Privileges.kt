package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.encodeList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Privileges(
    val bless: Long,
    val assign: List<Long>,
    val designate: Long,
    @SerialName("always_acc")
    val alwaysAcc: List<AlwaysAccItem>

) : Encodable {
    override fun encode(): ByteArray {
        val blessBytes = encodeFixedWidthInteger(bless, 4, false)
        val assignBytes = assign.flatMap { encodeFixedWidthInteger(it, 4, false).toList() }.toByteArray()
        val designateBytes = encodeFixedWidthInteger(designate, 4, false)
        val alwaysAccBytes = encodeList(alwaysAcc)
        return blessBytes + assignBytes + designateBytes + alwaysAccBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Privileges) return false

        if (bless != other.bless) return false
        if (assign != other.assign) return false
        if (designate != other.designate) return false
        if (alwaysAcc.size != other.alwaysAcc.size) return false

        for (i in alwaysAcc.indices) {
            val thisItem = alwaysAcc[i]
            val otherItem = other.alwaysAcc[i]
            if (thisItem.id != otherItem.id || thisItem.gas != otherItem.gas) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = bless.hashCode()
        result = 31 * result + assign.hashCode()
        result = 31 * result + designate.hashCode()
        result = 31 * result + alwaysAcc.hashCode()
        return result
    }

    override fun toString(): String {
        return "Privileges(bless=$bless, assign=$assign, designate=$designate, alwaysAcc=$alwaysAcc)"
    }
}
