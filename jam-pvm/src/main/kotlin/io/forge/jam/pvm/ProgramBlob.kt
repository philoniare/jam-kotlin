package io.forge.jam.pvm

import java.nio.ByteBuffer
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import kotlin.reflect.KMutableProperty0

typealias LookupEntry = Int

const val EMPTY_LOOKUP_ENTRY: LookupEntry = 0

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class LookupTable(private val entries: IntArray) {
    companion object {
        private const val MAX_SKIP = 0b11111
        private const val MAX_AUX = 0b111

        private fun pack(imm1Bits: Int, imm1Skip: Int, imm2Bits: Int): LookupEntry {
            require(imm1Bits <= 0b111111) { "imm1Bits out of range" }
            require(imm2Bits <= 0b111111) { "imm2Bits out of range" }
            require(imm1Skip <= 0b111111) { "imm1Skip out of range" }
            return imm1Bits or (imm1Skip shl 6) or (imm2Bits shl 12)
        }

        private fun unpack(entry: LookupEntry): Triple<Int, Int, Int> {
            val imm1Bits = entry and 0b111111
            val imm1Skip = (entry shr 6) and 0b111111
            val imm2Bits = (entry shr 12) and 0b111111
            return Triple(imm1Bits, imm1Skip, imm2Bits)
        }

        private fun minU32(a: Int, b: Int): Int = if (a < b) a else b

        private fun clampI32(range: IntRange, value: Int): Int {
            return when {
                value < range.first -> range.first
                value > range.last -> range.last
                else -> value
            }
        }

        private fun signExtendCutoffForLength(length: Int): Int {
            return when (length) {
                0 -> 32
                1 -> 24
                2 -> 16
                3 -> 8
                4 -> 0
                else -> error("Invalid length")
            }
        }

        private fun getLookupIndex(skip: Int, aux: Int): Int {
            require(skip <= MAX_SKIP) { "skip out of range" }
            val index = skip or ((aux and MAX_AUX) shl 5)
            require(index <= 0xff) { "index out of range" }
            return index
        }

        fun build(offset: Int): LookupTable {
            val output = IntArray(256) { EMPTY_LOOKUP_ENTRY }
            var skip = 0
            while (skip <= MAX_SKIP) {
                var aux = 0
                while (aux <= MAX_AUX) {
                    val imm1Length = minU32(4, aux)
                    val imm2Length = clampI32(0..4, skip - imm1Length - offset)
                    val imm1Bits = signExtendCutoffForLength(imm1Length)
                    val imm2Bits = signExtendCutoffForLength(imm2Length)
                    val imm1Skip = imm1Length * 8

                    val index = getLookupIndex(skip, aux)
                    output[index] = pack(imm1Bits, imm1Skip, imm2Bits)
                    aux += 1
                }
                skip += 1
            }
            return LookupTable(output)
        }
    }

    fun get(skip: Int, aux: Int): Triple<Int, Int, Int> {
        val index = getLookupIndex(skip, aux)
        require(index in entries.indices) { "index out of range" }
        return unpack(entries[index])
    }
}

val TABLE_1 = LookupTable.build(1)
val TABLE_2 = LookupTable.build(2)

// Define UByteArray extension
fun UByteArray.startsWith(needle: UByteArray): Boolean {
    val n = needle.size
    return this.size >= n && this.copyOfRange(0, n).contentEquals(needle)
}

// The magic bytes with which every program blob must start with.
val BLOB_MAGIC = byteArrayOf('P'.code.toByte(), 'V'.code.toByte(), 'M'.code.toByte(), 0)

val MAX_INSTRUCTION_LENGTH: UInt = 2u + MAX_VARINT_LENGTH * 2u

const val SECTION_MEMORY_CONFIG: UByte = 1u
const val SECTION_RO_DATA: UByte = 2u
const val SECTION_RW_DATA: UByte = 3u
const val SECTION_IMPORTS: UByte = 4u
const val SECTION_EXPORTS: UByte = 5u
const val SECTION_CODE_AND_JUMP_TABLE: UByte = 6u
const val SECTION_OPT_DEBUG_STRINGS: UByte = 128u
const val SECTION_OPT_DEBUG_LINE_PROGRAMS: UByte = 129u
const val SECTION_OPT_DEBUG_LINE_PROGRAM_RANGES: UByte = 130u
const val SECTION_END_OF_FILE: UByte = 0u

const val BLOB_LEN_SIZE = 8
const val BLOB_VERSION_V1_64: UByte = 0u
const val BLOB_VERSION_V1_32: UByte = 1u

const val VERSION_DEBUG_LINE_PROGRAM_V1: UByte = 1u

const val BITMASK_MAX: Int = 24

const val INSTRUCTION_LIMIT_PER_REGION: Int = 512

fun ByteArray.readUInt32LE(offset: Int): UInt {
    return ((this[offset].toUInt() and 0xFFU) or
        ((this[offset + 1].toUInt() and 0xFFU) shl 8) or
        ((this[offset + 2].toUInt() and 0xFFU) shl 16) or
        ((this[offset + 3].toUInt() and 0xFFU) shl 24))
}

fun ByteArray.toULongLE(): ULong {
    if (this.size < 8) throw IllegalArgumentException("ByteArray is too short to convert to ULong")
    return (this[0].toULong() and 0xFFuL) or
        ((this[1].toULong() and 0xFFuL) shl 8) or
        ((this[2].toULong() and 0xFFuL) shl 16) or
        ((this[3].toULong() and 0xFFuL) shl 24) or
        ((this[4].toULong() and 0xFFuL) shl 32) or
        ((this[5].toULong() and 0xFFuL) shl 40) or
        ((this[6].toULong() and 0xFFuL) shl 48) or
        ((this[7].toULong() and 0xFFuL) shl 56)
}

fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (this.size < prefix.size) return false
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) return false
    }
    return true
}

class ByteArrayReader(val data: ByteArray) {
    var position: Int = 0

    fun readByte(): UByte? {
        return if (position < data.size) data[position++].toUByte() else null
    }

    fun readVarInt(): UInt {
        var result = 0U
        var shift = 0
        while (true) {
            val byte = readByte()?.toUInt() ?: throw ProgramParseError.failedToReadVarint(shift)
            result = result or ((byte and 0x7FU) shl shift)
            if ((byte and 0x80U) == 0U) break
            shift += 7
        }
        return result
    }
}

fun signExtendAt(value: Int, bits: Int): Int {
    val shift = 32 - bits
    return (value shl shift) shr shift
}

fun readSimpleVarint(chunk: UInt, length: Int): Int {
    val shift = 32 - length
    val shifted = chunk.toInt() shl shift
    return shifted shr shift
}

fun readArgsImm(chunk: UInt, skip: Int): Int {
    return readSimpleVarint(chunk, skip)
}

fun readArgsOffset(chunk: UInt, instructionOffset: Int, skip: Int): Int {
    val imm = readArgsImm(chunk, skip)
    return instructionOffset + imm
}

fun readArgsImm2(chunk: ULong, skip: Int): Pair<Int, Int> {
    val aux = (chunk and 0xFFu).toUInt().toInt() // Assuming `aux` is derived from the first byte
    val (imm1Bits, imm1Skip, imm2Bits) = TABLE_1.get(skip, aux)
    var chunkShifted = chunk shr 8 // Skip the opcode (1 byte)
    val imm1 = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), imm1Bits)
    chunkShifted = chunkShifted shr imm1Skip
    val imm2 = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), imm2Bits)
    return Pair(imm1, imm2)
}

fun readArgsRegImm(chunk: ULong, skip: Int): Pair<Int, Int> {
    val reg = (chunk and 0xFFu).toInt() // Register is in the first byte after opcode
    val aux = 0 // As per the Rust code
    val (_, _, immBits) = TABLE_1.get(skip, aux)
    val chunkShifted = chunk shr 8 // Skip opcode and reg
    val imm = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), immBits)
    return Pair(reg, imm)
}

fun readArgsRegImm2(chunk: ULong, skip: Int): Triple<Int, Int, Int> {
    val reg = (chunk and 0xFFu).toInt() // Register is in the first byte after opcode
    val aux = ((chunk shr 4) and 0xFFu).toUInt().toInt()
    val (imm1Bits, imm1Skip, imm2Bits) = TABLE_1.get(skip, aux)
    var chunkShifted = chunk shr 8 // Skip opcode and reg
    val imm1 = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), imm1Bits)
    chunkShifted = chunkShifted shr imm1Skip
    val imm2 = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), imm2Bits)
    return Triple(reg, imm1, imm2)
}

