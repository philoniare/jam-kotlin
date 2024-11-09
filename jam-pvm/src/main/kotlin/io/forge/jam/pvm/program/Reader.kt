package io.forge.jam.pvm.program

import io.forge.jam.pvm.readVarint

open class Reader<T>(
    val blob: T,
    var position: Int = 0
) : Cloneable where T : Any {

    public override fun clone(): Reader<T> = Reader(blob, position)

    companion object {
        fun <T> from(blob: T): Reader<T> where T : Any = Reader(blob)
    }

    private fun T.asBytes(): ByteArray = when (this) {
        is ByteArray -> this
        is String -> this.encodeToByteArray()
        is ArcBytes -> this.asRef()
        else -> throw IllegalArgumentException("Type must be convertible to ByteArray")
    }

    fun skip(count: Int): Result<Unit> = runCatching {
        readSliceAsRange(count).map { }
    }

    fun readByte(): Result<Byte> = runCatching {
        val slice = readSlice(1).getOrThrow()
        slice[0]
    }

    open fun readSlice(length: Int): Result<ByteArray> = runCatching {
        val blobBytes = blob.asBytes()
        if (position + length > blobBytes.size) {
            throw ProgramParseError.unexpectedEndOfFile(
                position,
                length,
                blobBytes.size - position
            )
        }
        val slice = blobBytes.copyOfRange(position, position + length)
        position += length
        println("slice: ${slice.contentToString()}")
        slice
    }

    fun readVarintInternal(): Result<UInt> = runCatching {
        val firstByte = readByte().getOrThrow()
        val blobBytes = blob.asBytes()
        val (length, value) = readVarint(
            blobBytes.copyOfRange(position, blobBytes.size).toUByteArray(),
            firstByte.toUByte()
        ) ?: throw ProgramParseError.failedToReadVarint(position - 1)
        position += length.toInt()
        value
    }

    fun readBytesWithLength(): Result<ByteArray> = runCatching {
        val length = readVarintInternal().getOrThrow().toInt()
        readSlice(length).getOrThrow()
    }

    fun readStringWithLength(): Result<String> = runCatching {
        val offset = position
        val slice = readBytesWithLength().getOrThrow()
        String(slice).also {
            if (!it.isValidUtf8()) {
                throw ProgramParseError(ProgramParseErrorKind.FailedToReadStringNonUtf(offset))
            }
        }
    }

    fun readSliceAsRange(count: Int): Result<IntRange> = runCatching {
        val blobBytes = blob.asBytes()
        if (position + count > blobBytes.size) {
            throw ProgramParseError.unexpectedEndOfFile(
                position,
                count,
                blobBytes.size - position
            )
        }
        val range = position until (position + count)
        position += count
        range
    }
}

private fun String.isValidUtf8(): Boolean = try {
    this.encodeToByteArray().decodeToString()
    true
} catch (e: Exception) {
    false
}
