package io.forge.jam.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

// Convert ByteArray to hex string
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

// Convert hex string to JamByteArray
fun String.hexToJamBytes(): JamByteArray {
    val hex = this.removePrefix("0x")
    val len = hex.length
    require(len % 2 == 0) { "Hex string must have even length" }
    return JamByteArray(ByteArray(len / 2) { i ->
        hex.substring(2 * i, 2 * i + 2).toInt(16).toByte()
    })
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

fun <T : Encodable> encodeNullable(item: T?): ByteArray {
    return if (item == null) {
        byteArrayOf(0)
    } else {
        byteArrayOf(1) + item.encode()
    }
}

/**
 * Encode a list of non-optional Encodable items with compact integer (variable-width) length prefix.
 * This follows the JAM codec specification where arrays use variable-width length encoding.
 * Set includeLength=false for fixed-size arrays that don't need a length prefix.
 */
fun <T : Encodable> encodeList(list: List<T>, includeLength: Boolean = true): ByteArray {
    val itemsBytes = list.fold(ByteArray(0)) { acc, item ->
        acc + item.encode()
    }
    if (includeLength) {
        // JAM codec uses variable-width (compact integer) encoding for array lengths
        val lengthBytes = encodeCompactInteger(list.size.toLong())
        return lengthBytes + itemsBytes
    } else {
        return itemsBytes
    }
}

/**
 * Encode a list of non-optional Encodable items with compact integer length prefix.
 * This is an alias for encodeList with includeLength=true for backwards compatibility.
 */
fun <T : Encodable> encodeListWithCompactLength(list: List<T>): ByteArray {
    return encodeList(list, true)
}

/**
 * Encodes nested lists according to JAM protocol specification.
 * For fixed-size outer arrays (like ConfigFixedSizeArray), set includeLength=false.
 * Inner lists always use compact integer length prefix.
 */
fun <T : Encodable> encodeNestedList(list: List<List<T>>, includeLength: Boolean = true): ByteArray {
    val outerLengthBytes = if (includeLength) {
        // Variable-width length for variable-size outer array
        encodeCompactInteger(list.size.toLong())
    } else {
        // No length prefix for fixed-size outer array
        ByteArray(0)
    }

    val innerListsBytes = list.fold(ByteArray(0)) { acc, innerList ->
        // Inner lists always use compact integer length
        acc + encodeList(innerList, true)
    }

    return outerLengthBytes + innerListsBytes
}

/**
 * Encode a list of optional Encodable items with compact integer length prefix.
 */
fun <T : Encodable> encodeOptionalList(list: List<T?>, includeLength: Boolean = true): ByteArray {
    val itemsBytes = list.fold(ByteArray(0)) { acc, item ->
        acc + if (item == null) {
            byteArrayOf(0)
        } else {
            byteArrayOf(1) + item.encode()
        }
    }

    return if (includeLength) {
        encodeCompactInteger(list.size.toLong()) + itemsBytes
    } else {
        itemsBytes
    }
}

/**
 * Helper to convert Long to little-endian byte array of specified size
 */
fun Long.toByteArray(size: Int): ByteArray {
    val result = ByteArray(size)
    var value = this
    for (i in 0 until size) {
        result[i] = (value and 0xFF).toByte()
        value = value shr 8
    }
    return result
}

fun encodeFixedWidthInteger(value: Number, byteSize: Int = 4, hasDiscriminator: Boolean = true): ByteArray {
    val buffer = ByteBuffer.allocate(byteSize)
        .order(ByteOrder.LITTLE_ENDIAN)

    when (byteSize) {
        1 -> buffer.put(value.toByte())
        2 -> buffer.putShort(value.toShort())
        4 -> buffer.putInt(value.toInt())
        8 -> buffer.putLong(value.toLong())
        else -> throw IllegalArgumentException("Unsupported byte size")
    }

    if (hasDiscriminator) {
        val discriminator = byteArrayOf(16.toByte())
        return buffer.array() + discriminator
    }
    return buffer.array()
}

/**
 * Encodes a nonnegative Long (up to < 2^64) according to
 *
 * Returns the full spec-compliant encoding: prefix byte + E_l(remainder).
 * - If x = 0, returns [0].
 * - If 2^(7l) <= x < 2^(7(l+1)) for some l in [0..8], then
 *     [ 256 - 2^(8-l) + floor(x / 2^(8*l)) ] ++ E_l(x mod 2^(8*l))
 * - Otherwise (x < 2^64), returns [255] ++ E_8(x).
 *
 * Here E_l( r ) means "r in little-endian form over l bytes."
 */
fun encodeCompactInteger(x: Long): ByteArray {
    require(x >= 0) { "No negative values allowed." }
    // 1) Special case x = 0
    if (x == 0L) {
        return byteArrayOf(0)
    }

    // 2) If x < 2^(64), figure out which l fits 2^(7l) <= x < 2^(7(l+1))
    //    for l in [0..8].
    //    (We only go up to l=8 because 2^(7*8) = 2^56 < 2^64.)
    for (l in 0..8) {
        val lowerBound = 1L shl (7 * l)          // 2^(7l)
        val upperBound = 1L shl (7 * (l + 1))    // 2^(7(l+1))
        if (x in lowerBound until upperBound) {
            // prefix = 256 - 2^(8-l) + floor(x / 2^(8*l))
            val prefixVal = (256 - (1 shl (8 - l))) + (x ushr (8 * l))
            val prefixByte = prefixVal.toByte()

            // remainder = x mod 2^(8*l)
            val remainder = x and ((1L shl (8 * l)) - 1)
            // E_l(remainder) -> little-endian representation in l bytes
            val remainderBytes = ByteArray(l)
            for (i in 0 until l) {
                remainderBytes[i] = ((remainder shr (8 * i)) and 0xFF).toByte()
            }
            return byteArrayOf(prefixByte) + remainderBytes
        }
    }

    // 3) Otherwise (still x < 2^64 from the require() above):
    //    [255] ++ E_8(x)
    val prefix = 0xFF.toByte()
    val fullBytes = ByteArray(8)
    for (i in 0 until 8) {
        fullBytes[i] = ((x shr (8 * i)) and 0xFF).toByte()
    }
    return byteArrayOf(prefix) + fullBytes
}


fun encodeOptionalByteArray(value: ByteArray?): ByteArray {
    return if (value == null) {
        // Existence flag for 'absent' value
        byteArrayOf(0)
    } else {
        // Existence flag for 'present' value
        val existenceFlag = byteArrayOf(1.toByte())
        // Combine existence flag and data
        existenceFlag + value
    }
}

/**
 * Decodes a fixed-width little-endian integer from bytes.
 * @param data The byte array to decode from
 * @param offset The starting offset in the byte array
 * @param byteSize The number of bytes to read (1, 2, 4, or 8)
 * @param hasDiscriminator Whether to skip a discriminator byte at the end
 * @return The decoded value as a Long
 */
fun decodeFixedWidthInteger(data: ByteArray, offset: Int, byteSize: Int, hasDiscriminator: Boolean = false): Long {
    var value = 0L
    for (i in 0 until byteSize) {
        value = value or ((data[offset + i].toLong() and 0xFF) shl (8 * i))
    }
    return value
}

/**
 * Decodes a compact integer from bytes.
 * Returns a Pair of (decoded value, number of bytes consumed).
 *
 * Encoding scheme:
 * - l=0: prefix in [1, 127], value = prefix, 1 byte total
 * - l=1: prefix in [128, 191], 2 bytes total
 * - l=2: prefix in [192, 223], 3 bytes total
 * - l=3: prefix in [224, 239], 4 bytes total
 * - l=4: prefix in [240, 247], 5 bytes total
 * - l=5: prefix in [248, 251], 6 bytes total
 * - l=6: prefix in [252, 253], 7 bytes total
 * - l=7: prefix = 254, 8 bytes total
 * - l=8: prefix = 255, 9 bytes total
 * - Special: prefix = 0 means value = 0
 */
fun decodeCompactInteger(data: ByteArray, offset: Int): Pair<Long, Int> {
    if (offset >= data.size) {
        return Pair(0L, 0)
    }

    val prefix = data[offset].toInt() and 0xFF

    // Special case: prefix = 0 means value = 0
    if (prefix == 0) {
        return Pair(0L, 1)
    }

    // Determine l from prefix using the encoding formula:
    // For l=0: prefix = 256 - 256 + x = x, so prefix in [1, 127]
    // For l=1: prefix = 256 - 128 + high = 128 + high, so prefix in [128, 191]
    // For l=2: prefix = 256 - 64 + high = 192 + high, so prefix in [192, 223]
    // etc.
    val l = when {
        prefix < 128 -> 0     // [1, 127]
        prefix < 192 -> 1     // [128, 191]
        prefix < 224 -> 2     // [192, 223]
        prefix < 240 -> 3     // [224, 239]
        prefix < 248 -> 4     // [240, 247]
        prefix < 252 -> 5     // [248, 251]
        prefix < 254 -> 6     // [252, 253]
        prefix < 255 -> 7     // [254]
        else -> 8             // [255]
    }

    // Handle special case where prefix = 255 means 8 bytes follow
    if (prefix == 255) {
        var value = 0L
        for (i in 0 until 8) {
            if (offset + 1 + i < data.size) {
                value = value or ((data[offset + 1 + i].toLong() and 0xFF) shl (8 * i))
            }
        }
        return Pair(value, 9)
    }

    // For l=0, the value is just the prefix itself
    if (l == 0) {
        return Pair(prefix.toLong(), 1)
    }

    // Calculate the high bits from prefix
    // base = 256 - 2^(8-l), so highBits = (prefix - base) << (8*l)
    val base = 256 - (1 shl (8 - l))
    val highBits = (prefix - base).toLong() shl (8 * l)

    // Read the low bytes (l bytes in little-endian)
    var lowBits = 0L
    for (i in 0 until l) {
        if (offset + 1 + i < data.size) {
            lowBits = lowBits or ((data[offset + 1 + i].toLong() and 0xFF) shl (8 * i))
        }
    }

    return Pair(highBits or lowBits, 1 + l)
}

/**
 * Decodes a variable-length list from bytes.
 * The list is prefixed with a compact-encoded length.
 * @param data The byte array to decode from
 * @param offset The starting offset in the byte array
 * @param decoder A function that decodes a single item and returns (item, bytesConsumed)
 * @return Pair of (decoded list, total bytes consumed)
 */
fun <T> decodeList(
    data: ByteArray,
    offset: Int,
    decoder: (ByteArray, Int) -> Pair<T, Int>
): Pair<List<T>, Int> {
    val (length, lengthBytes) = decodeCompactInteger(data, offset)
    var currentOffset = offset + lengthBytes
    val items = mutableListOf<T>()
    for (i in 0 until length.toInt()) {
        val (item, itemBytes) = decoder(data, currentOffset)
        items.add(item)
        currentOffset += itemBytes
    }
    return Pair(items, currentOffset - offset)
}

/**
 * Decodes a fixed-length list from bytes (no length prefix).
 * @param data The byte array to decode from
 * @param offset The starting offset in the byte array
 * @param count The number of items to decode
 * @param itemSize The size of each item in bytes (for fixed-size items)
 * @param decoder A function that decodes a single item at the given offset
 * @return The decoded list
 */
fun <T> decodeFixedList(
    data: ByteArray,
    offset: Int,
    count: Int,
    itemSize: Int,
    decoder: (ByteArray, Int) -> T
): List<T> {
    return (0 until count).map { i ->
        decoder(data, offset + i * itemSize)
    }
}

/**
 * Decodes a fixed-length list from bytes with variable-size items (no length prefix).
 * @param data The byte array to decode from
 * @param offset The starting offset in the byte array
 * @param count The number of items to decode
 * @param decoder A function that decodes a single item and returns (item, bytesConsumed)
 * @return Pair of (decoded list, total bytes consumed)
 */
fun <T> decodeFixedListVariable(
    data: ByteArray,
    offset: Int,
    count: Int,
    decoder: (ByteArray, Int) -> Pair<T, Int>
): Pair<List<T>, Int> {
    var currentOffset = offset
    val items = mutableListOf<T>()
    for (i in 0 until count) {
        val (item, itemBytes) = decoder(data, currentOffset)
        items.add(item)
        currentOffset += itemBytes
    }
    return Pair(items, currentOffset - offset)
}

/**
 * Decodes an optional value from bytes.
 * First byte is 0 (absent) or 1 (present).
 * @param data The byte array to decode from
 * @param offset The starting offset in the byte array
 * @param decoder A function that decodes the value if present
 * @return Pair of (decoded value or null, bytes consumed)
 */
fun <T> decodeOptional(
    data: ByteArray,
    offset: Int,
    decoder: (ByteArray, Int) -> Pair<T, Int>
): Pair<T?, Int> {
    if (offset >= data.size) {
        return Pair(null, 0)
    }
    val flag = data[offset].toInt() and 0xFF
    return if (flag == 0) {
        Pair(null, 1)
    } else {
        val (value, valueBytes) = decoder(data, offset + 1)
        Pair(value, 1 + valueBytes)
    }
}