fun readArgsRegImmOffset(chunk: ULong, instructionOffset: Int, skip: Int): Triple<Int, Int, Int> {
    val (reg, imm1, imm2) = readArgsRegImm2(chunk, skip)
    val offset = instructionOffset + imm2
    return Triple(reg, imm1, offset)
}

fun readArgsRegs2Imm2(chunk: ULong, skip: Int): Quadruple<Int, Int, Int, Int> {
    val value = chunk.toUInt()
    val reg1 = (value and 0xFu).toInt() // First 4 bits
    val reg2 = ((value shr 4) and 0xFu).toInt() // Next 4 bits
    val imm1Aux = ((value shr 8) and 0xFFu).toInt()
    val (imm1Bits, imm1Skip, imm2Bits) = TABLE_2.get(skip, imm1Aux)
    var chunkShifted = chunk shr 16 // Skip opcode, reg1, reg2, and imm1Aux
    val imm1 = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), imm1Bits)
    chunkShifted = chunkShifted shr imm1Skip
    val imm2 = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), imm2Bits)
    return Quadruple(reg1, reg2, imm1, imm2)
}

fun readArgsRegs2Imm(chunk: ULong, skip: Int): Triple<Int, Int, Int> {
    val value = chunk.toUInt()
    val reg1 = (value and 0xFu).toInt()
    val reg2 = ((value shr 4) and 0xFu).toInt()
    val aux = 0 // As per the Rust code
    val (_, _, immBits) = TABLE_1.get(skip, aux)
    val chunkShifted = chunk shr 8 // Skip opcode and regs
    val imm = signExtendAt((chunkShifted.toUInt() and 0xFFFFFFFFu).toInt(), immBits)
    return Triple(reg1, reg2, imm)
}

fun readArgsRegs2Offset(chunk: ULong, instructionOffset: Int, skip: Int): Triple<Int, Int, Int> {
    val (reg1, reg2, imm) = readArgsRegs2Imm(chunk, skip)
    val offset = instructionOffset + imm
    return Triple(reg1, reg2, offset)
}

fun readArgsRegs3(chunk: UInt): Triple<Int, Int, Int> {
    val value = chunk
    val reg1 = ((value shr 8) and 0xFu).toInt()
    val reg2 = (value and 0xFu).toInt()
    val reg3 = ((value shr 4) and 0xFu).toInt()
    return Triple(reg1, reg2, reg3)
}

fun readArgsRegs2(chunk: UInt): Pair<Int, Int> {
    val value = chunk
    val reg1 = (value and 0xFu).toInt()
    val reg2 = ((value shr 4) and 0xFu).toInt()
    return Pair(reg1, reg2)
}

fun chunkToULong(chunk: ByteArray): ULong {
    var result = 0UL
    val length = minOf(8, chunk.size)
    for (i in 0 until length) {
        result = result or ((chunk[i].toULong() and 0xFFUL) shl (i * 8))
    }
    return result
}

class EnumVisitor<I : InstructionSet>(
    private val instructionSet: I
) {
    fun dispatch(
        opcodeValue: Int,
        chunk: ByteArray,
        offset: Int,
        skip: Int
    ): Instruction {
        val opcode = Opcode.fromInt(opcodeValue)
        if (opcode == Opcode.INVALID) {
            return Instruction.Invalid(opcodeValue)
        }

        val chunkULong = chunkToULong(chunk)

        return when (opcode) {
            Opcode.TRAP -> Instruction.Trap
            Opcode.FALLTHROUGH -> Instruction.Fallthrough

            // args: reg, imm
            Opcode.JUMP_INDIRECT,
            Opcode.LOAD_IMM,
            Opcode.LOAD_U8,
            Opcode.LOAD_I8,
            Opcode.LOAD_U16,
            Opcode.LOAD_I16,
            Opcode.LOAD_U32,
            Opcode.STORE_U8,
            Opcode.STORE_U16,
            Opcode.STORE_U32 -> {
                val (reg, imm) = readArgsRegImm(chunkULong, skip)
                Instruction.RegImmInstruction(opcode, reg, imm)
            }

            // args: reg, imm, offset
            Opcode.LOAD_IMM_AND_JUMP,
            Opcode.BRANCH_EQ_IMM,
            Opcode.BRANCH_NOT_EQ_IMM,
            Opcode.BRANCH_LESS_UNSIGNED_IMM,
            Opcode.BRANCH_LESS_SIGNED_IMM,
            Opcode.BRANCH_GREATER_OR_EQUAL_UNSIGNED_IMM,
            Opcode.BRANCH_GREATER_OR_EQUAL_SIGNED_IMM,
            Opcode.BRANCH_LESS_OR_EQUAL_SIGNED_IMM,
            Opcode.BRANCH_LESS_OR_EQUAL_UNSIGNED_IMM,
            Opcode.BRANCH_GREATER_SIGNED_IMM,
            Opcode.BRANCH_GREATER_UNSIGNED_IMM -> {
                val (reg, imm1, imm2) = readArgsRegImmOffset(chunkULong, offset, skip)
                Instruction.RegImmOffsetInstruction(opcode, reg, imm1, imm2)
            }

            // args: reg, imm, imm
            Opcode.STORE_IMM_INDIRECT_U8,
            Opcode.STORE_IMM_INDIRECT_U16,
            Opcode.STORE_IMM_INDIRECT_U32 -> {
                val (reg, imm1, imm2) = readArgsRegImm2(chunkULong, skip)
                Instruction.RegImmImmInstruction(opcode, reg, imm1, imm2)
            }

            // ... Add cases for other instruction formats

            else -> Instruction.Invalid(opcodeValue)
        }
    }
}

fun <I : InstructionSet> parseInstruction(
    instructionSet: I,
    code: ByteArray,
    bitmask: ByteArray,
    offset: UInt
): Triple<UInt, Instruction, Boolean> {
    val codeLength = code.size
    val visitor = EnumVisitor(instructionSet)

    return if (offset.toInt() + 32 <= codeLength) {
        visitorStepFast(code, bitmask, offset, visitor)
    } else {
        visitorStepSlow(code, bitmask, offset, visitor)
    }
}


class JumpTable(
    private val blob: UByteArray,
    private val entrySize: UInt
) {
    fun isEmpty(): Boolean = size == 0u

    val size: UInt
        get() = if (entrySize == 0u) 0u else (blob.size.toUInt() / entrySize)

    fun getByAddress(address: UInt): UInt? {
        if ((address and (VM_CODE_ADDRESS_ALIGNMENT - 1u)) != 0u || address == 0u) {
            return null
        }
        return getByIndex((address - VM_CODE_ADDRESS_ALIGNMENT) / VM_CODE_ADDRESS_ALIGNMENT)
    }

    fun getByIndex(index: UInt): UInt? {
        if (entrySize == 0u) {
            return null
        }

        val start = (index * entrySize).toLong()
        val end = (start + entrySize.toLong()).coerceAtMost(blob.size.toLong())
        if (start >= blob.size || start < 0 || end > blob.size || end <= start) {
            return null
        }

        return when (entrySize.toInt()) {
            1 -> blob[start.toInt()].toUInt()
            2 -> (blob[start.toInt()].toUInt() or (blob[start.toInt() + 1].toUInt() shl 8))
            3 -> (blob[start.toInt()].toUInt() or
                (blob[start.toInt() + 1].toUInt() shl 8) or
                (blob[start.toInt() + 2].toUInt() shl 16))

            4 -> (blob[start.toInt()].toUInt() or
                (blob[start.toInt() + 1].toUInt() shl 8) or
                (blob[start.toInt() + 2].toUInt() shl 16) or
                (blob[start.toInt() + 3].toUInt() shl 24))

            else -> throw IllegalStateException("Unexpected entry size")
        }
    }

    fun iterator(): Iterator<UInt> = JumpTableIterator(this)
}

class JumpTableIterator(private val jumpTable: JumpTable) : Iterator<UInt> {
    private var index: UInt = 0u

    override fun hasNext(): Boolean = index < jumpTable.size

    override fun next(): UInt {
        if (!hasNext()) throw NoSuchElementException()
        val value = jumpTable.getByIndex(index) ?: throw IllegalStateException("Unexpected null value")
        index++
        return value
    }
}

// Extension function to make JumpTable iterable
operator fun JumpTable.iterator(): Iterator<UInt> = this.iterator()


