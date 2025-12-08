package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class Extrinsic(
    val tickets: List<TicketEnvelope>,
    val preimages: List<Preimage>,
    val guarantees: List<GuaranteeExtrinsic>,
    val assurances: List<AssuranceExtrinsic>,
    val disputes: Dispute,
) : Encodable {
    companion object {
        fun fromBytes(data: ByteArray, offset: Int = 0, coresCount: Int, votesPerVerdict: Int): Pair<Extrinsic, Int> {
            var currentOffset = offset

            // tickets - variable length list of fixed-size items
            val (ticketsLength, ticketsLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += ticketsLengthBytes
            val tickets = decodeFixedList(data, currentOffset, ticketsLength.toInt(), TicketEnvelope.SIZE) { d, o ->
                TicketEnvelope.fromBytes(d, o)
            }
            currentOffset += ticketsLength.toInt() * TicketEnvelope.SIZE

            // preimages - variable length list of variable-size items
            val (preimages, preimagesBytesConsumed) = decodeList(data, currentOffset) { d, o ->
                Preimage.fromBytes(d, o)
            }
            currentOffset += preimagesBytesConsumed

            // guarantees - variable length list of variable-size items
            val (guarantees, guaranteesBytesConsumed) = decodeList(data, currentOffset) { d, o ->
                GuaranteeExtrinsic.fromBytes(d, o)
            }
            currentOffset += guaranteesBytesConsumed

            // assurances - variable length list of fixed-size items (based on coresCount)
            val (assurancesLength, assurancesLengthBytes) = decodeCompactInteger(data, currentOffset)
            currentOffset += assurancesLengthBytes
            val assuranceSize = AssuranceExtrinsic.size(coresCount)
            val assurances = mutableListOf<AssuranceExtrinsic>()
            for (i in 0 until assurancesLength.toInt()) {
                assurances.add(AssuranceExtrinsic.fromBytes(data, currentOffset, coresCount))
                currentOffset += assuranceSize
            }

            // disputes - variable size
            val (disputes, disputesBytesConsumed) = Dispute.fromBytes(data, currentOffset, votesPerVerdict)
            currentOffset += disputesBytesConsumed

            return Pair(Extrinsic(tickets, preimages, guarantees, assurances, disputes), currentOffset - offset)
        }
    }

    override fun encode(): ByteArray {
        val ticketsBytes = encodeList(tickets)
        val disputesBytes = disputes.encode()
        val preimagesBytes = encodeList(preimages)
        val assurancesBytes = encodeList(assurances)
        val guaranteesBytes = encodeList(guarantees)
        return ticketsBytes + preimagesBytes + guaranteesBytes + assurancesBytes + disputesBytes
    }
}

