package io.forge.jam.pvm.program

/**
 * A wrapper around ByteArray that provides zero-copy slicing capabilities.
 */
class ArcBytes private constructor(
    private val data: ByteArray,
    private val offset: Int,
    private val length: Int
) : Cloneable {

    companion object {
        fun empty(): ArcBytes = ArcBytes(ByteArray(0), 0, 0)

        fun fromStatic(bytes: ByteArray): ArcBytes = ArcBytes(bytes, 0, bytes.size)
    }

    fun subslice(range: IntRange): ArcBytes {
        if (range.first == range.last) {
            return empty()
        }
        require(range.last >= range.first) { "Invalid range: end must be >= start" }

        val newLength = range.last - range.first
        require(newLength <= length) { "Subslice length exceeds available length" }

        return ArcBytes(data, offset + range.first, newLength)
    }

    // Convert to ByteArray for reading - only copies when absolutely necessary
    fun toByteArray(): ByteArray = when {
        offset == 0 && length == data.size -> data
        else -> data.copyOfRange(offset, offset + length)
    }

    public override fun clone(): ArcBytes = ArcBytes(data, offset, length)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArcBytes) return false

        // Compare actual bytes
        val thisBytes = toByteArray()
        val otherBytes = other.toByteArray()
        return thisBytes.contentEquals(otherBytes)
    }

    override fun hashCode(): Int {
        return toByteArray().contentHashCode()
    }

    override fun toString(): String = "ArcBytes(length=$length)"
}

// Extension function to make ArcBytes work with the Reader implementation
fun ArcBytes.asRef(): ByteArray = this.toByteArray()
