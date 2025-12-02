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

            // Decode culprits - fixed-size items
            val (culpritsLength, culpritsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += culpritsLengthBytes
            val culprits = decodeFixedList(data, currentOffset, culpritsLength.toInt(), Culprit.SIZE) { d, o ->
                Culprit.fromBytes(d, o)
            }
            currentOffset += culpritsLength.toInt() * Culprit.SIZE

            // Decode faults - fixed-size items
            val (faultsLength, faultsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += faultsLengthBytes
            val faults = decodeFixedList(data, currentOffset, faultsLength.toInt(), Fault.SIZE) { d, o ->
                Fault.fromBytes(d, o)
            }
            currentOffset += faultsLength.toInt() * Fault.SIZE

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
