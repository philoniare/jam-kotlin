package io.forge.jam.core

// Convert ByteArray to hex string
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

// Convert hex string to ByteArray
fun String.hexToBytes(): ByteArray {
    val hex = this.removePrefix("0x")
    val len = hex.length
    require(len % 2 == 0) { "Hex string must have even length" }
    return ByteArray(len / 2) { i ->
        hex.substring(2 * i, 2 * i + 2).toInt(16).toByte()
    }
}

// Convert Integer to Little Endian Bytes
fun Int.toLEBytes(size: Int = 4): ByteArray {
    val bytes = ByteArray(size)
    for (i in 0 until size) {
        bytes[i] = ((this shr (8 * i)) and 0xFF).toByte()
    }
    return bytes
}

// Convert Boolean to Byte
fun Boolean.toByte(): Byte = if (this) 1.toByte() else 0.toByte()

// Encode a list of Encodable items with length prefix
fun <T : Encodable> encodeList(list: List<T>, includeLength: Boolean = true): ByteArray {
    val itemsBytes = list.fold(ByteArray(0)) { acc, item ->
        acc + item.encode()
    }
    if (includeLength) {
        val lengthBytes = encodeCompactInteger(list.size)
        return lengthBytes + itemsBytes
    } else {
        return itemsBytes
    }
}

fun encodeCompactInteger(value: Int): ByteArray {
    return when {
        value < (1 shl 6) -> {
            // Single-byte mode
            byteArrayOf((value shl 2).toByte())
        }

        value < (1 shl 14) -> {
            // Two-byte mode
            val v = (value shl 2) or 0x01
            byteArrayOf(
                (v and 0xFF).toByte(),
                ((v shr 8) and 0xFF).toByte()
            )
        }

        value < (1 shl 30) -> {
            // Four-byte mode
            val v = (value shl 2) or 0x02
            byteArrayOf(
                (v and 0xFF).toByte(),
                ((v shr 8) and 0xFF).toByte(),
                ((v shr 16) and 0xFF).toByte(),
                ((v shr 24) and 0xFF).toByte()
            )
        }

        else -> {
            // Big-integer mode (Not needed for Int)
            throw IllegalArgumentException("Value too large for compact encoding")
        }
    }
}

