package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.encodeList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Privileges(
    val bless: Long,
    val assign: Long,
    val designate: Long,
    @SerialName("always_acc")
    val alwaysAcc: List<AlwaysAccItem>

) : Encodable {
    override fun encode(): ByteArray {
        val blessBytes = encodeFixedWidthInteger(bless, 4, false)
        val assignBytes = encodeFixedWidthInteger(bless, 4, false)
        val designateBytes = encodeFixedWidthInteger(designate, 4, false)
        val alwaysAccBytes = encodeList(alwaysAcc)
        return blessBytes + assignBytes + designateBytes + alwaysAccBytes
    }
}
