package io.forge.jam.pvm

fun ByteArray.compareTo(other: ByteArray): Int {
    val minLength = minOf(size, other.size)
    for (i in 0 until minLength) {
        val diff = (this[i].toInt() and 0xFF) - (other[i].toInt() and 0xFF)
        if (diff != 0) return diff
    }
    return size - other.size
}

/**
 * Represents a 32-byte hash value with various utility implementations
 */
data class Hash(val bytes: ByteArray) : Comparable<Hash> {
    init {
        require(bytes.size == 32) { "Hash must be exactly 32 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hash) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun compareTo(other: Hash): Int {
        return bytes.compareTo(other.bytes)
    }

    override fun toString(): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun from(bytes: ByteArray): Hash {
            return Hash(bytes)
        }
    }
}