fun parseBitmaskSlow(bitmask: UByteArray, offset: Int): Pair<Int, Int>? {
    if (bitmask.isEmpty()) {
        return null
    }

    var currentOffset = offset + 1
    var argsLength = 0
    while (currentOffset shr 3 < bitmask.size) {
        val byte = bitmask[currentOffset shr 3].toInt() and 0xFF
        val shift = currentOffset and 7
        val mask = byte shr shift
        val length = if (mask == 0) {
            8 - shift
        } else {
            val trailingZeros = mask.countTrailingZeroBits()
            if (trailingZeros == 0) {
                break
            }
            trailingZeros
        }

        val newArgsLength = argsLength + length
        if (newArgsLength >= BITMASK_MAX) {
            currentOffset += BITMASK_MAX - argsLength
            argsLength = BITMASK_MAX
            break
        }

        argsLength = newArgsLength
        currentOffset += length
    }

    return Pair(currentOffset, argsLength)
}


fun parseBitmaskFast(bitmask: UByteArray, offset: Int): Pair<Int, Int>? {
    var currentOffset = offset + 1

    val startIndex = currentOffset shr 3
    if (startIndex + 4 > bitmask.size) {
        return null
    }

    val shift = currentOffset and 7
    val mask: Int = (bitmask.sliceArray(startIndex until startIndex + 4)
        .foldIndexed(0) { index, acc, byte -> acc or ((byte.toInt() and 0xFF) shl (index * 8)) }
        shr shift) or (1 shl BITMASK_MAX)

    val argsLength = mask.countTrailingZeroBits()
    assert(argsLength <= BITMASK_MAX) { "argsLength should not exceed BITMASK_MAX" }
    currentOffset += argsLength

    return Pair(currentOffset, argsLength)
}


enum class LineProgramOp(val value: Int) {
    FinishProgram(0),
    SetMutationDepth(1),
    SetKindEnter(2),
    SetKindCall(3),
    SetKindLine(4),
    SetNamespace(5),
    SetFunctionName(6),
    SetPath(7),
    SetLine(8),
    SetColumn(9),
    SetStackDepth(10),
    IncrementLine(11),
    AddLine(12),
    SubLine(13),
    FinishInstruction(14),
    FinishMultipleInstructions(15),
    FinishInstructionAndIncrementStackDepth(16),
    FinishMultipleInstructionsAndIncrementStackDepth(17),
    FinishInstructionAndDecrementStackDepth(18),
    FinishMultipleInstructionsAndDecrementStackDepth(19);

    companion object {
        fun fromInt(value: Int): LineProgramOp? {
            return entries.find { it.value == value }
        }
    }
}

class DisplayName(private val prefix: String, private val suffix: String) {
    override fun toString(): String {
        return buildString {
            append(prefix)
            if (prefix.isNotEmpty()) {
                append("::")
            }
            append(suffix)
        }
    }
}


sealed class SourceLocation {
    data class Path(val path: String) : SourceLocation()
    data class PathAndLine(val path: String, val line: UInt) : SourceLocation()
    data class Full(val path: String, val line: UInt, val column: UInt) : SourceLocation()

    fun getPath(): String = when (this) {
        is Path -> this.path
        is PathAndLine -> this.path
        is Full -> this.path
    }

    fun getLine(): UInt? = when (this) {
        is Path -> null
        is PathAndLine -> this.line
        is Full -> this.line
    }

    fun getColumn(): UInt? = when (this) {
        is Full -> this.column
        else -> null
    }

    override fun toString(): String = when (this) {
        is Path -> path
        is PathAndLine -> "$path:$line"
        is Full -> "$path:$line:$column"
    }
}


/**
 * A binary search implementation which can work on chunks of items, and guarantees that it
 * will always return the first item if there are multiple identical consecutive items.
 */
fun binarySearch(
    data: ByteArray,
    chunkSize: Int,
    compare: (ByteArray) -> Int
): Int {
    var size = data.size / chunkSize
    if (size == 0) return -1

    var base = 0
    while (size > 1) {
        val half = size / 2
        val mid = base + half
        val item = data.copyOfRange(mid * chunkSize, (mid + 1) * chunkSize)
        val cmp = compare(item)
        when {
            cmp > 0 -> size -= half
            cmp < 0 -> {
                base = mid
                size -= half
            }

            else -> {
                // Check for the first occurrence
                var index = mid
                while (index > 0) {
                    val prevItem = data.copyOfRange((index - 1) * chunkSize, index * chunkSize)
                    if (compare(prevItem) != 0) break
                    index--
                }
                return index * chunkSize
            }
        }
    }
    val item = data.copyOfRange(base * chunkSize, (base + 1) * chunkSize)
    return if (compare(item) == 0) base * chunkSize else -1
}

sealed class ProgramParseErrorKind {
    data class FailedToReadVarint(val offset: Int) : ProgramParseErrorKind()
    data class FailedToReadStringNonUtf(val offset: Int) : ProgramParseErrorKind()
    data class UnexpectedSection(val offset: Int, val section: UByte) : ProgramParseErrorKind()
    data class UnexpectedEnd(val offset: Int, val expectedCount: Int, val actualCount: Int) : ProgramParseErrorKind()
    data class UnsupportedVersion(val version: UByte) : ProgramParseErrorKind()
    data class Other(val error: String) : ProgramParseErrorKind()
}

data class ProgramParseError(val kind: ProgramParseErrorKind) : Exception() {
    companion object {
        fun failedToReadVarint(offset: Int): ProgramParseError {
            return ProgramParseError(ProgramParseErrorKind.FailedToReadVarint(offset))
        }

        fun unexpectedEndOfFile(offset: Int, expectedCount: Int, actualCount: Int): ProgramParseError {
            return ProgramParseError(
                ProgramParseErrorKind.UnexpectedEnd(
                    offset = offset,
                    expectedCount = expectedCount,
                    actualCount = actualCount
                )
            )
        }

        fun failedToReadStringNonUtf(offset: Int): ProgramParseError {
            return ProgramParseError(ProgramParseErrorKind.FailedToReadStringNonUtf(offset))
        }

        fun unexpectedSection(offset: Int, section: UByte): ProgramParseError {
            return ProgramParseError(ProgramParseErrorKind.UnexpectedSection(offset, section))
        }

        fun unsupportedVersion(version: UByte): ProgramParseError {
            return ProgramParseError(ProgramParseErrorKind.UnsupportedVersion(version))
        }

        fun other(error: String): ProgramParseError {
            return ProgramParseError(ProgramParseErrorKind.Other(error))
        }
    }

    override fun toString(): String {
        return when (val kind = kind) {
            is ProgramParseErrorKind.FailedToReadVarint -> {
                "Failed to parse program blob: failed to parse a varint at offset 0x${kind.offset.toString(16)}"
            }

            is ProgramParseErrorKind.FailedToReadStringNonUtf -> {
                "Failed to parse program blob: failed to parse a string at offset 0x${kind.offset.toString(16)} (not valid UTF-8)"
            }

            is ProgramParseErrorKind.UnexpectedSection -> {
                "Failed to parse program blob: found unexpected section at offset 0x${kind.offset.toString(16)}: 0x${
                    kind.section.toString(16)
                }"
            }

            is ProgramParseErrorKind.UnexpectedEnd -> {
                "Failed to parse program blob: unexpected end of file at offset 0x${kind.offset.toString(16)}: expected to be able to read at least ${kind.expectedCount} bytes, found ${kind.actualCount} bytes"
            }

            is ProgramParseErrorKind.UnsupportedVersion -> {
                "Failed to parse program blob: unsupported version: ${kind.version}"
            }

            is ProgramParseErrorKind.Other -> {
                "Failed to parse program blob: ${kind.error}"
            }
        }
    }
}

enum class Opcode(val value: Int) {
    // Instructions with args: none
    TRAP(0),
    FALLTHROUGH(17),

    // Instructions with args: reg, imm
    JUMP_INDIRECT(19),
    LOAD_IMM(4),
    LOAD_U8(60),
    LOAD_I8(74),
    LOAD_U16(76),
    LOAD_I16(66),
    LOAD_U32(10),
    LOAD_I32(102),
    LOAD_U64(95),
    STORE_U8(71),
    STORE_U16(69),
    STORE_U32(22),
    STORE_U64(96),

    // Instructions with args: reg, imm, offset
    LOAD_IMM_AND_JUMP(6),
    BRANCH_EQ_IMM(7),
    BRANCH_NOT_EQ_IMM(15),
    BRANCH_LESS_UNSIGNED_IMM(44),
    BRANCH_LESS_SIGNED_IMM(32),
    BRANCH_GREATER_OR_EQUAL_UNSIGNED_IMM(52),
    BRANCH_GREATER_OR_EQUAL_SIGNED_IMM(45),
    BRANCH_LESS_OR_EQUAL_SIGNED_IMM(46),
    BRANCH_LESS_OR_EQUAL_UNSIGNED_IMM(59),
    BRANCH_GREATER_SIGNED_IMM(53),
    BRANCH_GREATER_UNSIGNED_IMM(50),

