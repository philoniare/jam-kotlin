package io.forge.jam.safrole.accumulation

import io.forge.jam.pvm.engine.InstructionSetKind
import io.forge.jam.pvm.program.ArcBytes
import io.forge.jam.pvm.program.ProgramBlob
import io.forge.jam.pvm.program.ProgramParseError
import io.forge.jam.pvm.program.ProgramParts
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProgramCodeTest {

    private fun parse(blob: ByteArray): ProgramBlob {
        val parts = ProgramParts(
            isaKind = InstructionSetKind.Latest64,
            codeAndJumpTable = ArcBytes.fromStatic(blob)
        )
        return ProgramBlob.fromParts(parts).getOrThrow()
    }

    @Test
    fun empty() {
        val blob = byteArrayOf()
        assertThrows(ProgramParseError::class.java) {
            parse(blob)
        }
    }

    @Test
    fun invalidJumpTableEntriesCount() {
        val blob = byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x08, 0, 0)
        assertThrows(ProgramParseError::class.java) {
            parse(blob)
        }
    }

    @Test
    fun minimal() {
        val blob = byteArrayOf(0, 0, 0)
        val program = parse(blob)
        assertEquals(0.toByte(), program.jumpTableEntrySize)
        assertEquals(0, program.jumpTable().len().toInt())
        assertEquals(0, program.code.toByteArray().size)
    }

    @Test
    fun simple() {
        val blob = byteArrayOf(0, 0, 2, 1, 2, 0)
        val program = parse(blob)
        assertEquals(0.toByte(), program.jumpTableEntrySize)
        assertEquals(0, program.jumpTable().len().toInt())
        assertEquals(2, program.code.toByteArray().size)
        assertArrayEquals(byteArrayOf(1, 2), program.code.toByteArray())
    }

    @Test
    fun parseProgramCodeFibonacci() {
        val blob = byteArrayOf(
            0,
            0,
            33,
            51,
            8,
            1,
            51,
            9,
            1,
            40,
            3,
            0,
            149.toByte(),
            119,
            255.toByte(),
            81,
            7,
            12,
            100,
            138.toByte(),
            200.toByte(),
            152.toByte(),
            8,
            100,
            169.toByte(),
            40,
            243.toByte(),
            100,
            135.toByte(),
            51,
            8,
            51,
            9,
            1,
            50,
            0,
            73,
            147.toByte(),
            82,
            213.toByte(),
            0
        )
        val program = parse(blob)
        assertEquals(0.toByte(), program.jumpTableEntrySize)
        assertEquals(0, program.jumpTable().len().toInt())
        // Code length is 33 (0x21)
        assertEquals(33, program.code.toByteArray().size)

        // Verify code content (offset 3 to 3+33)
        val expectedCode = blob.copyOfRange(3, 3 + 33)
        assertArrayEquals(expectedCode, program.code.toByteArray())
    }

    @Test
    fun invalidJumpTableEncodeSize() {
        val blob = byteArrayOf(1, 0xFF.toByte(), 0, 0)
        assertThrows(ProgramParseError::class.java) {
            parse(blob)
        }
    }

    @Test
    fun invalidCodeLength() {
        val blob = byteArrayOf(0, 0, 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x08)
        assertThrows(ProgramParseError::class.java) {
            parse(blob)
        }
    }

    @Test
    fun tooMuchData() {
        val blob = byteArrayOf(0, 0, 2, 1, 2, 0, 0)
        assertThrows(ProgramParseError::class.java) {
            parse(blob)
        }
    }

    @Test
    fun tooLittleData() {
        val blob = byteArrayOf(0, 0, 2, 1, 2)
        assertThrows(ProgramParseError::class.java) {
            parse(blob)
        }
    }
}
