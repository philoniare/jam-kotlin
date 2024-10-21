package io.forge.jam.core

data class EpochMark(
    val entropy: ByteArray,
    val validators: List<ByteArray>
) {
    fun encode(): ByteArray {
        val entropyBytes = entropy
        val validatorsCountBytes = validators.size.toLEBytes(4)
        val validatorsBytes = validators.fold(byteArrayOf()) { acc, validator ->
            acc + validator
        }
        return entropyBytes + validatorsCountBytes + validatorsBytes
    }
}
