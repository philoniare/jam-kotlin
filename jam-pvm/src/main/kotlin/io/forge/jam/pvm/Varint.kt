package io.forge.jam.pvm

const val MAX_VARINT_LENGTH: UInt = 5u

fun UInt.countLeadingOneBits(): Int {
    return (-this.toInt()).countLeadingZeroBits()
}

fun ULong.countLeadingOneBits(): Int {
    return (-this.toLong()).countLeadingZeroBits()
}

fun UByteArray.toUIntLittleEndian(length: Int): UInt {
    require(length in 1..4) { "Length must be between 1 and 4" }
    var result = 0u
    for (i in 0 until length) {
        result = result or (this[i].toUInt() shl (8 * i))
    }
    return result
}

fun UInt.toLittleEndianByteArray(): UByteArray {
    return ubyteArrayOf(
        (this and 0xFFu).toUByte(),
        ((this shr 8) and 0xFFu).toUByte(),
        ((this shr 16) and 0xFFu).toUByte(),
        ((this shr 24) and 0xFFu).toUByte()
    )
}

fun getVarintLength(leadingZeroes: UInt): UInt {
    val bitsRequired = 32u - leadingZeroes
    val x = bitsRequired shr 3
    return ((x + bitsRequired) xor x) shr 3
}

fun writeVarint(value: UInt, buffer: UByteArray): UInt {
    val varintLength = getVarintLength(value.countLeadingZeroBits().toUInt())

    when (varintLength) {
        0u -> {
            buffer[0] = value.toUByte()
        }

        1u -> {
            buffer[0] = (0b10000000u or (value shr 8)).toUByte()
            buffer[1] = (value and 0xFFu).toUByte()
        }

        2u -> {
            buffer[0] = (0b11000000u or (value shr 16)).toUByte()
            buffer[1] = ((value shr 8) and 0xFFu).toUByte()
            buffer[2] = (value and 0xFFu).toUByte()
        }

        3u -> {
            buffer[0] = (0b11100000u or (value shr 24)).toUByte()
            buffer[1] = ((value shr 16) and 0xFFu).toUByte()
            buffer[2] = ((value shr 8) and 0xFFu).toUByte()
            buffer[3] = (value and 0xFFu).toUByte()
        }

        else -> {
            buffer[0] = 0b11110000u.toUByte()
            buffer[1] = (value and 0xFFu).toUByte()
            buffer[2] = ((value shr 8) and 0xFFu).toUByte()
            buffer[3] = ((value shr 16) and 0xFFu).toUByte()
            buffer[4] = ((value shr 24) and 0xFFu).toUByte()
        }
    }

    return varintLength + 1u
}

fun readVarint(input: UByteArray, firstByte: UByte): Pair<UInt, UInt>? {
    val length = firstByte.inv().countLeadingZeroBits()

    if (input.size < length) {
        return null
    }

    val value = when (length) {
        0 -> firstByte.toUInt()
        1 -> ((firstByte and 0x7Fu).toUInt() shl 8) or input[0].toUInt()
        2 -> ((firstByte and 0x3Fu).toUInt() shl 16) or (input[0].toUInt() shl 8) or input[1].toUInt()
        3 -> ((firstByte and 0x1Fu).toUInt() shl 24) or (input[0].toUInt() shl 16) or (input[1].toUInt() shl 8) or input[2].toUInt()
        4 -> input[0].toUInt() or (input[1].toUInt() shl 8) or (input[2].toUInt() shl 16) or (input[3].toUInt() shl 24)
        else -> {
            return null
        }
    }

    return Pair(length.toUInt(), value)
}

val LENGTH_TO_SHIFT: UIntArray = UIntArray(256) { length ->
    when (length) {
        0 -> 32u
        1 -> 24u
        2 -> 16u
        3 -> 8u
        else -> 0u
    }
}

fun readSimpleVarint(chunk: UInt, length: UInt): UInt {
    val shift = LENGTH_TO_SHIFT[length.toInt()]
    return (chunk.toULong() shl shift.toInt()).toUInt().toInt().shr(shift.toInt()).toUInt()
}

fun writeSimpleVarint(value: UInt, buffer: UByteArray): UInt {
    val varintLength = getBytesRequired(value)
    when (varintLength) {
        0u -> {}
        1u -> {
            buffer[0] = value.toUByte()
        }

        2u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
        }

        3u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
        }

        4u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
            buffer[3] = (value shr 24).toUByte()
        }

        else -> throw IllegalStateException("Unexpected varint length")
    }

    return varintLength
}

fun writeSimpleVarint64(value: ULong, buffer: UByteArray): UInt {
    val varintLength = getBytesRequired64(value)
    when (varintLength) {
        0u -> {}
        1u -> {
            buffer[0] = value.toUByte()
        }

        2u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
        }

        3u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
        }

        4u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
            buffer[3] = (value shr 24).toUByte()
        }

        5u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
            buffer[3] = (value shr 24).toUByte()
            buffer[4] = (value shr 32).toUByte()
        }

        6u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
            buffer[3] = (value shr 24).toUByte()
            buffer[4] = (value shr 32).toUByte()
            buffer[5] = (value shr 40).toUByte()
        }

        7u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
            buffer[3] = (value shr 24).toUByte()
            buffer[4] = (value shr 32).toUByte()
            buffer[5] = (value shr 40).toUByte()
            buffer[6] = (value shr 48).toUByte()
        }

        8u -> {
            buffer[0] = value.toUByte()
            buffer[1] = (value shr 8).toUByte()
            buffer[2] = (value shr 16).toUByte()
            buffer[3] = (value shr 24).toUByte()
            buffer[4] = (value shr 32).toUByte()
            buffer[5] = (value shr 40).toUByte()
            buffer[6] = (value shr 48).toUByte()
            buffer[7] = (value shr 56).toUByte()
        }

        else -> throw IllegalStateException("Unexpected varint length")
    }

    return varintLength
}


fun getBytesRequired(value: UInt): UInt {
    val zeros = value.countLeadingZeroBits()
    return when {
        zeros == 32 -> 0u
        zeros > 24 -> 1u
        zeros > 16 -> 2u
        zeros > 8 -> 3u
        zeros != 0 -> 4u
        else -> {
            val ones = value.countLeadingOneBits()
            when {
                ones > 24 -> 1u
                ones > 16 -> 2u
                ones > 8 -> 3u
                else -> 4u
            }
        }
    }
}

fun getBytesRequired64(value: ULong): UInt {
    val zeros = value.countLeadingZeroBits()
    return when {
        zeros == 64 -> 0u
        zeros > 56 -> 1u
        zeros > 48 -> 2u
        zeros > 40 -> 3u
        zeros > 32 -> 4u
        zeros > 24 -> 5u
        zeros > 16 -> 6u
        zeros > 8 -> 7u
        zeros != 0 -> 8u
        else -> {
            val ones = value.countLeadingOneBits()
            when {
                ones > 56 -> 1u
                ones > 48 -> 2u
                ones > 40 -> 3u
                ones > 32 -> 4u
                ones > 24 -> 5u
                ones > 16 -> 6u
                ones > 8 -> 7u
                else -> 8u
            }
        }
    }
}
