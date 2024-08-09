package io.forge.jam.pvm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals

class ProgramBlobTest {

    @Test
    fun testUByteArrayStartsWith() {
        val v = ubyteArrayOf(10u, 40u, 30u)
        assert(v.startsWith(ubyteArrayOf(10u)))
        assert(v.startsWith(ubyteArrayOf(10u, 40u))) // true
        assertNotEquals(v.startsWith(ubyteArrayOf(50u)), true) // false
        assertNotEquals(v.startsWith(ubyteArrayOf(10u, 50u)), true) // false
        assert(v.startsWith(ubyteArrayOf())) // true, empty slice
    }

    @Test
    fun testParseBitmask() {
        fun parseBoth(bitmask: UByteArray, offset: Int): Pair<Int, Int>? {
            val resultFast = parseBitmaskFast(bitmask, offset)
            val resultSlow = parseBitmaskSlow(bitmask, offset)
            assertEquals(resultFast, resultSlow, "Fast and slow parsing results should be equal")
            return resultFast
        }

        assertEquals(Pair(1, 0), parseBoth(ubyteArrayOf(0b00000011u, 0u, 0u, 0u), 0))
        assertEquals(Pair(2, 1), parseBoth(ubyteArrayOf(0b00000101u, 0u, 0u, 0u), 0))
        assertEquals(Pair(7, 6), parseBoth(ubyteArrayOf(0b10000001u, 0u, 0u, 0u), 0))
        assertEquals(Pair(8, 7), parseBoth(ubyteArrayOf(0b00000001u, 1u, 0u, 0u), 0))
        assertEquals(Pair(15, 14), parseBoth(ubyteArrayOf(0b00000001u, (1u shl 7).toUByte(), 0u, 0u), 0))
        assertEquals(Pair(16, 15), parseBoth(ubyteArrayOf(0b00000001u, 0u, 1u, 0u), 0))
        assertEquals(Pair(23, 22), parseBoth(ubyteArrayOf(0b00000001u, 0u, (1u shl 7).toUByte(), 0u), 0))
        assertEquals(Pair(24, 23), parseBoth(ubyteArrayOf(0b00000001u, 0u, 0u, 1u), 0))

        assertEquals(Pair(7, 0), parseBoth(ubyteArrayOf(0b11000000u, 0u, 0u, 0u, 0u), 6))
        assertEquals(Pair(8, 1), parseBoth(ubyteArrayOf(0b01000000u, 1u, 0u, 0u, 0u), 6))

        assertEquals(Pair(8, 0), parseBoth(ubyteArrayOf(0b10000000u, 1u, 0u, 0u, 0u), 7))
        assertEquals(Pair(9, 1), parseBoth(ubyteArrayOf(0b10000000u, (1u shl 1).toUByte(), 0u, 0u, 0u), 7))

        assertEquals(Pair(25, 24), parseBoth(ubyteArrayOf(0u, 0u, 0u, 0u, 0b00000001u), 0))
        assertEquals(Pair(31, 24), parseBoth(ubyteArrayOf(0u, 0u, 0u, 0u, 0b00000001u), 6))
        assertEquals(Pair(32, 24), parseBoth(ubyteArrayOf(0u, 0u, 0u, 0u, 0b00000001u), 7))
    }
}
