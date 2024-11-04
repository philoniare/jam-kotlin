package io.forge.jam.pvm.program

class ArcBytesReader(
    blob: ArcBytes,
    position: Int = 0
) : Reader<ArcBytes>(blob, position) {

    // Override clone to return correct type
    override fun clone(): ArcBytesReader = ArcBytesReader(blob, position)

    companion object {
        fun from(blob: ArcBytes): ArcBytesReader = ArcBytesReader(blob)
    }

    // Optimized readSlice for ArcBytes that uses subslice
    override fun readSlice(length: Int): Result<ByteArray> = runCatching {
        val blobBytes = blob.asRef()
        if (position + length > blobBytes.size) {
            throw ProgramParseError.unexpectedEndOfFile(
                position,
                length,
                blobBytes.size - position
            )
        }
        val slice = blob.subslice(position until (position + length)).asRef()
        position += length
        slice
    }

    // Specialized method that returns ArcBytes instead of ByteArray
    fun readSliceAsBytes(length: Int): Result<ArcBytes> = runCatching {
        if (position + length > blob.asRef().size) {
            throw ProgramParseError.unexpectedEndOfFile(
                position,
                length,
                blob.asRef().size - position
            )
        }
        val result = blob.subslice(position until (position + length))
        position += length
        result
    }

    fun readSectionAsBytes(
        outSection: Byte,
        expectedSection: Byte
    ): Result<Pair<Byte, ArcBytes>> = runCatching {
        if (outSection != expectedSection) {
            return@runCatching Pair(outSection, ArcBytes.empty())
        }

        val sectionLength = readVarintInternal().getOrThrow().toInt()
        val range = position until (position + sectionLength)
        val result = blob.subslice(range)
        position += sectionLength
        val newSection = readByte().getOrThrow()
        Pair(newSection, result)
    }

    fun readBytesWithLengthAsArcBytes(): Result<ArcBytes> = runCatching {
        val length = readVarintInternal().getOrThrow().toInt()
        readSliceAsBytes(length).getOrThrow()
    }
}
