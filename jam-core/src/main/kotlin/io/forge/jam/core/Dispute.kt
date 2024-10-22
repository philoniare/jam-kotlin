package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Dispute(
    val verdicts: List<Verdict>,
    val culprits: List<Culprit>,
    val faults: List<Fault>
) : Encodable {
    override fun encode(): ByteArray {
        val verdictsBytes = encodeList(verdicts, false)
        val culpritsBytes = encodeList(culprits)
        val faultsBytes = encodeList(faults)
        return verdictsBytes + culpritsBytes + faultsBytes
    }
}