    // Instructions with args: reg, imm, imm
    STORE_IMM_INDIRECT_U8(26),
    STORE_IMM_INDIRECT_U16(54),
    STORE_IMM_INDIRECT_U32(13),
    STORE_IMM_INDIRECT_U64(93),

    // Instructions with args: reg, reg, imm
    STORE_INDIRECT_U8(16),
    STORE_INDIRECT_U16(29),
    STORE_INDIRECT_U32(3),
    STORE_INDIRECT_U64(90),
    LOAD_INDIRECT_U8(11),
    LOAD_INDIRECT_I8(21),
    LOAD_INDIRECT_U16(37),
    LOAD_INDIRECT_I16(33),
    LOAD_INDIRECT_I32(99),
    LOAD_INDIRECT_U32(1),
    LOAD_INDIRECT_U64(91),
    ADD_IMM(2),
    ADD_64_IMM(104),
    AND_IMM(18),
    AND_64_IMM(118),
    XOR_IMM(31),
    XOR_64_IMM(119),
    OR_IMM(49),
    OR_64_IMM(120),
    MUL_IMM(35),
    MUL_64_IMM(121),
    MUL_UPPER_SIGNED_SIGNED_IMM(65),
    MUL_UPPER_SIGNED_SIGNED_IMM_64(131),
    MUL_UPPER_UNSIGNED_UNSIGNED_IMM(63),
    MUL_UPPER_UNSIGNED_UNSIGNED_IMM_64(132),
    SET_LESS_THAN_UNSIGNED_IMM(27),
    SET_LESS_THAN_UNSIGNED_64_IMM(125),
    SET_LESS_THAN_SIGNED_IMM(56),
    SET_LESS_THAN_SIGNED_64_IMM(126),
    SHIFT_LOGICAL_LEFT_IMM(9),
    SHIFT_LOGICAL_LEFT_64_IMM(105),
    SHIFT_LOGICAL_RIGHT_IMM(14),
    SHIFT_LOGICAL_RIGHT_64_IMM(106),
    SHIFT_ARITHMETIC_RIGHT_IMM(25),
    SHIFT_ARITHMETIC_RIGHT_64_IMM(107),
    NEGATE_AND_ADD_IMM(40),
    SET_GREATER_THAN_UNSIGNED_IMM(39),
    SET_GREATER_THAN_UNSIGNED_64_IMM(129),
    SET_GREATER_THAN_SIGNED_IMM(61),
    SET_GREATER_THAN_SIGNED_64_IMM(130),
    SHIFT_LOGICAL_RIGHT_IMM_ALT(72),
    SHIFT_LOGICAL_RIGHT_64_IMM_ALT(103),
    SHIFT_ARITHMETIC_RIGHT_IMM_ALT(80),
    SHIFT_ARITHMETIC_RIGHT_64_IMM_ALT(111),
    SHIFT_LOGICAL_LEFT_IMM_ALT(75),
    SHIFT_LOGICAL_LEFT_64_IMM_ALT(110),

    CMOV_IF_ZERO_IMM(85),
    CMOV_IF_NOT_ZERO_IMM(86),

    // Instructions with args: reg, reg, offset
    BRANCH_EQ(24),
    BRANCH_NOT_EQ(30),
    BRANCH_LESS_UNSIGNED(47),
    BRANCH_LESS_SIGNED(48),
    BRANCH_GREATER_OR_EQUAL_UNSIGNED(41),
    BRANCH_GREATER_OR_EQUAL_SIGNED(43),

    // Instructions with args: reg, reg, reg
    ADD(8),
    ADD_64(101),
    SUB(20),
    SUB_64(112),
    AND(23),
    AND_64(124),
    XOR(28),
    XOR_64(122),
    OR(12),
    OR_64(123),
    MUL(34),
    MUL_64(113),
    MUL_UPPER_SIGNED_SIGNED(67),
    MUL_UPPER_SIGNED_SIGNED_64(133),
    MUL_UPPER_UNSIGNED_UNSIGNED(57),
    MUL_UPPER_UNSIGNED_UNSIGNED_64(134),
    MUL_UPPER_SIGNED_UNSIGNED(81),
    MUL_UPPER_SIGNED_UNSIGNED_64(135),
    SET_LESS_THAN_UNSIGNED(36),
    SET_LESS_THAN_UNSIGNED_64(127),
    SET_LESS_THAN_SIGNED(58),
    SET_LESS_THAN_SIGNED_64(128),
    SHIFT_LOGICAL_LEFT(55),
    SHIFT_LOGICAL_LEFT_64(100),
    SHIFT_LOGICAL_RIGHT(51),
    SHIFT_LOGICAL_RIGHT_64(108),
    SHIFT_ARITHMETIC_RIGHT(77),
    SHIFT_ARITHMETIC_RIGHT_64(109),
    DIV_UNSIGNED(68),
    DIV_UNSIGNED_64(114),
    DIV_SIGNED(64),
    DIV_SIGNED_64(115),
    REM_UNSIGNED(73),
    REM_UNSIGNED_64(116),
    REM_SIGNED(70),
    REM_SIGNED_64(117),

    CMOV_IF_ZERO(83),
    CMOV_IF_NOT_ZERO(84),

    // Instructions with args: offset
    JUMP(5),

    // Instructions with args: imm
    ECALLI(78),

    // Instructions with args: imm, imm
    STORE_IMM_U8(62),
    STORE_IMM_U16(79),
    STORE_IMM_U32(38),
    STORE_IMM_U64(98),

    // Instructions with args: reg, reg
    MOVE_REG(82),
    SBRK(87),

    // Instructions with args: reg, reg, imm, imm
    LOAD_IMM_AND_JUMP_INDIRECT(42),

    // Invalid opcode
    INVALID(88);

    companion object {
        private val map = values().associateBy(Opcode::value)

        fun fromInt(type: Int) = map[type] ?: INVALID
    }
}

interface InstructionVisitor<T> {
    // Instructions with args: none
    fun visitTrap(): T
    fun visitFallthrough(): T

    // Instructions with args: reg, imm
    fun visitJumpIndirect(reg: Int, imm: Int): T
    fun visitLoadImm(reg: Int, imm: Int): T
    fun visitLoadU8(reg: Int, imm: Int): T
    fun visitLoadI8(reg: Int, imm: Int): T
    fun visitLoadU16(reg: Int, imm: Int): T
    fun visitLoadI16(reg: Int, imm: Int): T
    fun visitLoadU32(reg: Int, imm: Int): T
    fun visitLoadI32(reg: Int, imm: Int): T
    fun visitLoadU64(reg: Int, imm: Int): T
    fun visitStoreU8(reg: Int, imm: Int): T
    fun visitStoreU16(reg: Int, imm: Int): T
    fun visitStoreU32(reg: Int, imm: Int): T
    fun visitStoreU64(reg: Int, imm: Int): T

    // Instructions with args: reg, imm, offset
    fun visitLoadImmAndJump(reg: Int, imm: Int, offset: Int): T
    fun visitBranchEqImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchNotEqImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchLessUnsignedImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchLessSignedImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchGreaterOrEqualUnsignedImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchGreaterOrEqualSignedImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchLessOrEqualSignedImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchLessOrEqualUnsignedImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchGreaterSignedImm(reg: Int, imm: Int, offset: Int): T
    fun visitBranchGreaterUnsignedImm(reg: Int, imm: Int, offset: Int): T

    // Instructions with args: reg, reg
    fun visitMoveReg(dest: Int, src: Int): T
    fun visitSbrk(dest: Int, src: Int): T

    // Instructions with args: imm
    fun visitEcalli(imm: Int): T

    // Instructions with args: offset
    fun visitJump(offset: Int): T

    // Instructions with args: reg, reg, reg
    fun visitAdd(dest: Int, src1: Int, src2: Int): T
    fun visitSub(dest: Int, src1: Int, src2: Int): T
    fun visitAnd(dest: Int, src1: Int, src2: Int): T
    fun visitOr(dest: Int, src1: Int, src2: Int): T
    fun visitXor(dest: Int, src1: Int, src2: Int): T
    fun visitMul(dest: Int, src1: Int, src2: Int): T

    // ... Continue adding methods for all other instructions

    // Invalid instruction
    fun visitInvalid(): T
}

