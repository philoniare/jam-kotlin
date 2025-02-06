package io.forge.jam.safrole.accumulation

import io.forge.jam.core.*
import io.forge.jam.core.serializers.ByteArrayNestedListSerializer
import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import io.forge.jam.safrole.report.ServiceItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccumulationState(
    val slot: Long,
    @Serializable(with = JamByteArrayHexSerializer::class)
    val entropy: JamByteArray,
    @SerialName("ready_queue")
    val readyQueue: List<List<ReadyRecord>>,
    @SerialName("accumulated")
    @Serializable(with = ByteArrayNestedListSerializer::class)
    val accumulated: List<List<JamByteArray>>,
    val privileges: Privileges,
    val accounts: List<ServiceItem>
) : Encodable {
    override fun encode(): ByteArray {
        val slotBytes = encodeFixedWidthInteger(slot, 4, false)
        return slotBytes + entropy.bytes + encodeNestedList(readyQueue) + encodeNestedList(accumulated) + privileges.encode() + encodeList(
            accounts
        )
    }

    fun deepCopy(): AccumulationState {
        return AccumulationState(
            slot = slot,
            entropy = entropy.copy(),
            readyQueue = readyQueue.map { it.map { it.copy() } },
            accumulated = accumulated.map { it.map { it.copy() } },
            privileges = privileges.copy(),
            accounts = accounts.map { it.copy() }
        )
    }
}
