package io.forge.jam.pvm

import io.forge.jam.pvm.program.Program
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

/**
 * Test utilities and test cases for Program functionality verification
 */
class ProgramTest {
    companion object {
        /**
         * Clamps a value within a given range
         */
        private fun <T : Comparable<T>> clamp(range: ClosedRange<T>, value: T): T = when {
            value < range.start -> range.start
            value > range.endInclusive -> range.endInclusive
            else -> value
        }

        /**
         * Reads bytes from a slice with given offset and length
         */
        private fun read(slice: ByteArray, offset: Int, length: Int): UInt {
            val subSlice = slice.slice(offset until offset + length)
            return when (length) {
                0 -> 0u
                1 -> subSlice[0].toUByte().toUInt()
                2 -> (subSlice[0].toUByte().toUInt() or
                    (subSlice[1].toUByte().toUInt() shl 8))

                3 -> (subSlice[0].toUByte().toUInt() or
                    (subSlice[1].toUByte().toUInt() shl 8) or
                    (subSlice[2].toUByte().toUInt() shl 16))

                4 -> (subSlice[0].toUByte().toUInt() or
                    (subSlice[1].toUByte().toUInt() shl 8) or
                    (subSlice[2].toUByte().toUInt() shl 16) or
                    (subSlice[3].toUByte().toUInt() shl 24))

                else -> throw IllegalArgumentException("Invalid length: $length")
            }
        }

        /**
         * Sign extends a value based on its length
         */
        private fun sext(value: UInt, length: Int): UInt = when (length) {
            0 -> 0u
            1 -> value.toByte().toInt().toUInt()
            2 -> value.toShort().toInt().toUInt()
            3 -> (((value shl 8).toInt() shr 8)).toUInt()
            4 -> value
            else -> throw IllegalArgumentException("Invalid length: $length")
        }
    }

    @Test
    fun verifyReadArgsImm() {
        fun simpleReadArgsImm(code: ByteArray, skip: UInt): UInt {
            val immLength = minOf(4, skip.toInt())
            return sext(read(code, 0, immLength), immLength)
        }

        // Test with various inputs
        val testCases = listOf(
            Triple(byteArrayOf(1, 2, 3, 4), 2u, generateChunk(1, 2, 3, 4)),
            Triple(byteArrayOf(5, 6, 7, 8), 3u, generateChunk(5, 6, 7, 8)),
            Triple(byteArrayOf(9, 10, 11, 12), 4u, generateChunk(9, 10, 11, 12))
        )

        for ((code, skip, chunk) in testCases) {
            assertEquals(
                simpleReadArgsImm(code, skip),
                Program.readArgsImm(chunk, skip)
            )
        }
    }

    @Test
    fun verifyReadArgsImm2() {
        fun simpleReadArgsImm2(code: ByteArray, skip: Int): Pair<UInt, UInt> {
            val imm1Length = minOf(4, code[0].toInt() and 0b111)
            val imm2Length = clamp(0..4, skip - imm1Length - 1)
            val imm1 = sext(read(code, 1, imm1Length), imm1Length)
            val imm2 = sext(read(code, 1 + imm1Length, imm2Length), imm2Length)
            return Pair(imm1, imm2)
        }

        val testCases = listOf(
            Triple(byteArrayOf(3, 1, 2, 3, 4), 5, generateChunk(3, 1, 2, 3, 4)),
            Triple(byteArrayOf(2, 5, 6, 7, 8), 4, generateChunk(2, 5, 6, 7, 8))
        )

        for ((code, skip, chunk) in testCases) {
            assertEquals(
                simpleReadArgsImm2(code, skip),
                Program.readArgsImm2(chunk, skip.toUInt())
            )
        }
    }

    @Test
    fun verifyReadArgsRegImm() {
        fun simpleReadArgsRegImm(code: ByteArray, skip: Int): Pair<UByte, UInt> {
            val reg = minOf(12, code[0].toInt() and 0b1111).toUByte()
            val immLength = clamp(0..4, skip - 1)
            val imm = sext(read(code, 1, immLength), immLength)
            return Pair(reg, imm)
        }

        val testCases = listOf(
            Triple(byteArrayOf(3, 1, 2, 3), 3, generateChunk(3, 1, 2, 3)),
            Triple(byteArrayOf(7, 4, 5, 6), 4, generateChunk(7, 4, 5, 6))
        )

        for ((code, skip, chunk) in testCases) {
            val (expectedReg, expectedImm) = simpleReadArgsRegImm(code, skip)
            val (actualReg, actualImm) = Program.readArgsRegImm(chunk, skip.toUInt())
            assertEquals(expectedReg.toInt(), actualReg.rawUnparsed().toInt() and 0b1111)
            assertEquals(expectedImm, actualImm)
        }
    }

    @Test
    fun verifyReadArgsRegs3() {
        fun simpleReadArgsRegs3(code: ByteArray): Triple<UByte, UByte, UByte> {
            val reg2 = minOf(12, code[0].toInt() and 0b1111).toUByte()
            val reg3 = minOf(12, (code[0].toInt() shr 4) and 0b1111).toUByte()
            val reg1 = minOf(12, code[1].toInt() and 0b1111).toUByte()
            return Triple(reg1, reg2, reg3)
        }

        val testCases = listOf(
            Pair(byteArrayOf(0x23, 0x01), generateChunk(0x23, 0x01)),
            Pair(byteArrayOf(0x45, 0x02), generateChunk(0x45, 0x02))
        )

        for ((code, chunk) in testCases) {
            val (expectedReg1, expectedReg2, expectedReg3) = simpleReadArgsRegs3(code)
            val (actualReg1, actualReg2, actualReg3) = Program.readArgsRegs3(chunk)
            assertEquals(expectedReg1.toInt(), actualReg1.rawUnparsed().toInt() and 0b1111)
            assertEquals(expectedReg2.toInt(), actualReg2.rawUnparsed().toInt() and 0b1111)
            assertEquals(expectedReg3.toInt(), actualReg3.rawUnparsed().toInt() and 0b1111)
        }
    }

    /**
     * Helper function to generate a chunk from bytes
     */
    private fun generateChunk(vararg bytes: Int): U128 {
        var low = 0UL
        for (i in 0 until minOf(8, bytes.size)) {
            low = low or ((bytes[i].toULong() and 0xFFUL) shl (i * 8))
        }

        var high = 0UL
        for (i in 8 until minOf(16, bytes.size)) {
            high = high or ((bytes[i].toULong() and 0xFFUL) shl ((i - 8) * 8))
        }

        return U128(low, high)
    }
}
