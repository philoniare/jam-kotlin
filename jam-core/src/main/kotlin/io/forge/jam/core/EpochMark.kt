package io.forge.jam.core

import kotlinx.serialization.Serializable

@Serializable
data class EpochMark(
    @Serializable(with = ByteArrayHexSerializer::class)
    val entropy: ByteArray,
    @Serializable(with = ByteArrayListHexSerializer::class)
    val validators: List<ByteArray>
) : Encodable {
    override fun encode(): ByteArray {
        val validatorsBytes =
            validators.fold(byteArrayOf()) { acc, validator ->
                acc + validator
            }
        return entropy + validatorsBytes
    }
}
