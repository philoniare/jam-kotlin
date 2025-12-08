package io.forge.jam.pvm.program

/**
 * Type alias for lookup entry represented as an unsigned 32-bit integer
 */
typealias LookupEntry = UInt

/**
 * Constant representing an empty lookup entry
 */
private const val EMPTY_LOOKUP_ENTRY: LookupEntry = 0u

/**
 * A lookup table for handling immediate value operations.
 * This class provides efficient lookup capabilities for immediate value bit patterns.
 *
 * @property table The underlying array of lookup entries
 */
@JvmInline
value class LookupTable private constructor(
    private val table: Array<LookupEntry>
) {
    companion object {
        /**
         * Packs immediate value parameters into a single lookup entry.
         *
         * @param imm1Bits First immediate value bits
         * @param imm1Skip First immediate value skip amount
         * @param imm2Bits Second immediate value bits
         * @return Packed lookup entry
         */
        private fun pack(imm1Bits: UInt, imm1Skip: UInt, imm2Bits: UInt): LookupEntry {
            require(imm1Bits <= 0b111111u) { "imm1Bits out of range" }
            require(imm2Bits <= 0b111111u) { "imm2Bits out of range" }
            require(imm1Skip <= 0b111111u) { "imm1Skip out of range" }

            return imm1Bits or (imm1Skip shl 6) or (imm2Bits shl 12)
        }

        /**
         * Unpacks a lookup entry into its component values.
         *
         * @param entry The lookup entry to unpack
         * @return Triple of (imm1Bits, imm1Skip, imm2Bits)
         */
        private fun unpack(entry: LookupEntry): Triple<UInt, UInt, UInt> = Triple(
            entry and 0b111111u,
            (entry shr 6) and 0b111111u,
            (entry shr 12) and 0b111111u
        )

        /**
         * Builds a lookup table with the specified offset.
         *
         * @param offset The offset to apply during table construction
         * @return New LookupTable instance
         */
        fun build(offset: Int): LookupTable {
            val output = Array(256) { EMPTY_LOOKUP_ENTRY }

            for (skip in 0..0b11111) {
                for (aux in 0..0b111) {
                    val imm1Length = minOf(4u, aux.toUInt())
                    val imm2Length = (skip.toInt() - imm1Length.toInt() - offset)
                        .coerceIn(0..4)
                        .toUInt()

                    val imm1Bits = signExtendCutoffForLength(imm1Length)
                    val imm2Bits = signExtendCutoffForLength(imm2Length)
                    val imm1Skip = imm1Length * 8u

                    val index = getLookupIndex(skip.toUInt(), aux.toUInt())
                    output[index.toInt()] = pack(imm1Bits, imm1Skip, imm2Bits)
                }
            }

            return LookupTable(output)
        }

        /**
         * Calculates lookup index from skip and aux values.
         *
         * @param skip Skip value (must be <= 0b11111)
         * @param aux Auxiliary value (must be <= 0b111)
         * @return Calculated lookup index
         */
        private fun getLookupIndex(skip: UInt, aux: UInt): UInt {
            require(skip <= 0b11111u) { "Skip value out of range" }
            val index = skip or ((aux and 0b111u) shl 5)
            require(index <= 0xFFu) { "Calculated index out of range" }
            return index
        }

        /**
         * Determines sign extension cutoff based on length.
         *
         * @param length Length value to process
         * @return Sign extension cutoff value
         */
        private fun signExtendCutoffForLength(length: UInt): UInt = when (length) {
            0u -> 32u
            1u -> 24u
            2u -> 16u
            3u -> 8u
            4u -> 0u
            else -> throw IllegalArgumentException("Invalid length: $length")
        }
    }

    /**
     * Retrieves the unpacked values for given skip and aux parameters.
     *
     * @param skip Skip value
     * @param aux Auxiliary value
     * @return Triple of (imm1Bits, imm1Skip, imm2Bits)
     */
    fun get(skip: UInt, aux: UInt): Triple<UInt, UInt, UInt> {
        val index = getLookupIndex(skip, aux)
        return unpack(table[index.toInt()])
    }
}
