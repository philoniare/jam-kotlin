package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.decodeCompactInteger
import io.forge.jam.core.decodeFixedWidthInteger
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.encodeList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Privileges(
    val bless: Long,
    val assign: List<Long>,
    val designate: Long,
    val register: Long,
    @SerialName("always_acc")
    val alwaysAcc: List<AlwaysAccItem>
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, coresCount: Int): Pair<Privileges, Int> {
            var currentOffset = offset

            // bless - 4 bytes
            val bless = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // assign - fixed size list (coresCount)
            val assign = mutableListOf<Long>()
            for (i in 0 until coresCount) {
                assign.add(decodeFixedWidthInteger(data, currentOffset, 4, false))
                currentOffset += 4
            }

            // designate - 4 bytes
            val designate = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // register - 4 bytes
            val register = decodeFixedWidthInteger(data, currentOffset, 4, false)
            currentOffset += 4

            // alwaysAcc - compact length prefix + fixed-size items
            val (alwaysAccLength, alwaysAccLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += alwaysAccLengthBytes
            val alwaysAcc = mutableListOf<AlwaysAccItem>()
            for (i in 0 until alwaysAccLength.toInt()) {
                alwaysAcc.add(AlwaysAccItem.fromBytes(data, currentOffset))
                currentOffset += AlwaysAccItem.SIZE
            }

            return Pair(Privileges(bless, assign, designate, register, alwaysAcc), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val blessBytes = encodeFixedWidthInteger(bless, 4, false)
        val assignBytes = assign.flatMap { encodeFixedWidthInteger(it, 4, false).toList() }.toByteArray()
        val designateBytes = encodeFixedWidthInteger(designate, 4, false)
        val registerBytes = encodeFixedWidthInteger(register, 4, false)
        val alwaysAccBytes = encodeList(alwaysAcc)
        return blessBytes + assignBytes + designateBytes + registerBytes + alwaysAccBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Privileges) return false

        if (bless != other.bless) return false
        if (assign != other.assign) return false
        if (designate != other.designate) return false
        if (register != other.register) return false
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
        result = 31 * result + register.hashCode()
        result = 31 * result + alwaysAcc.hashCode()
        return result
    }

    override fun toString(): String {
        return "Privileges(bless=$bless, assign=$assign, designate=$designate, register=$register, alwaysAcc=$alwaysAcc)"
    }
}
