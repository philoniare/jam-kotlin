package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Dispute(
    val verdicts: List<Verdict>,
    val culprits: List<Culprit>,
    val faults: List<Fault>
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, votesPerVerdict: Int = Verdict.VOTES_COUNT): Pair<Dispute, Int> {
            var currentOffset = offset

            // Decode verdicts
            val (verdictsLength, verdictsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += verdictsLengthBytes
            val verdicts = mutableListOf<Verdict>()
            val verdictSize = 36 + votesPerVerdict * Vote.SIZE
            for (i in 0 until verdictsLength.toInt()) {
                val (verdict, _) = Verdict.fromBytes(data, currentOffset, votesPerVerdict)
                verdicts.add(verdict)
                currentOffset += verdictSize
            }

            // Decode culprits
            val (culprits, culpritsBytesConsumed) = decodeList(data, currentOffset) { d, o ->
                Pair(Culprit.fromBytes(d, o), Culprit.SIZE)
            }
            currentOffset += culpritsBytesConsumed

            // Decode faults
            val (faults, faultsBytesConsumed) = decodeList(data, currentOffset) { d, o ->
                Pair(Fault.fromBytes(d, o), Fault.SIZE)
            }
            currentOffset += faultsBytesConsumed

            return Pair(Dispute(verdicts, culprits, faults), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val verdictsBytes = encodeList(verdicts)
        val culpritsBytes = encodeList(culprits)
        val faultsBytes = encodeList(faults)
        return verdictsBytes + culpritsBytes + faultsBytes
    }
}
