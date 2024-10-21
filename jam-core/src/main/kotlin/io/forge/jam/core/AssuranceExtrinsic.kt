package io.forge.jam.core

data class AssuranceExtrinsic(
    val anchor: ByteArray,        // 32-byte anchor (hash)
    val bitfield: ByteArray,      // Bitfield indicating core assurance
    val validatorIndex: Int,      // Validator index (encoded as two bytes)
    val signature: ByteArray      // Ed25519 signature of the assurance
) {
    // Method to encode the extrinsic into a binary format
    fun encode(): ByteArray {
        // Encode validatorIndex as 2 bytes (little-endian)
        val validatorIndexBytes = ByteArray(2)
        validatorIndexBytes[0] = (validatorIndex and 0xFF).toByte()
        validatorIndexBytes[1] = ((validatorIndex shr 8) and 0xFF).toByte()

        // Concatenate anchor, bitfield, validatorIndexBytes, signature
        return anchor + bitfield + validatorIndexBytes + signature
    }
}
