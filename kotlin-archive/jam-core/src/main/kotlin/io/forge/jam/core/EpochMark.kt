package io.forge.jam.core

import io.forge.jam.core.serializers.JamByteArrayHexSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpochMark(
    @Serializable(with = JamByteArrayHexSerializer::class)
    val entropy: JamByteArray,
    @Serializable(with = JamByteArrayHexSerializer::class)
    @SerialName("tickets_entropy")
    val ticketsEntropy: JamByteArray,
    val validators: List<EpochValidatorKey>
) : Encodable {
    companion object {
        fun size(validatorCount: Int): Int = 32 + 32 + validatorCount * EpochValidatorKey.SIZE

        fun fromBytes(data: ByteArray, offset: Int = 0, validatorCount: Int): Pair<EpochMark, Int> {
            val entropy = JamByteArray(data.copyOfRange(offset, offset + 32))
            val ticketsEntropy = JamByteArray(data.copyOfRange(offset + 32, offset + 64))
            val validators = decodeFixedList(data, offset + 64, validatorCount, EpochValidatorKey.SIZE) { d, o ->
                EpochValidatorKey.fromBytes(d, o)
            }
            val totalSize = 64 + validatorCount * EpochValidatorKey.SIZE
            return Pair(EpochMark(entropy, ticketsEntropy, validators), totalSize)
        }
    }
    override fun toString(): String {
        return "EpochMark(" +
            "entropy=${entropy.toHex()}, " +
            "ticketsEntropy=${ticketsEntropy.toHex()}, " +
            "validators=$validators" +
            ")"
    }

    override fun encode(): ByteArray {
        val validatorsBytes =
            validators.fold(byteArrayOf()) { acc, validator ->
                acc + validator.encode()
            }
        return entropy.bytes + ticketsEntropy.bytes + validatorsBytes
    }
}