interface ParsingVisitor<T> {
    fun visitArgless(offset: Int, argsLength: Int): T
    fun visitRegImm(offset: Int, argsLength: Int, reg: Int, imm: Int): T
    fun visitRegImmOffset(offset: Int, argsLength: Int, reg: Int, imm1: Int, imm2: Int): T
    fun visitRegImmImm(offset: Int, argsLength: Int, reg: Int, imm1: Int, imm2: Int): T
    fun visitRegRegImm(offset: Int, argsLength: Int, reg1: Int, reg2: Int, imm: Int): T
    fun visitRegRegOffset(offset: Int, argsLength: Int, reg1: Int, reg2: Int, imm: Int): T
    fun visitRegRegReg(offset: Int, argsLength: Int, reg1: Int, reg2: Int, reg3: Int): T
    fun visitOffset(offset: Int, argsLength: Int, imm: Int): T
    fun visitImm(offset: Int, argsLength: Int, imm: Int): T
    fun visitImmImm(offset: Int, argsLength: Int, imm1: Int, imm2: Int): T
    fun visitRegReg(offset: Int, argsLength: Int, reg1: Int, reg2: Int): T
    fun visitRegRegImmImm(offset: Int, argsLength: Int, reg1: Int, reg2: Int, imm1: Int, imm2: Int): T

    fun visitInvalid(offset: Int, argsLength: Int): T
}

data class ParsedInstruction(
    val kind: Instruction,
    val offset: ProgramCounter,
    val nextOffset: ProgramCounter
) {
    override fun toString(): String {
        return String.format("%7d: %s", offset, kind)
    }

    fun <T> visit(visitor: InstructionVisitor<T>): T {
        return kind.visit(visitor)
    }
}

