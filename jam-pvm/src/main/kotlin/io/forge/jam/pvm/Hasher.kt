package io.forge.jam.pvm

/**
 * Main Hasher class using Bouncy Castle's Blake3 implementation
 */
class Hasher {
    private val inner = Blake3Hasher()

    fun update(bytes: ByteArray) {
        inner.update(bytes)
    }

    fun updateU32Array(values: IntArray) {
        // Convert IntArray to ByteArray maintaining little-endian order
        val bytes = ByteArray(values.size * 4)
        values.forEachIndexed { index, value ->
            bytes[index * 4] = value.toByte()
            bytes[index * 4 + 1] = (value shr 8).toByte()
            bytes[index * 4 + 2] = (value shr 16).toByte()
            bytes[index * 4 + 3] = (value shr 24).toByte()
        }
        update(bytes)
    }

    fun finalize(): Hash {
        val hash = ByteArray(32)
        inner.finalize(hash)
        return Hash(hash)
    }

    companion object {
        fun new(): Hasher = Hasher()
    }
}
