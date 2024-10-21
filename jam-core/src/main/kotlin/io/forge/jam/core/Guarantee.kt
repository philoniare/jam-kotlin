package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Guarantee(
    val report: Report,
    val slot: Int,
    val signatures: List<GuaranteeSignature>
) : Encodable {
    override fun encode(): ByteArray {
        val reportBytes = report.encode()
        val slotBytes = slot.toLEBytes()
        val signaturesBytes = encodeList(signatures)
        return reportBytes + slotBytes + signaturesBytes
    }
}
