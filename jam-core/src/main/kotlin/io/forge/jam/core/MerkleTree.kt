import org.bouncycastle.jcajce.provider.digest.Blake2b

// Blake2b-256 hash function
fun blakeHash(data: ByteArray): ByteArray {
    val digest = Blake2b.Blake2b256()
    digest.update(data, 0, data.size)
    return digest.digest()
}

// GP (286): Branch function
fun branch(l: ByteArray, r: ByteArray): ByteArray {
    require(l.size == 32) { "Left child must be 32 bytes" }
    require(r.size == 32) { "Right child must be 32 bytes" }
    val head = (l[0].toInt() and 0x7f).toByte()
    return byteArrayOf(head) + l.sliceArray(1 until l.size) + r
}

// GP (287): Leaf function
fun leaf(k: ByteArray, v: ByteArray): ByteArray {
    return if (v.size <= 32) {
        val head = (0b10000000 or (v.size)).toByte()
        byteArrayOf(head) + k.sliceArray(0 until k.size - 1) + v + ByteArray(32 - v.size)
    } else {
        val head = 0b11000000.toByte()
        byteArrayOf(head) + k.sliceArray(0 until k.size - 1) + blakeHash(v)
    }
}

// Bit extraction function
fun bit(k: ByteArray, i: Int): Boolean {
    return (k[i shr 3].toInt() and (1 shl (7 - (i and 7)))) != 0
}

// GP (289): Merkle function
fun merkle(kvMap: Map<ByteArray, ByteArray>, i: Int = 0): ByteArray {
    if (kvMap.isEmpty()) {
        return ByteArray(32) { 0 }
    }

    val encoded = if (kvMap.size == 1) {
        val entry = kvMap.entries.first()
        leaf(entry.key, entry.value)
    } else {
        val l = mutableMapOf<ByteArray, ByteArray>()
        val r = mutableMapOf<ByteArray, ByteArray>()

        for ((k, v) in kvMap) {
            if (bit(k, i)) {
                r[k] = v
            } else {
                l[k] = v
            }
        }
        branch(merkle(l, i + 1), merkle(r, i + 1))
    }
    require(encoded.size == 64) { "Encoded length must be 64 bytes" }
    return blakeHash(encoded)
}