sealed class Instruction {
    // Instructions with args: none
    object Trap : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitTrap()
        override fun opcode(): Opcode = Opcode.TRAP
    }

    object Fallthrough : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitFallthrough()
        override fun opcode(): Opcode = Opcode.FALLTHROUGH
    }

    // Instructions with args: reg, imm
    data class JumpIndirect(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitJumpIndirect(reg, imm)
        override fun opcode(): Opcode = Opcode.JUMP_INDIRECT
    }

    data class LoadImm(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadImm(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_IMM
    }

    data class LoadU8(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadU8(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_U8
    }

    data class LoadI8(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadI8(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_I8
    }

    data class LoadU16(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadU16(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_U16
    }

    data class LoadI16(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadI16(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_I16
    }

    data class LoadU32(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadU32(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_U32
    }

    data class LoadI32(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadI32(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_I32
    }

    data class LoadU64(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadU64(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_U64
    }

    data class StoreU8(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitStoreU8(reg, imm)
        override fun opcode(): Opcode = Opcode.STORE_U8
    }

    data class StoreU16(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitStoreU16(reg, imm)
        override fun opcode(): Opcode = Opcode.STORE_U16
    }

    data class StoreU32(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitStoreU32(reg, imm)
        override fun opcode(): Opcode = Opcode.STORE_U32
    }

    data class StoreU64(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitStoreU64(reg, imm)
        override fun opcode(): Opcode = Opcode.STORE_U64
    }

    // Instructions with args: reg, imm, offset
    data class LoadImmAndJump(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitLoadImmAndJump(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.LOAD_IMM_AND_JUMP
    }

    data class BranchEqImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitBranchEqImm(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_EQ_IMM
    }

    data class BranchNotEqImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitBranchNotEqImm(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_NOT_EQ_IMM
    }

    data class BranchLessUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitBranchLessUnsignedImm(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_UNSIGNED_IMM
    }

    data class BranchLessSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitBranchLessSignedImm(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_SIGNED_IMM
    }

    data class BranchGreaterOrEqualUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T =
            visitor.visitBranchGreaterOrEqualUnsignedImm(reg, imm, offset)

        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_OR_EQUAL_UNSIGNED_IMM
    }

    data class BranchGreaterOrEqualSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T =
            visitor.visitBranchGreaterOrEqualSignedImm(reg, imm, offset)

        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_OR_EQUAL_SIGNED_IMM
    }

    data class BranchLessOrEqualSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T =
            visitor.visitBranchLessOrEqualSignedImm(reg, imm, offset)

        override fun opcode(): Opcode = Opcode.BRANCH_LESS_OR_EQUAL_SIGNED_IMM
    }

    data class BranchLessOrEqualUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T =
            visitor.visitBranchLessOrEqualUnsignedImm(reg, imm, offset)

        override fun opcode(): Opcode = Opcode.BRANCH_LESS_OR_EQUAL_UNSIGNED_IMM
    }

    data class BranchGreaterSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T =
            visitor.visitBranchGreaterSignedImm(reg, imm, offset)

        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_SIGNED_IMM
    }

    data class BranchGreaterUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T =
            visitor.visitBranchGreaterUnsignedImm(reg, imm, offset)

        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_UNSIGNED_IMM
    }

    // Instructions with args: reg, reg
    data class MoveReg(val dest: Int, val src: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitMoveReg(dest, src)
        override fun opcode(): Opcode = Opcode.MOVE_REG
    }

    data class Sbrk(val dest: Int, val src: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitSbrk(dest, src)
        override fun opcode(): Opcode = Opcode.SBRK
    }

    // Instructions with args: imm
    data class Ecalli(val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitEcalli(imm)
        override fun opcode(): Opcode = Opcode.ECALLI
    }

    // Instructions with args: offset
    data class Jump(val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitJump(offset)
        override fun opcode(): Opcode = Opcode.JUMP
    }

    // Instructions with args: reg, reg, reg
    data class Add(val dest: Int, val src1: Int, val src2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitAdd(dest, src1, src2)
        override fun opcode(): Opcode = Opcode.ADD
    }

    data class Sub(val dest: Int, val src1: Int, val src2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitSub(dest, src1, src2)
        override fun opcode(): Opcode = Opcode.SUB
    }

    data class And(val dest: Int, val src1: Int, val src2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitAnd(dest, src1, src2)
        override fun opcode(): Opcode = Opcode.AND
    }

    data class Or(val dest: Int, val src1: Int, val src2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitOr(dest, src1, src2)
        override fun opcode(): Opcode = Opcode.OR
    }

    data class Xor(val dest: Int, val src1: Int, val src2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitXor(dest, src1, src2)
        override fun opcode(): Opcode = Opcode.XOR
    }

    data class Mul(val dest: Int, val src1: Int, val src2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitMul(dest, src1, src2)
        override fun opcode(): Opcode = Opcode.MUL
    }

    // ... Continue adding other instructions with their respective visit methods

    // Invalid instruction
    data class Invalid(val value: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitInvalid()
        override fun opcode(): Opcode = Opcode.INVALID
    }

    abstract fun <T> visit(visitor: InstructionVisitor<T>): T
    abstract fun opcode(): Opcode
}

class ProgramParts(
    var is64Bit: Boolean = false,
    var roDataSize: UInt = 0u,
    var rwDataSize: UInt = 0u,
    var stackSize: UInt = 0u,

    var roData: ByteArray = ByteArray(0),
    var rwData: ByteArray = ByteArray(0),
    var codeAndJumpTable: ByteArray = ByteArray(0),
    var importOffsets: ByteArray = ByteArray(0),
    var importSymbols: ByteArray = ByteArray(0),
    var exports: ByteArray = ByteArray(0),

    var debugStrings: ByteArray = ByteArray(0),
    var debugLineProgramRanges: ByteArray = ByteArray(0),
    var debugLinePrograms: ByteArray = ByteArray(0)
) {
    companion object {
        @Throws(ProgramParseError::class)
        fun fromBytes(blob: ByteArray): ProgramParts {
            if (!blob.startsWith(BLOB_MAGIC)) {
                throw ProgramParseError.other("blob doesn't start with the expected magic bytes")
            }

            val reader = Reader(blob, BLOB_MAGIC.size)

            val is64Bit = when (val blobVersion = reader.readByte()) {
                BLOB_VERSION_V1_32 -> false
                BLOB_VERSION_V1_64 -> true
                else -> throw ProgramParseError.unsupportedVersion(blobVersion)
            }

            val blobLenBytes = reader.readBytes(BLOB_LEN_SIZE)
            val blobLen = blobLenBytes.toULongLE()
            if (blobLen != blob.size.toULong()) {
                throw ProgramParseError.other("blob size doesn't match the blob length metadata")
            }

            val parts = ProgramParts(is64Bit = is64Bit)

            var section = reader.readByte()
            if (section == SECTION_MEMORY_CONFIG) {
                val sectionLength = reader.readVarInt()
                val position = reader.position
                parts.roDataSize = reader.readVarInt()
                parts.rwDataSize = reader.readVarInt()
                parts.stackSize = reader.readVarInt()
                if (position + sectionLength.toInt() != reader.position) {
                    throw ProgramParseError.other("the memory config section contains more data than expected")
                }
                section = reader.readByte()
            }

            parts.roData = reader.readSectionAsBytes(::section, SECTION_RO_DATA)
            parts.rwData = reader.readSectionAsBytes(::section, SECTION_RW_DATA)

            if (section == SECTION_IMPORTS) {
                val sectionLength = reader.readVarInt()
                val sectionStart = reader.position
                val importCount = reader.readVarInt()
                if (importCount > VM_MAXIMUM_IMPORT_COUNT) {
                    throw ProgramParseError.other("too many imports")
                }

                val importOffsetsSize = importCount.toInt().times(4)
                if (importOffsetsSize < 0) {
                    throw ProgramParseError.other("the imports section is invalid")
                }

                parts.importOffsets = reader.readBytes(importOffsetsSize)
                val importSymbolsSize = sectionLength.toInt() - (reader.position - sectionStart)
                if (importSymbolsSize < 0) {
                    throw ProgramParseError.other("the imports section is invalid")
                }

                parts.importSymbols = reader.readBytes(importSymbolsSize)
                section = reader.readByte()
            }

            parts.exports = reader.readSectionAsBytes(::section, SECTION_EXPORTS)
            parts.codeAndJumpTable = reader.readSectionAsBytes(::section, SECTION_CODE_AND_JUMP_TABLE)
            parts.debugStrings = reader.readSectionAsBytes(::section, SECTION_OPT_DEBUG_STRINGS)
            parts.debugLinePrograms = reader.readSectionAsBytes(::section, SECTION_OPT_DEBUG_LINE_PROGRAMS)
            parts.debugLineProgramRanges = reader.readSectionAsBytes(::section, SECTION_OPT_DEBUG_LINE_PROGRAM_RANGES)

            while ((section.toInt() and 0b10000000) != 0) {
                // We don't know this section, but it's optional, so just skip it.
                val sectionLength = reader.readVarInt()
                reader.skip(sectionLength.toInt())
                section = reader.readByte()
            }

            if (section != SECTION_END_OF_FILE) {
                throw ProgramParseError.unexpectedSection(reader.position - 1, section)
            }

            return parts
        }
    }
}

fun getBitForOffset(bitmask: ByteArray, codeLength: Int, offset: UInt): Boolean {
    if (offset.toInt() >= codeLength) {
        return false
    }
    val index = (offset.toInt() shr 3)
    val shift = (offset.toInt() and 7)
    val byte = bitmask.getOrNull(index) ?: return false
    return ((byte.toInt() shr shift) and 1) == 1
}

fun findNextOffsetUnbounded(bitmask: ByteArray, codeLength: UInt, offset: UInt): UInt {
    var currentOffset = offset
    while (true) {
        val index = (currentOffset.toInt() shr 3)
        if (index >= bitmask.size) {
            break
        }
        val byte = bitmask[index].toInt() and 0xFF
        val shift = currentOffset.toInt() and 7
        val mask = byte shr shift
        if (mask == 0) {
            currentOffset += (8 - shift).toUInt()
        } else {
            currentOffset += Integer.numberOfTrailingZeros(mask)
            break
        }
    }
    return minOf(codeLength, currentOffset)
}

fun parseBitmaskSlow(bitmask: ByteArray, codeLength: Int, offset: UInt): Pair<UInt, Boolean> {
    var skip = 0u
    var currentOffset = offset
    while (currentOffset.toInt() + skip.toInt() + 1 < codeLength) {
        val nextOffset = currentOffset + skip + 1u
        if (!getBitForOffset(bitmask, codeLength, nextOffset)) {
            skip += 1u
        } else {
            break
        }
    }
    val isNextInstructionInvalid = !getBitForOffset(bitmask, codeLength, currentOffset + skip + 1u)
    return Pair(skip, isNextInstructionInvalid)
}

fun parseBitmaskFast(bitmask: ByteArray, offset: UInt): UInt? {
    // For fast parsing, we can read 3 bytes from the bitmask starting at (offset >> 3)
    val index = offset.toInt() shr 3
    if (index + 2 >= bitmask.size) {
        return null
    }

    val b0 = bitmask[index].toInt() and 0xFF
    val b1 = bitmask[index + 1].toInt() and 0xFF
    val b2 = bitmask[index + 2].toInt() and 0xFF

    val shift = offset.toInt() and 7
    val bits = ((b2 shl 16) or (b1 shl 8) or b0) ushr shift
    val mask = bits and 0xFFFFFF

    val skip = Integer.numberOfTrailingZeros(mask).toUInt()
    return skip
}

fun <I : InstructionSet> visitorStepSlow(
    code: ByteArray,
    bitmask: ByteArray,
    offset: UInt,
    visitor: EnumVisitor<I>
): Triple<UInt, Instruction, Boolean> {
    if (offset.toInt() >= code.size) {
        // No more code to read, return invalid instruction
        return Triple(offset + 1u, Instruction.Invalid, true)
    }

    val codeLength = code.size
    require(getBitForOffset(bitmask, codeLength, offset)) { "bit at $offset is zero" }

    val (skip, isNextInstructionInvalid) = parseBitmaskSlow(bitmask, codeLength, offset)

    val chunkSize = minOf(17, codeLength - offset.toInt())
    val chunk = code.copyOfRange(offset.toInt(), offset.toInt() + chunkSize)

    val opcode = chunk[0].toInt() and 0xFF

    // Prepare the arguments chunk (t)
    val t = ByteArray(16) { 0 }
    val dataLength = chunk.size - 1
    if (dataLength > 0) {
        System.arraycopy(chunk, 1, t, 0, dataLength)
    }

    val instruction = visitor.dispatch(opcode, t, offset, skip)

    var isNextInvalid = isNextInstructionInvalid
    val nextOffset = offset + skip + 1u

    if (isNextInvalid && nextOffset.toInt() >= codeLength) {
        // This is the last instruction
        if (!instruction.canFallthrough()) {
            isNextInvalid = false
        }
    }

    return Triple(nextOffset, instruction, isNextInvalid)
}

fun <I : InstructionSet> visitorStepFast(
    code: ByteArray,
    bitmask: ByteArray,
    offset: UInt,
    visitor: EnumVisitor<I>
): Triple<UInt, Instruction, Boolean> {
    val codeLength = code.size
    require(getBitForOffset(bitmask, codeLength, offset)) { "bit at $offset is zero" }
    require(offset.toInt() + 32 <= codeLength) { "Not enough code for fast path" }

    val chunk = code.copyOfRange(offset.toInt(), offset.toInt() + 32)
    val opcode = chunk[0].toInt() and 0xFF

    val skip = parseBitmaskFast(bitmask, offset) ?: throw IllegalStateException("Invalid bitmask at offset $offset")

    val t = ByteArray(16) { 0 }
    System.arraycopy(chunk, 1, t, 0, 16)

    val instruction = visitor.dispatch(opcode, t, offset, skip)

    val nextOffset = offset + skip + 1u
    val isNextInstructionInvalid = skip == 24u && !getBitForOffset(bitmask, codeLength, nextOffset)

    return Triple(nextOffset, instruction, isNextInstructionInvalid)
}

interface InstructionSet {
    fun opcodeFromByte(byte: UByte): Opcode?
    fun createInstruction(opcode: Opcode): Instruction
}

class Instructions<I : InstructionSet>(
    private val instructionSet: I,
    private val code: ByteArray,
    private val bitmask: ByteArray,
    private var offset: UInt,
    private var invalidOffset: UInt?,
    private val isBounded: Boolean,
    private var isDone: Boolean
) : Iterator<ParsedInstruction> {

    companion object {
        fun <I : InstructionSet> newBounded(
            instructionSet: I,
            code: ByteArray,
            bitmask: ByteArray,
            offset: UInt
        ): Instructions<I> {
            return Instructions(instructionSet, code, bitmask, offset, null, true, false).apply {
                initialize()
            }
        }

        fun <I : InstructionSet> newUnbounded(
            instructionSet: I,
            code: ByteArray,
            bitmask: ByteArray,
            offset: UInt
        ): Instructions<I> {
            return Instructions(instructionSet, code, bitmask, offset, null, false, false).apply {
                initialize()
            }
        }
    }

    private fun initialize() {
        require(code.size <= UInt.MAX_VALUE.toLong())
        require(bitmask.size == (code.size + 7) / 8)

        val isValid = getBitForOffset(bitmask, code.size, offset)
        if (isValid) {
            invalidOffset = null
        } else if (isBounded) {
            isDone = true
            offset = minOf(offset + 1u, code.size.toUInt())
            invalidOffset = offset - 1u
        } else {
            val nextOffset = findNextOffsetUnbounded(bitmask, code.size.toUInt(), offset)
            invalidOffset = offset
            offset = nextOffset
        }
    }

    fun offset(): UInt {
        return invalidOffset ?: offset
    }

    fun <T> visit(visitor: InstructionVisitor<T>): T? {
        return if (hasNext()) {
            val nextInstruction = next()
            nextInstruction.visit(visitor)
        } else {
            null
        }
    }

    override fun hasNext(): Boolean {
        return !isDone && (invalidOffset != null || offset.toInt() < code.size)
    }

    override fun next(): ParsedInstruction {
        invalidOffset?.let { invalidOffset ->
            this.invalidOffset = null
            return ParsedInstruction(
                kind = Instruction.Invalid,
                offset = ProgramCounter(invalidOffset),
                nextOffset = ProgramCounter(offset),
            )
        }

        if (isDone || offset.toInt() >= code.size) {
            throw NoSuchElementException()
        }

        val currentOffset = offset
        val isValid = getBitForOffset(bitmask, code.size, offset)
        require(isValid) { "bit at $offset is zero" }

        val (nextOffset, instruction, isNextInstructionInvalid) =
            parseInstruction(instructionSet, code, bitmask, offset)
        require(nextOffset > offset)

        if (!isNextInstructionInvalid) {
            offset = nextOffset
            if (offset.toInt() != code.size) {
                require(getBitForOffset(bitmask, code.size, offset)) { "bit at $offset is zero" }
            }
        } else {
            if (nextOffset.toInt() == code.size) {
                offset = code.size.toUInt() + 1u
            } else if (isBounded) {
                isDone = true
                offset = if (instruction.canFallthrough()) {
                    code.size.toUInt()
                } else {
                    nextOffset
                }
            } else {
                offset = findNextOffsetUnbounded(bitmask, code.size.toUInt(), nextOffset)
                if (offset.toInt() != code.size) {
                    require(getBitForOffset(bitmask, code.size, offset)) { "bit at $offset is zero" }
                }
            }

            if (instruction.canFallthrough()) {
                invalidOffset = nextOffset
            }
        }

        return ParsedInstruction(
            kind = instruction,
            offset = ProgramCounter(currentOffset),
            nextOffset = ProgramCounter(nextOffset)
        )
    }
}

class ProgramBlob private constructor(
    val roDataSize: Int,
    val rwDataSize: Int,
    val stackSize: Int,

    val roData: UByteArray,
    val rwData: UByteArray,
    val exports: UByteArray,
    val importSymbols: UByteArray,
    val importOffsets: UByteArray,
    val code: UByteArray,
    val jumpTable: UByteArray,
    val jumpTableEntrySize: UByte,
    val bitmask: UByteArray,

    val debugStrings: UByteArray,
    val debugLineProgramRanges: UByteArray,
    val debugLinePrograms: UByteArray
) {

    companion object {
        fun parse(bytes: UByteArray): ProgramBlob {
            val parts = ProgramParts.fromBytes(bytes)
            return fromParts(parts)
        }

        fun fromParts(parts: ProgramParts): ProgramBlob {
            val blob = ProgramBlob(
                roDataSize = parts.roDataSize,
                rwDataSize = parts.rwDataSize,
                stackSize = parts.stackSize,

                roData = parts.roData,
                rwData = parts.rwData,
                exports = parts.exports,
                importSymbols = parts.importSymbols,
                importOffsets = parts.importOffsets,
                code = parts.codeAndJumpTable,
                jumpTable = UByteArray(0), // Assuming this is populated somewhere in real usage
                jumpTableEntrySize = 0u,   // Assuming this is set somewhere in real usage
                bitmask = UByteArray(0),   // Assuming this is set somewhere in real usage

                debugStrings = parts.debugStrings,
                debugLineProgramRanges = parts.debugLineProgramRanges,
                debugLinePrograms = parts.debugLinePrograms
            )

            // Additional logic here similar to your Rust code...

            return blob
        }
    }

    fun roData(): UByteArray = roData
    fun roDataSize(): Int = roDataSize
    fun rwData(): UByteArray = rwData
    fun rwDataSize(): Int = rwDataSize
    fun stackSize(): Int = stackSize
    fun code(): UByteArray = code
    fun bitmask(): UByteArray = bitmask

    fun imports(): Imports {
        return Imports(importOffsets, importSymbols)
    }

    fun exports(): Iterator<ProgramExport<UByteArray>> {
        return object : Iterator<ProgramExport<UByteArray>> {
            private var state: State = State.Uninitialized
            private val reader = Reader(exports, 0)

            override fun hasNext(): Boolean {
                return when (state) {
                    State.Uninitialized -> reader.readVarInt() != null
                    State.Pending -> true
                    State.Finished -> false
                }
            }

            override fun next(): ProgramExport<UByteArray>? {
                val remaining = when (val currentState = state) {
                    State.Uninitialized -> reader.readVarInt()
                    State.Pending -> currentState.remaining
                    State.Finished -> return null
                }

                if (remaining == 0) return null

                val targetCodeOffset = reader.readVarInt()
                val symbol = reader.readBytesWithLength()
                state = State.Pending(remaining - 1)

                return ProgramExport(targetCodeOffset, ProgramSymbol(symbol))
            }
        }
    }

    fun instructions(): Instructions {
        return Instructions.new(code, bitmask, 0)
    }

    fun instructionsAt(offset: Int): Instructions? {
        return if (bitmask[offset shr 3].toInt() and (1 shl (offset and 7)) == 0) {
            null
        } else {
            Instructions.new(code, bitmask, offset)
        }
    }

    fun jumpTable(): JumpTable {
        return JumpTable(jumpTable, jumpTableEntrySize.toInt())
    }

    fun getDebugString(offset: Int): String {
        val reader = Reader(debugStrings, offset)
        return reader.readStringWithLength()
    }

    fun getDebugLineProgramAt(nthInstruction: Int): LineProgram? {
        if (debugLineProgramRanges.isEmpty() || debugLinePrograms.isEmpty()) {
            return null
        }

        if (debugLinePrograms[0] != VERSION_DEBUG_LINE_PROGRAM_V1) {
            throw ProgramParseError(ProgramParseErrorKind.Other("Unsupported debug line program version"))
        }

        val entrySize = 12
        if (debugLineProgramRanges.size % entrySize != 0) {
            throw ProgramParseError(ProgramParseErrorKind.Other("Invalid size for debug function ranges section"))
        }

        val offset = binarySearch(debugLineProgramRanges, entrySize) { xs ->
            val begin = xs.copyOfRange(0, 4).toInt()
            when {
                nthInstruction < begin -> Ordering.Greater
                nthInstruction >= xs.copyOfRange(4, 8).toInt() -> Ordering.Less
                else -> Ordering.Equal
            }
        }

        val infoOffset = debugLineProgramRanges.copyOfRange(offset + 8, offset + 12).toInt()
        val reader = Reader(debugLinePrograms, infoOffset)
        return LineProgram(
            entryIndex = offset / entrySize,
            regionCounter = 0,
            blob = this,
            reader = reader,
            isFinished = false,
            programCounter = debugLineProgramRanges.copyOfRange(offset, offset + 4).toInt(),
            stack = Array(16) { LineProgramFrame() },
            stackDepth = 0,
            mutationDepth = 0
        )
    }

    @Throws(ProgramParseError::class)
    fun getDebugLineProgramAt(programCounter: ProgramCounter): LineProgram? {
        val pc = programCounter.value
        if (debugLineProgramRanges.isEmpty() || debugLinePrograms.isEmpty()) {
            return null
        }

        if (debugLinePrograms[0] != VERSION_DEBUG_LINE_PROGRAM_V1) {
            throw ProgramParseError("The debug line programs section has an unsupported version")
        }

        val ENTRY_SIZE = 12
        val slice = debugLineProgramRanges
        if (slice.size % ENTRY_SIZE != 0) {
            throw ProgramParseError("The debug function ranges section has an invalid size")
        }

        val offset = binarySearch(slice, ENTRY_SIZE) { xs ->
            val begin = xs.readUInt32LE(0)
            if (pc < begin) {
                1
            } else {
                val end = xs.readUInt32LE(4)
                if (pc >= end) {
                    -1
                } else {
                    0
                }
            }
        }

        if (offset < 0) {
            return null
        }

        val xs = slice.copyOfRange(offset, offset + ENTRY_SIZE)
        val indexBegin = xs.readUInt32LE(0)
        val indexEnd = xs.readUInt32LE(4)
        val infoOffset = xs.readUInt32LE(8)

        if (pc < indexBegin || pc >= indexEnd) {
            throw ProgramParseError("Binary search for function debug info failed")
        }

        val reader = ByteArrayReader(debugLinePrograms)
        reader.position = infoOffset.toInt()

        return LineProgram(
            entryIndex = offset / ENTRY_SIZE,
            regionCounter = 0,
            blob = this,
            reader = reader,
            isFinished = false,
            programCounter = indexBegin,
            stack = Array(16) { LineProgramFrame() },
            stackDepth = 0,
            mutationDepth = 0
        )
    }
}

data class Imports(
    val offsets: UByteArray,
    val symbols: UByteArray
)

data class ProgramCounter(val value: UInt) {
    override fun toString(): String = value.toString()
}

class ProgramExport<T : AsBytes>(
    private val programCounter: ProgramCounter,
    private val symbol: ProgramSymbol<T>
) {
    companion object {
        fun <T : AsBytes> new(programCounter: ProgramCounter, symbol: ProgramSymbol<T>): ProgramExport<T> {
            return ProgramExport(programCounter, symbol)
        }
    }

    fun getProgramCounter(): ProgramCounter = programCounter

    fun getSymbol(): ProgramSymbol<T> = symbol

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is String -> symbol.asBytes().contentEquals(other.toByteArray(Charsets.UTF_8))
            is ProgramExport<*> -> programCounter == other.programCounter && symbol == other.symbol
            else -> false
        }
    }

    override fun hashCode(): Int {
        return 31 * programCounter.hashCode() + symbol.hashCode()
    }

    override fun toString(): String {
        return "ProgramExport(programCounter=$programCounter, symbol=$symbol)"
    }
}

