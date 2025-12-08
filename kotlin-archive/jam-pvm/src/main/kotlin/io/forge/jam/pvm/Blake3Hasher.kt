package io.forge.jam.pvm

import org.bouncycastle.crypto.digests.Blake3Digest


/**
 * Implementation of Blake3 hasher using Bouncy Castle crypto library
 */
class Blake3Hasher {
    private val digest = Blake3Digest(32) // 32 bytes (256 bits) output size

    /**
     * Updates the hash with new input data
     */
    fun update(bytes: ByteArray) {
        digest.update(bytes, 0, bytes.size)
    }

    /**
     * Finalizes the hash and writes it to the provided output buffer
     */
    fun finalize(output: ByteArray) {
        require(output.size == 32) { "Output buffer must be 32 bytes" }
        digest.doFinal(output, 0)
    }
}
