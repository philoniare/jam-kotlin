package io.forge.jam.pvm

import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

fun UByteArray.toUInt(): UInt {
    require(this.size >= 4) { "ByteArray must have at least 4 bytes" }
    return (this[0].toUInt() and 0xFFu) or
        ((this[1].toUInt() and 0xFFu) shl 8) or
        ((this[2].toUInt() and 0xFFu) shl 16) or
        ((this[3].toUInt() and 0xFFu) shl 24)
}

fun readSimpleVarintFromByteArray(input: UByteArray, length: UInt): UInt {
    val chunk = input.toUInt()
    return readSimpleVarint(chunk, length.toUInt())
}

class VarintTest {
    @Test
    fun testSimpleVarint() {
        assertEquals(getBytesRequired(0b00000000_00000000_00000000_00000000u), 0u)
        assertEquals(getBytesRequired(0b00000000_00000000_00000000_00000001u), 1u)
        assertEquals(getBytesRequired(0b00000000_00000000_00000000_01000001u), 1u)
        assertEquals(getBytesRequired(0b00000000_00000000_00000000_10000000u), 2u)
        assertEquals(getBytesRequired(0b00000000_00000000_00000000_11111111u), 2u)
        assertEquals(getBytesRequired(0b00000000_00000000_00000001_00000000u), 2u)
        assertEquals(getBytesRequired(0b00000000_00000000_01000000_00000000u), 2u)
        assertEquals(getBytesRequired(0b00000000_00000000_10000000_00000000u), 3u)
        assertEquals(getBytesRequired(0b00000000_00000001_00000000_00000000u), 3u)
        assertEquals(getBytesRequired(0b00000000_01000000_00000000_00000000u), 3u)
        assertEquals(getBytesRequired(0b00000000_10000000_00000000_00000000u), 4u)
        assertEquals(getBytesRequired(0b00000001_00000000_00000000_00000000u), 4u)
        assertEquals(getBytesRequired(0b10000000_00000000_00000000_00000000u), 4u)
        assertEquals(getBytesRequired(0b11111111_11111111_11111111_11111111u), 1u)
        assertEquals(getBytesRequired(0b10111111_11111111_11111111_11111111u), 4u)
        assertEquals(getBytesRequired(0b11111110_11111111_11111111_11111111u), 4u)
        assertEquals(getBytesRequired(0b11111111_01111111_11111111_11111111u), 4u)
        assertEquals(getBytesRequired(0b11111111_10111111_11111111_11111111u), 3u)
        assertEquals(getBytesRequired(0b11111111_11111110_11111111_11111111u), 3u)
        assertEquals(getBytesRequired(0b11111111_11111111_01111111_11111111u), 3u)
        assertEquals(getBytesRequired(0b11111111_11111111_10111111_11111111u), 2u)
        assertEquals(getBytesRequired(0b11111111_11111111_11111110_11111111u), 2u)
        assertEquals(getBytesRequired(0b11111111_11111111_11111111_01111111u), 2u)
        assertEquals(getBytesRequired(0b11111111_11111111_11111111_10111111u), 1u)

        assertEquals(0xffffffffu, readSimpleVarint(0x000000ffu, 1u))
        assertEquals(0xffffffffu, readSimpleVarint(0x555555ffu, 1u))
        assertEquals(0xffffffffu, readSimpleVarint(0xaaaaaaffu, 1u))
        assertEquals(0xffffffffu, readSimpleVarint(0xffffffffu, 1u))

        assertEquals(0u, readSimpleVarint(0x000000ffu, 0u))
        assertEquals(0u, readSimpleVarint(0x555555ffu, 0u))
        assertEquals(0u, readSimpleVarint(0xaaaaaaffu, 0u))
        assertEquals(0u, readSimpleVarint(0xffffffffu, 0u))

        repeat(10000) {
            val value = Random.nextUInt()

            val buffer = UByteArray(MAX_VARINT_LENGTH.toInt())
            val length = writeVarint(value, buffer)

            val (parsedLength, parsedValue) = readVarint(buffer.sliceArray(1 until buffer.size), buffer[0])
                ?: fail("Failed to parse varint")

            assertEquals(value, parsedValue, "Value mismatch for input: $value")
            assertEquals(length, parsedLength + 1u, "Length mismatch for input: $value")
        }

        repeat(10000) { // Run 10000 random tests
            val value = Random.nextUInt()
            listOf<UByte>(0x00u, 0x55u, 0xAAu, 0xFFu).forEach { fillByte ->
                val t = UByteArray(4) { fillByte }
                val length = writeSimpleVarint(value, t)
                val readValue = readSimpleVarintFromByteArray(t, length.toUInt())
                assertEquals(value, readValue, "Mismatch for value $value with fill byte $fillByte")
            }
        }
    }
}