interface AsBytes {
    fun asBytes(): ByteArray
}

class ProgramSymbol<T : AsBytes>(private val value: T) : AsBytes {
    fun intoInner(): T = value

    override fun asBytes(): ByteArray = value.asBytes()

    override fun equals(other: Any?): Boolean {
        val bytes = asBytes()
        return when (other) {
            is String -> bytes.contentEquals(other.toByteArray(Charsets.UTF_8))
            is ProgramSymbol<*> -> bytes.contentEquals(other.asBytes())
            else -> false
        }
    }

    override fun hashCode(): Int = asBytes().contentHashCode()

    override fun toString(): String {
        val bytes = asBytes()
        return if (bytes.isValidUtf8()) {
            val ident = bytes.toString(Charsets.UTF_8)
            "'$ident'"
        } else {
            "0x" + bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

fun ByteArray.isValidUtf8(): Boolean {
    val decoder: CharsetDecoder = Charsets.UTF_8.newDecoder()
    decoder.onMalformedInput(CodingErrorAction.REPORT)
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
    return try {
        decoder.decode(ByteBuffer.wrap(this))
        true
    } catch (e: Exception) {
        false
    }
}

class Reader(val blob: ByteArray, var position: Int = 0) {
    @Throws(ProgramParseError::class)
    fun readByte(): UByte {
        if (position >= blob.size) {
            throw ProgramParseError.unexpectedEndOfFile(position, 1, 0)
        }
        return blob[position++].toUByte()
    }

    @Throws(ProgramParseError::class)
    fun readBytes(length: Int): ByteArray {
        if (position + length > blob.size) {
            throw ProgramParseError.unexpectedEndOfFile(position, length, blob.size - position)
        }
        val result = blob.copyOfRange(position, position + length)
        position += length
        return result
    }

    @Throws(ProgramParseError::class)
    fun readVarInt(): UInt {
        val offset = position
        var result = 0u
        var shift = 0
        while (true) {
            if (position >= blob.size) {
                throw ProgramParseError.failedToReadVarint(offset)
            }
            val byte = readByte().toUInt()
            result = result or ((byte and 0x7Fu) shl shift)
            if ((byte and 0x80u) == 0u) {
                break
            }
            shift += 7
            if (shift >= 32) {
                throw ProgramParseError.other("VarInt is too long")
            }
        }
        return result
    }

    @Throws(ProgramParseError::class)
    fun skip(count: Int) {
        if (position + count > blob.size) {
            throw ProgramParseError.unexpectedEndOfFile(position, count, blob.size - position)
        }
        position += count
    }

    @Throws(ProgramParseError::class)
    fun readStringWithLength(): String {
        val offset = position
        val length = readVarInt().toInt()
        val bytes = readBytes(length)
        return bytes.toString(Charsets.UTF_8)
    }

    @Throws(ProgramParseError::class)
    fun readSectionAsBytes(sectionRef: KMutableProperty0<UByte>, expectedSection: UByte): ByteArray {
        if (sectionRef.get() != expectedSection) {
            return ByteArray(0)
        }
        val sectionLength = readVarInt().toInt()
        val bytes = readBytes(sectionLength)
        sectionRef.set(readByte())
        return bytes
    }
}

enum class FrameKind {
    Enter,
    Call,
    Line
}

data class LineProgramFrame(
    var kind: FrameKind? = null,
    var namespaceOffset: Int = 0,
    var functionNameOffset: Int = 0,
    var pathOffset: Int = 0,
    var line: Int = 0,
    var column: Int = 0
)

data class RegionInfo(
    val entryIndex: Int,
    val blob: ProgramBlob,
    val range: IntRange,
    val frames: List<LineProgramFrame>
) {
    fun entryIndex(): Int = entryIndex

    fun instructionRange(): IntRange = range

    fun frames(): Iterator<FrameInfo> {
        return frames.map { FrameInfo(blob, it) }.iterator()
    }
}

// FrameInfo.kt

class FrameInfo(
    private val blob: ProgramBlob,
    private val inner: LineProgramFrame
) {
    @Throws(ProgramParseError::class)
    fun namespace(): String? {
        val namespace = blob.getDebugString(inner.namespaceOffset)
        return if (namespace.isEmpty()) null else namespace
    }

    @Throws(ProgramParseError::class)
    fun functionNameWithoutNamespace(): String? {
        val functionName = blob.getDebugString(inner.functionNameOffset)
        return if (functionName.isEmpty()) null else functionName
    }

    fun pathDebugStringOffset(): UInt? =
        if (inner.pathOffset == 0U) null else inner.pathOffset

    @Throws(ProgramParseError::class)
    fun path(): String? {
        val path = blob.getDebugString(inner.pathOffset)
        return if (path.isEmpty()) null else path
    }

    fun line(): UInt? = if (inner.line == 0U) null else inner.line

    fun column(): UInt? = if (inner.column == 0U) null else inner.column

    fun kind(): FrameKind = inner.kind ?: FrameKind.Line

    @Throws(ProgramParseError::class)
    fun fullName(): String {
        val prefix = namespace() ?: ""
        val suffix = functionNameWithoutNamespace() ?: ""
        return if (prefix.isNotEmpty()) "$prefix::$suffix" else suffix
    }

    @Throws(ProgramParseError::class)
    fun location(): SourceLocation? {
        val path = path() ?: return null
        val line = line()
        val column = column()
        return when {
            line != null && column != null -> SourceLocation.Full(path, line, column)
            line != null -> SourceLocation.PathAndLine(path, line)
            else -> SourceLocation.Path(path)
        }
    }
}


class LineProgram(
    private val entryIndex: Int,
    private var regionCounter: Int,
    private val blob: ProgramBlob,
    private val reader: ByteArrayReader,
    private var isFinished: Boolean,
    private var programCounter: UInt,
    private val stack: Array<LineProgramFrame>,
    private var stackDepth: UInt,
    private var mutationDepth: UInt
) {
    @Throws(ProgramParseError::class)
    fun run(): RegionInfo? {
        if (isFinished) return null

        val INSTRUCTION_LIMIT_PER_REGION = 512

        for (i in 0 until INSTRUCTION_LIMIT_PER_REGION) {
            val byte = reader.readByte() ?: throw ProgramParseError("Unexpected end of program")
            val opcode = LineProgramOp.fromByte(byte)
                ?: throw ProgramParseError("Found an unrecognized line program opcode")

            val (count, depth) = when (opcode) {
                LineProgramOp.FinishProgram -> return null
                LineProgramOp.SetMutationDepth -> {
                    mutationDepth = reader.readVarInt()
                    continue
                }

                LineProgramOp.SetKindEnter -> {
                    stack.getOrNull(mutationDepth.toInt())?.kind = FrameKind.Enter
                    continue
                }
                // Handle other opcodes...
                LineProgramOp.FinishInstruction -> Pair(1U, stackDepth)
                LineProgramOp.FinishMultipleInstructions -> {
                    val cnt = reader.readVarInt()
                    Pair(cnt, stackDepth)
                }

                else -> continue
            }

            val range = programCounter until (programCounter + count)
            programCounter += count

            val frames = stack.sliceArray(0 until minOf(depth.toInt(), stack.size))
            isFinished = false

            val regionInfo = RegionInfo(
                entryIndex = regionCounter,
                blob = blob,
                range = range,
                frames = frames
            )
            regionCounter += 1
            return regionInfo
        }

        throw ProgramParseError("Found a line program with too many instructions")
    }
}
