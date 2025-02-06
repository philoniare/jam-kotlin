package io.forge.jam.safrole.accumulation

import io.forge.jam.core.Encodable
import io.forge.jam.core.WorkReport
import io.forge.jam.core.encodeFixedWidthInteger
import io.forge.jam.core.encodeList
import kotlinx.serialization.Serializable

@Serializable
data class AccumulationInput(val slot: Long, val reports: List<WorkReport>) : Encodable {
    override fun encode(): ByteArray {
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        val reportBytes = encodeList(reports)
        return slotBytes + reportBytes
    }
}
