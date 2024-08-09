package io.forge.jam.pvm

import io.forge.jam.core.Result

// Define UByteArray extension
fun UByteArray.startsWith(needle: UByteArray): Boolean {
    val n = needle.size
    return this.size >= n && this.copyOfRange(0, n).contentEquals(needle)
}

// The magic bytes with which every program blob must start with.
val BLOB_MAGIC: UByteArray =
    ubyteArrayOf('P'.code.toByte().toUByte(), 'V'.code.toByte().toUByte(), 'M'.code.toByte().toUByte(), 0.toUByte())

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

const val BLOB_VERSION_V1: UByte = 1u

const val VERSION_DEBUG_LINE_PROGRAM_V1: UByte = 1u

const val BITMASK_MAX: Int = 24

const val INSTRUCTION_LIMIT_PER_REGION: Int = 512

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
    data class PathAndLine(val path: String, val line: Int) : SourceLocation()
    data class Full(val path: String, val line: Int, val column: Int) : SourceLocation()

    // The path to the original source file.
    fun path(): String = when (this) {
        is Path -> path
        is PathAndLine -> path
        is Full -> path
    }

    // The line in the original source file.
    fun line(): Int? = when (this) {
        is Path -> null
        is PathAndLine -> line
        is Full -> line
    }

    // The column in the original source file.
    fun column(): Int? = when (this) {
        is Path -> null
        is PathAndLine -> null
        is Full -> column
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
    bytes: UByteArray,
    chunkSize: Int,
    compare: (UByteArray) -> Int
): Result<Int, Int> {
    var size = bytes.size / chunkSize
    if (size == 0) {
        return Result.Err(0)
    }

    var base = 0
    while (size > 1) {
        val half = size / 2
        val mid = base + half
        val item = bytes.sliceArray(mid * chunkSize until (mid + 1) * chunkSize)
        when (compare(item)) {
            1 -> {
                // The value we're looking for is to the left of the midpoint.
                size -= half
            }

            -1 -> {
                // The value we're looking for is to the right of the midpoint.
                size -= half
                base = mid
            }

            0 -> {
                // We've found the value, but it might not be the first value.
                val previousItem = bytes.sliceArray((mid - 1) * chunkSize until mid * chunkSize)
                if (compare(previousItem) != 0) {
                    // It is the first value.
                    return Result.Ok(mid * chunkSize)
                }

                // It's not the first value. Let's continue.
                //
                // We could do a linear search here which in the average case
                // would probably be faster, but keeping it as a binary search
                // will avoid a worst-case O(n) scenario.
                size -= half
            }
        }
    }

    val item = bytes.sliceArray(base * chunkSize until (base + 1) * chunkSize)
    val comparison = compare(item)
    return if (comparison == 0) {
        Result.Ok(base * chunkSize)
    } else {
        Result.Err((base + if (comparison < 0) 1 else 0) * chunkSize)
    }
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
    }

    override fun toString(): String {
        return when (kind) {
            is ProgramParseErrorKind.FailedToReadVarint -> {
                "Failed to parse program blob: failed to parse a varint at offset 0x${kind.offset.toString(16)}"
            }

            is ProgramParseErrorKind.FailedToReadStringNonUtf -> {
                "Failed to parse program blob: failed to parse a string at offset 0x${kind.offset.toString(16)} (not valid UTF-8)"
            }

            is ProgramParseErrorKind.UnexpectedSection -> {
                "Failed to parse program blob: found unexpected section at offset 0x${kind.offset.toString(16)}: 0x${
                    kind.section.toString(
                        16
                    )
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
    // args: none
    TRAP(0),
    FALLTHROUGH(17),

    // args: reg, imm
    JUMP_INDIRECT(19),
    LOAD_IMM(4),
    LOAD_U8(60),
    LOAD_I8(74),
    LOAD_U16(76),
    LOAD_I16(66),
    LOAD_U32(10),
    STORE_U8(71),
    STORE_U16(69),
    STORE_U32(22),

    // args: reg, imm, offset
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

    // args: reg, imm, imm
    STORE_IMM_INDIRECT_U8(26),
    STORE_IMM_INDIRECT_U16(54),
    STORE_IMM_INDIRECT_U32(13),

    // args: reg, reg, imm
    STORE_INDIRECT_U8(16),
    STORE_INDIRECT_U16(29),
    STORE_INDIRECT_U32(3),
    LOAD_INDIRECT_U8(11),
    LOAD_INDIRECT_I8(21),
    LOAD_INDIRECT_U16(37),
    LOAD_INDIRECT_I16(33),
    LOAD_INDIRECT_U32(1),
    ADD_IMM(2),
    AND_IMM(18),
    XOR_IMM(31),
    OR_IMM(49),
    MUL_IMM(35),
    MUL_UPPER_SIGNED_SIGNED_IMM(65),
    MUL_UPPER_UNSIGNED_UNSIGNED_IMM(63),
    SET_LESS_THAN_UNSIGNED_IMM(27),
    SET_LESS_THAN_SIGNED_IMM(56),
    SHIFT_LOGICAL_LEFT_IMM(9),
    SHIFT_LOGICAL_RIGHT_IMM(14),
    SHIFT_ARITHMETIC_RIGHT_IMM(25),
    NEGATE_AND_ADD_IMM(40),
    SET_GREATER_THAN_UNSIGNED_IMM(39),
    SET_GREATER_THAN_SIGNED_IMM(61),
    SHIFT_LOGICAL_RIGHT_IMM_ALT(72),
    SHIFT_ARITHMETIC_RIGHT_IMM_ALT(80),
    SHIFT_LOGICAL_LEFT_IMM_ALT(75),

    CMOV_IF_ZERO_IMM(85),
    CMOV_IF_NOT_ZERO_IMM(86),

    // args: reg, reg, offset
    BRANCH_EQ(24),
    BRANCH_NOT_EQ(30),
    BRANCH_LESS_UNSIGNED(47),
    BRANCH_LESS_SIGNED(48),
    BRANCH_GREATER_OR_EQUAL_UNSIGNED(41),
    BRANCH_GREATER_OR_EQUAL_SIGNED(43),

    // args: reg, reg, reg
    ADD(8),
    SUB(20),
    AND(23),
    XOR(28),
    OR(12),
    MUL(34),
    MUL_UPPER_SIGNED_SIGNED(67),
    MUL_UPPER_UNSIGNED_UNSIGNED(57),
    MUL_UPPER_SIGNED_UNSIGNED(81),
    SET_LESS_THAN_UNSIGNED(36),
    SET_LESS_THAN_SIGNED(58),
    SHIFT_LOGICAL_LEFT(55),
    SHIFT_LOGICAL_RIGHT(51),
    SHIFT_ARITHMETIC_RIGHT(77),
    DIV_UNSIGNED(68),
    DIV_SIGNED(64),
    REM_UNSIGNED(73),
    REM_SIGNED(70),

    CMOV_IF_ZERO(83),
    CMOV_IF_NOT_ZERO(84),

    // args: offset
    JUMP(5),

    // args: imm
    ECALLI(78),

    // args: imm, imm
    STORE_IMM_U8(62),
    STORE_IMM_U16(79),
    STORE_IMM_U32(38),

    // args: reg, reg
    MOVE_REG(82),
    SBRK(87),

    // args: reg, reg, imm, imm
    LOAD_IMM_AND_JUMP_INDIRECT(42),

    // Invalid opcode
    INVALID(88);

    companion object {
        private val map = entries.associateBy(Opcode::value)

        fun fromInt(type: Int) = map[type] ?: INVALID
    }
}

interface InstructionVisitor<T> {
    fun visitArgless(): T
    fun visitRegImm(reg: Int, imm: Int): T
    fun visitRegImmOffset(reg: Int, imm1: Int, imm2: Int): T
    fun visitRegImmImm(reg: Int, imm1: Int, imm2: Int): T
    fun visitRegRegImm(reg1: Int, reg2: Int, imm: Int): T
    fun visitRegRegOffset(reg1: Int, reg2: Int, imm: Int): T
    fun visitRegRegReg(reg1: Int, reg2: Int, reg3: Int): T
    fun visitOffset(imm: Int): T
    fun visitImm(imm: Int): T
    fun visitImmImm(imm1: Int, imm2: Int): T
    fun visitRegReg(reg1: Int, reg2: Int): T
    fun visitRegRegImmImm(reg1: Int, reg2: Int, imm1: Int, imm2: Int): T

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
    val offset: Int,
    val length: Int
) {
    override fun toString(): String {
        return String.format("%7d: %s", offset, kind)
    }
}

sealed class Instruction {
    // Instructions with args: none
    object Trap : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitArgless()
        override fun opcode(): Opcode = Opcode.TRAP
    }

    object Fallthrough : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitArgless()
        override fun opcode(): Opcode = Opcode.FALLTHROUGH
    }

    // Instructions with args: reg, imm
    data class JumpIndirect(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.JUMP_INDIRECT
    }

    data class LoadImm(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_IMM
    }

    data class LoadU8(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_U8
    }

    data class LoadI8(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_I8
    }

    data class LoadU16(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_U16
    }

    data class LoadI16(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_I16
    }

    data class LoadU32(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.LOAD_U32
    }

    data class StoreU8(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.STORE_U8
    }

    data class StoreU16(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.STORE_U16
    }

    data class StoreU32(val reg: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImm(reg, imm)
        override fun opcode(): Opcode = Opcode.STORE_U32
    }

    // Instructions with args: reg, imm, offset
    data class LoadImmAndJump(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.LOAD_IMM_AND_JUMP
    }

    data class BranchEqImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_EQ_IMM
    }

    data class BranchNotEqImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_NOT_EQ_IMM
    }

    data class BranchLessUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_UNSIGNED_IMM
    }

    data class BranchLessSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_SIGNED_IMM
    }

    data class BranchGreaterOrEqualUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_OR_EQUAL_UNSIGNED_IMM
    }

    data class BranchGreaterOrEqualSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_OR_EQUAL_SIGNED_IMM
    }

    data class BranchLessOrEqualSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_OR_EQUAL_SIGNED_IMM
    }

    data class BranchLessOrEqualUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_OR_EQUAL_UNSIGNED_IMM
    }

    data class BranchGreaterSignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_SIGNED_IMM
    }

    data class BranchGreaterUnsignedImm(val reg: Int, val imm: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmOffset(reg, imm, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_UNSIGNED_IMM
    }

    // Instructions with args: reg, imm, imm
    data class StoreImmIndirectU8(val reg: Int, val imm1: Int, val imm2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmImm(reg, imm1, imm2)
        override fun opcode(): Opcode = Opcode.STORE_IMM_INDIRECT_U8
    }

    data class StoreImmIndirectU16(val reg: Int, val imm1: Int, val imm2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmImm(reg, imm1, imm2)
        override fun opcode(): Opcode = Opcode.STORE_IMM_INDIRECT_U16
    }

    data class StoreImmIndirectU32(val reg: Int, val imm1: Int, val imm2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegImmImm(reg, imm1, imm2)
        override fun opcode(): Opcode = Opcode.STORE_IMM_INDIRECT_U32
    }

    // Instructions with args: reg, reg, imm
    data class StoreIndirectU8(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.STORE_INDIRECT_U8
    }

    data class StoreIndirectU16(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.STORE_INDIRECT_U16
    }

    data class StoreIndirectU32(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.STORE_INDIRECT_U32
    }

    data class LoadIndirectU8(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.LOAD_INDIRECT_U8
    }

    data class LoadIndirectI8(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.LOAD_INDIRECT_I8
    }

    data class LoadIndirectU16(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.LOAD_INDIRECT_U16
    }

    data class LoadIndirectI16(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.LOAD_INDIRECT_I16
    }

    data class LoadIndirectU32(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.LOAD_INDIRECT_U32
    }

    data class AddImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.ADD_IMM
    }

    data class AndImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.AND_IMM
    }

    data class XorImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.XOR_IMM
    }

    data class OrImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.OR_IMM
    }

    data class MulImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.MUL_IMM
    }

    data class MulUpperSignedSignedImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.MUL_UPPER_SIGNED_SIGNED_IMM
    }

    data class MulUpperUnsignedUnsignedImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.MUL_UPPER_UNSIGNED_UNSIGNED_IMM
    }

    data class SetLessThanUnsignedImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SET_LESS_THAN_UNSIGNED_IMM
    }

    data class SetLessThanSignedImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SET_LESS_THAN_SIGNED_IMM
    }

    data class ShiftLogicalLeftImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SHIFT_LOGICAL_LEFT_IMM
    }

    data class ShiftLogicalRightImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SHIFT_LOGICAL_RIGHT_IMM
    }

    data class ShiftArithmeticRightImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SHIFT_ARITHMETIC_RIGHT_IMM
    }

    data class NegateAndAddImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.NEGATE_AND_ADD_IMM
    }

    data class SetGreaterThanUnsignedImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SET_GREATER_THAN_UNSIGNED_IMM
    }

    data class SetGreaterThanSignedImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SET_GREATER_THAN_SIGNED_IMM
    }

    data class ShiftLogicalRightImmAlt(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SHIFT_LOGICAL_RIGHT_IMM_ALT
    }

    data class ShiftArithmeticRightImmAlt(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SHIFT_ARITHMETIC_RIGHT_IMM_ALT
    }

    data class ShiftLogicalLeftImmAlt(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.SHIFT_LOGICAL_LEFT_IMM_ALT
    }

    data class CmovIfZeroImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.CMOV_IF_ZERO_IMM
    }

    data class CmovIfNotZeroImm(val reg1: Int, val reg2: Int, val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImm(reg1, reg2, imm)
        override fun opcode(): Opcode = Opcode.CMOV_IF_NOT_ZERO_IMM
    }

    // Instructions with args: reg, reg, offset
    data class BranchEq(val reg1: Int, val reg2: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegOffset(reg1, reg2, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_EQ
    }

    data class BranchNotEq(val reg1: Int, val reg2: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegOffset(reg1, reg2, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_NOT_EQ
    }

    data class BranchLessUnsigned(val reg1: Int, val reg2: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegOffset(reg1, reg2, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_UNSIGNED
    }

    data class BranchLessSigned(val reg1: Int, val reg2: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegOffset(reg1, reg2, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_LESS_SIGNED
    }

    data class BranchGreaterOrEqualUnsigned(val reg1: Int, val reg2: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegOffset(reg1, reg2, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_OR_EQUAL_UNSIGNED
    }

    data class BranchGreaterOrEqualSigned(val reg1: Int, val reg2: Int, val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegOffset(reg1, reg2, offset)
        override fun opcode(): Opcode = Opcode.BRANCH_GREATER_OR_EQUAL_SIGNED
    }

    // Instructions with args: reg, reg, reg
    data class Add(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.ADD
    }

    data class Sub(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.SUB
    }

    data class And(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.AND
    }

    data class Xor(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.XOR
    }

    data class Or(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.OR
    }

    data class Mul(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.MUL
    }

    data class MulUpperSignedSigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.MUL_UPPER_SIGNED_SIGNED
    }

    data class MulUpperUnsignedUnsigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.MUL_UPPER_UNSIGNED_UNSIGNED
    }

    data class MulUpperSignedUnsigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.MUL_UPPER_SIGNED_UNSIGNED
    }

    data class SetLessThanUnsigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.SET_LESS_THAN_UNSIGNED
    }

    data class SetLessThanSigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.SET_LESS_THAN_SIGNED
    }

    data class ShiftLogicalLeft(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.SHIFT_LOGICAL_LEFT
    }

    data class ShiftLogicalRight(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.SHIFT_LOGICAL_RIGHT
    }

    data class ShiftArithmeticRight(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.SHIFT_ARITHMETIC_RIGHT
    }

    data class DivUnsigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.DIV_UNSIGNED
    }

    data class DivSigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.DIV_SIGNED
    }

    data class RemUnsigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.REM_UNSIGNED
    }

    data class RemSigned(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.REM_SIGNED
    }

    data class CmovIfZero(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.CMOV_IF_ZERO
    }

    data class CmovIfNotZero(val reg1: Int, val reg2: Int, val reg3: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegReg(reg1, reg2, reg3)
        override fun opcode(): Opcode = Opcode.CMOV_IF_NOT_ZERO
    }

    // Instructions with args: offset
    data class Jump(val offset: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitOffset(offset)
        override fun opcode(): Opcode = Opcode.JUMP
    }

    // Instructions with args: imm
    data class Ecalli(val imm: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitImm(imm)
        override fun opcode(): Opcode = Opcode.ECALLI
    }

    // Instructions with args: imm, imm
    data class StoreImmU8(val imm1: Int, val imm2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitImmImm(imm1, imm2)
        override fun opcode(): Opcode = Opcode.STORE_IMM_U8
    }

    data class StoreImmU16(val imm1: Int, val imm2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitImmImm(imm1, imm2)
        override fun opcode(): Opcode = Opcode.STORE_IMM_U16
    }

    data class StoreImmU32(val imm1: Int, val imm2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitImmImm(imm1, imm2)
        override fun opcode(): Opcode = Opcode.STORE_IMM_U32
    }

    // Instructions with args: reg, reg
    data class MoveReg(val reg1: Int, val reg2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegReg(reg1, reg2)
        override fun opcode(): Opcode = Opcode.MOVE_REG
    }

    data class Sbrk(val reg1: Int, val reg2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegReg(reg1, reg2)
        override fun opcode(): Opcode = Opcode.SBRK
    }

    // Instructions with args: reg, reg, imm, imm
    data class LoadImmAndJumpIndirect(val reg1: Int, val reg2: Int, val imm1: Int, val imm2: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitRegRegImmImm(reg1, reg2, imm1, imm2)
        override fun opcode(): Opcode = Opcode.LOAD_IMM_AND_JUMP_INDIRECT
    }

    // Invalid opcode
    data class Invalid(val value: Int) : Instruction() {
        override fun <T> visit(visitor: InstructionVisitor<T>): T = visitor.visitInvalid()
        override fun opcode(): Opcode = Opcode.INVALID
    }

    abstract fun <T> visit(visitor: InstructionVisitor<T>): T
    abstract fun opcode(): Opcode
}


//data class ProgramParts(
//    var roDataSize: Int = 0,
//    var rwDataSize: Int = 0,
//    var stackSize: Int = 0,
//
//    var roData: UByteArray = UByteArray(0),
//    var rwData: UByteArray = UByteArray(0),
//    var codeAndJumpTable: UByteArray = UByteArray(0),
//    var importOffsets: UByteArray = UByteArray(0),
//    var importSymbols: UByteArray = UByteArray(0),
//    var exports: UByteArray = UByteArray(0),
//
//    var debugStrings: UByteArray = UByteArray(0),
//    var debugLineProgramRanges: UByteArray = UByteArray(0),
//    var debugLinePrograms: UByteArray = UByteArray(0)
//) {
//
//    companion object {
//        fun fromBytes(blob: UByteArray): ProgramParts {
//            if (!blob.startsWith(BLOB_MAGIC)) {
//                throw ProgramParseError(ProgramParseErrorKind.Other("Blob doesn't start with the expected magic bytes"))
//            }
//
//            val reader = Reader(blob, BLOB_MAGIC.size)
//            val blobVersion = reader.readByte()
//            if (blobVersion != BLOB_VERSION_V1) {
//                throw ProgramParseError(ProgramParseErrorKind.UnsupportedVersion(blobVersion))
//            }
//
//            val parts = ProgramParts()
//            var section = reader.readByte()
//            if (section == SECTION_MEMORY_CONFIG) {
//                val sectionLength = reader.readVarInt()
//                val position = reader.position
//                parts.roDataSize = reader.readVarInt()
//                parts.rwDataSize = reader.readVarInt()
//                parts.stackSize = reader.readVarInt()
//                if (position + sectionLength != reader.position) {
//                    throw ProgramParseError(ProgramParseErrorKind.Other("The memory config section contains more data than expected"))
//                }
//                section = reader.readByte()
//            }
//
//            parts.roData = reader.readSectionAsBytes(section, SECTION_RO_DATA)
//            parts.rwData = reader.readSectionAsBytes(section, SECTION_RW_DATA)
//
//            if (section == SECTION_IMPORTS) {
//                val sectionLength = reader.readVarInt()
//                val sectionStart = reader.position
//                val importCount = reader.readVarInt()
//                if (importCount > VM_MAXIMUM_IMPORT_COUNT) {
//                    throw ProgramParseError(ProgramParseErrorKind.Other("Too many imports"))
//                }
//
//                val importOffsetsSize = importCount * 4
//                parts.importOffsets = reader.readSliceAsBytes(importOffsetsSize)
//                val importSymbolsSize = sectionLength - (reader.position - sectionStart)
//                parts.importSymbols = reader.readSliceAsBytes(importSymbolsSize)
//                section = reader.readByte()
//            }
//
//            parts.exports = reader.readSectionAsBytes(section, SECTION_EXPORTS)
//            parts.codeAndJumpTable = reader.readSectionAsBytes(section, SECTION_CODE_AND_JUMP_TABLE)
//            parts.debugStrings = reader.readSectionAsBytes(section, SECTION_OPT_DEBUG_STRINGS)
//            parts.debugLinePrograms = reader.readSectionAsBytes(section, SECTION_OPT_DEBUG_LINE_PROGRAMS)
//            parts.debugLineProgramRanges = reader.readSectionAsBytes(section, SECTION_OPT_DEBUG_LINE_PROGRAM_RANGES)
//
//            while (section and 0b10000000 != 0) {
//                val sectionLength = reader.readVarInt()
//                reader.skip(sectionLength)
//                section = reader.readByte()
//            }
//
//            if (section != SECTION_END_OF_FILE) {
//                throw ProgramParseError(ProgramParseErrorKind.UnexpectedSection(reader.position - 1, section))
//            }
//
//            return parts
//        }
//    }
//}
//
//
//class ProgramBlob private constructor(
//    val roDataSize: Int,
//    val rwDataSize: Int,
//    val stackSize: Int,
//
//    val roData: UByteArray,
//    val rwData: UByteArray,
//    val exports: UByteArray,
//    val importSymbols: UByteArray,
//    val importOffsets: UByteArray,
//    val code: UByteArray,
//    val jumpTable: UByteArray,
//    val jumpTableEntrySize: UByte,
//    val bitmask: UByteArray,
//
//    val debugStrings: UByteArray,
//    val debugLineProgramRanges: UByteArray,
//    val debugLinePrograms: UByteArray
//) {
//
//    companion object {
//        fun parse(bytes: UByteArray): ProgramBlob {
//            val parts = ProgramParts.fromBytes(bytes)
//            return fromParts(parts)
//        }
//
//        fun fromParts(parts: ProgramParts): ProgramBlob {
//            val blob = ProgramBlob(
//                roDataSize = parts.roDataSize,
//                rwDataSize = parts.rwDataSize,
//                stackSize = parts.stackSize,
//
//                roData = parts.roData,
//                rwData = parts.rwData,
//                exports = parts.exports,
//                importSymbols = parts.importSymbols,
//                importOffsets = parts.importOffsets,
//                code = parts.codeAndJumpTable,
//                jumpTable = UByteArray(0), // Assuming this is populated somewhere in real usage
//                jumpTableEntrySize = 0u,   // Assuming this is set somewhere in real usage
//                bitmask = UByteArray(0),   // Assuming this is set somewhere in real usage
//
//                debugStrings = parts.debugStrings,
//                debugLineProgramRanges = parts.debugLineProgramRanges,
//                debugLinePrograms = parts.debugLinePrograms
//            )
//
//            // Additional logic here similar to your Rust code...
//
//            return blob
//        }
//    }
//
//    fun roData(): UByteArray = roData
//    fun roDataSize(): Int = roDataSize
//    fun rwData(): UByteArray = rwData
//    fun rwDataSize(): Int = rwDataSize
//    fun stackSize(): Int = stackSize
//    fun code(): UByteArray = code
//    fun bitmask(): UByteArray = bitmask
//
//    fun imports(): Imports {
//        return Imports(importOffsets, importSymbols)
//    }
//
//    fun exports(): Iterator<ProgramExport<UByteArray>> {
//        return object : Iterator<ProgramExport<UByteArray>> {
//            private var state: State = State.Uninitialized
//            private val reader = Reader(exports, 0)
//
//            override fun hasNext(): Boolean {
//                return when (state) {
//                    State.Uninitialized -> reader.readVarInt() != null
//                    State.Pending -> true
//                    State.Finished -> false
//                }
//            }
//
//            override fun next(): ProgramExport<UByteArray>? {
//                val remaining = when (val currentState = state) {
//                    State.Uninitialized -> reader.readVarInt()
//                    State.Pending -> currentState.remaining
//                    State.Finished -> return null
//                }
//
//                if (remaining == 0) return null
//
//                val targetCodeOffset = reader.readVarInt()
//                val symbol = reader.readBytesWithLength()
//                state = State.Pending(remaining - 1)
//
//                return ProgramExport(targetCodeOffset, ProgramSymbol(symbol))
//            }
//        }
//    }
//
//    fun instructions(): Instructions {
//        return Instructions.new(code, bitmask, 0)
//    }
//
//    fun instructionsAt(offset: Int): Instructions? {
//        return if (bitmask[offset shr 3].toInt() and (1 shl (offset and 7)) == 0) {
//            null
//        } else {
//            Instructions.new(code, bitmask, offset)
//        }
//    }
//
//    fun jumpTable(): JumpTable {
//        return JumpTable(jumpTable, jumpTableEntrySize.toInt())
//    }
//
//    fun getDebugString(offset: Int): String {
//        val reader = Reader(debugStrings, offset)
//        return reader.readStringWithLength()
//    }
//
//    fun getDebugLineProgramAt(nthInstruction: Int): LineProgram? {
//        if (debugLineProgramRanges.isEmpty() || debugLinePrograms.isEmpty()) {
//            return null
//        }
//
//        if (debugLinePrograms[0] != VERSION_DEBUG_LINE_PROGRAM_V1) {
//            throw ProgramParseError(ProgramParseErrorKind.Other("Unsupported debug line program version"))
//        }
//
//        val entrySize = 12
//        if (debugLineProgramRanges.size % entrySize != 0) {
//            throw ProgramParseError(ProgramParseErrorKind.Other("Invalid size for debug function ranges section"))
//        }
//
//        val offset = binarySearch(debugLineProgramRanges, entrySize) { xs ->
//            val begin = xs.copyOfRange(0, 4).toInt()
//            when {
//                nthInstruction < begin -> Ordering.Greater
//                nthInstruction >= xs.copyOfRange(4, 8).toInt() -> Ordering.Less
//                else -> Ordering.Equal
//            }
//        }
//
//        val infoOffset = debugLineProgramRanges.copyOfRange(offset + 8, offset + 12).toInt()
//        val reader = Reader(debugLinePrograms, infoOffset)
//        return LineProgram(
//            entryIndex = offset / entrySize,
//            regionCounter = 0,
//            blob = this,
//            reader = reader,
//            isFinished = false,
//            programCounter = debugLineProgramRanges.copyOfRange(offset, offset + 4).toInt(),
//            stack = Array(16) { LineProgramFrame() },
//            stackDepth = 0,
//            mutationDepth = 0
//        )
//    }
//}
//
//data class Imports(
//    val offsets: UByteArray,
//    val symbols: UByteArray
//)
//
//data class ProgramExport(
//    val targetCodeOffset: Int,
//    val symbol: ProgramSymbol
//)
//
//data class ProgramSymbol(
//    val value: T
//)
//
//sealed class State {
//    object Uninitialized : State()
//    data class Pending(val remaining: Int) : State()
//    object Finished : State()
//}
//
//class Reader(
//    val blob: UByteArray,
//    var position: Int
//) {
//
//    fun readSlice(length: Int): Result<UByteArray, ProgramParseError> {
//        val blobSlice = blob.sliceArray(position until blob.size)
//        val slice = blobSlice.takeIf { it.size >= length } ?: return Result.Err(
//            ProgramParseError.unexpectedEndOfFile(
//                position,
//                length,
//                blobSlice.size
//            )
//        )
//        position += length
//        return Result.Ok(slice)
//    }
//
//
//    fun readByte(): Result<UByte, ProgramParseError> {
//        return readSlice(1).map { it[0] }
//    }
//
//    fun readVarint(): Result<UInt, ProgramParseError> {
//        val firstByte = readByte()
//        val (length, value) = readVarint(blob.sliceArray(position until blob.size), firstByte)
//    }
//
//    fun readSliceAsBytes(length: Int): UByteArray {
//        val slice = blob.copyOfRange(position, position + length)
//        position += length
//        return slice
//    }
//
//    fun readBytesWithLength(): Result<UByteArray, ProgramParseError> {
//        val length =
//            readVarint().toInt() ?: return Result.Err(ProgramParseError.unexpectedEndOfFile(position, 4, 0))
//        return readSlice(length)
//    }
//
//    fun readStringWithLength(): String {
//        val offset = position
//        val slice = readBytesWithLength()
//        return String(slice.toByteArray(), Charsets.UTF_8).takeIf { it.isNotEmpty() }
//            ?: return Err(ProgramParseError.failedToReadStringNonUtf(offset))
//    }
//
//    fun skip(length: Int) {
//        position += length
//    }
//
//    fun readSliceAsRange(count: Int): Result<IntRange, ProgramParseError> {
//        val blobSlice = blob.sliceArray(position until blob.size)
//        if (blobSlice.size < count) {
//            return Result.Err(ProgramParseError.unexpectedEndOfFile(position, count, blobSlice.size))
//        }
//        val range = position until position + count
//        position += count
//        return Result.Ok(range)
//    }
//}
//
//enum class FrameKind {
//    Enter,
//    Call,
//    Line
//}
//
//data class LineProgramFrame(
//    var kind: FrameKind? = null,
//    var namespaceOffset: Int = 0,
//    var functionNameOffset: Int = 0,
//    var pathOffset: Int = 0,
//    var line: Int = 0,
//    var column: Int = 0
//)
//
//data class RegionInfo(
//    val entryIndex: Int,
//    val blob: ProgramBlob,
//    val range: IntRange,
//    val frames: List<LineProgramFrame>
//) {
//    fun entryIndex(): Int = entryIndex
//
//    fun instructionRange(): IntRange = range
//
//    fun frames(): Iterator<FrameInfo> {
//        return frames.map { FrameInfo(blob, it) }.iterator()
//    }
//}
//
//data class FrameInfo(var blob: ProgramBlob, val inner: LineProgramFrame) {
//    fun namespace(): Result<String?, ProgramParseError> {
//        val namespace = blob.getDebugString(inner.namespaceOffset)
//        return if (namespace.isEmpty()) {
//            Result.Ok(null)
//        } else {
//            Result.Ok(namespace)
//        }
//    }
//
//    fun functionNameWithoutNamespace(): Result<String?, ProgramParseError> {
//        val functionName = blob.getDebugString(inner.functionNameOffset)
//        return if (functionName.isEmpty()) {
//            Result.Ok(null)
//        } else {
//            Result.Ok(functionName)
//        }
//    }
//
//    fun pathDebugStringOffset(): Int? {
//        return if (inner.pathOffset == 0) null else inner.pathOffset
//    }
//
//    fun path(): Result<String?, ProgramParseError> {
//        val path = blob.getDebugString(inner.pathOffset)
//        return if (path.isEmpty()) {
//            Result.Ok(null)
//        } else {
//            Result.Ok(path)
//        }
//    }
//
//    fun line(): Int? {
//        return if (inner.line == 0) null else inner.line
//    }
//
//    fun column(): Int? {
//        return if (inner.column == 0) null else inner.column
//    }
//
//    fun kind(): FrameKind {
//        return inner.kind ?: FrameKind.Line
//    }
//
//    fun fullName(): Result<DisplayName, ProgramParseError> {
//        return Result.Ok(
//            DisplayName(
//                prefix = namespace().getOrNull() ?: "",
//                suffix = functionNameWithoutNamespace().getOrNull() ?: ""
//            )
//        )
//    }
//
//    fun location(): Result<SourceLocation?, ProgramParseError> {
//        val path = path().getOrNull()
//        val line = line()
//        val column = column()
//
//        return if (path != null) {
//            if (line != null) {
//                if (column != null) {
//                    Result.Ok(SourceLocation.Full(path, line, column))
//                } else {
//                    Result.Ok(SourceLocation.PathAndLine(path, line))
//                }
//            } else {
//                Result.Ok(SourceLocation.Path(path))
//            }
//        } else {
//            Result.Ok(null)
//        }
//    }
//}
//
//
//class LineProgram(
//    private val entryIndex: Int,
//    private var regionCounter: Int,
//    private val blob: ProgramBlob,
//    private val reader: Reader<ArcBytes>,
//    private var isFinished: Boolean = false,
//    private var programCounter: Int = 0,
//    private val stack: Array<LineProgramFrame> = Array(16) { LineProgramFrame() },
//    private var stackDepth: Int = 0,
//    private var mutationDepth: Int = 0
//) {
//    fun entryIndex(): Int = entryIndex
//
//    fun run(): Result<RegionInfo?, ProgramParseError> {
//        class SetTrueOnDrop(var flag: Boolean) : AutoCloseable {
//            override fun close() {
//                flag = true
//            }
//        }
//
//        if (isFinished) {
//            return Result.Ok(null)
//        }
//
//        val markAsFinishedOnDrop = SetTrueOnDrop(isFinished)
//
//        for (i in 0 until INSTRUCTION_LIMIT_PER_REGION) {
//            val byte = reader.readByte().getOrElse {
//                return Result.Err(it)
//            }
//
//            val opcode = LineProgramOp.fromByte(byte) ?: return Result.Err(
//                ProgramParseError(ProgramParseErrorKind.Other("found an unrecognized line program opcode"))
//            )
//
//            val (count, depth) = when (opcode) {
//                LineProgramOp.FinishProgram -> return Result.Ok(null)
//                LineProgramOp.SetMutationDepth -> {
//                    mutationDepth = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    continue
//                }
//
//                LineProgramOp.SetKindEnter -> {
//                    stack.getOrNull(mutationDepth)?.kind = FrameKind.Enter
//                    continue
//                }
//
//                LineProgramOp.SetKindCall -> {
//                    stack.getOrNull(mutationDepth)?.kind = FrameKind.Call
//                    continue
//                }
//
//                LineProgramOp.SetKindLine -> {
//                    stack.getOrNull(mutationDepth)?.kind = FrameKind.Line
//                    continue
//                }
//
//                LineProgramOp.SetNamespace -> {
//                    val value = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    stack.getOrNull(mutationDepth)?.namespaceOffset = value
//                    continue
//                }
//
//                LineProgramOp.SetFunctionName -> {
//                    val value = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    stack.getOrNull(mutationDepth)?.functionNameOffset = value
//                    continue
//                }
//
//                LineProgramOp.SetPath -> {
//                    val value = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    stack.getOrNull(mutationDepth)?.pathOffset = value
//                    continue
//                }
//
//                LineProgramOp.SetLine -> {
//                    val value = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    stack.getOrNull(mutationDepth)?.line = value
//                    continue
//                }
//
//                LineProgramOp.SetColumn -> {
//                    val value = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    stack.getOrNull(mutationDepth)?.column = value
//                    continue
//                }
//
//                LineProgramOp.SetStackDepth -> {
//                    stackDepth = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    continue
//                }
//
//                LineProgramOp.IncrementLine -> {
//                    stack.getOrNull(mutationDepth)?.let { it.line += 1 }
//                    continue
//                }
//
//                LineProgramOp.AddLine -> {
//                    val value = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    stack.getOrNull(mutationDepth)?.let { it.line += value }
//                    continue
//                }
//
//                LineProgramOp.SubLine -> {
//                    val value = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    stack.getOrNull(mutationDepth)?.let { it.line -= value }
//                    continue
//                }
//
//                LineProgramOp.FinishInstruction -> Pair(1, stackDepth)
//                LineProgramOp.FinishMultipleInstructions -> {
//                    val count = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    Pair(count, stackDepth)
//                }
//
//                LineProgramOp.FinishInstructionAndIncrementStackDepth -> {
//                    val depth = stackDepth
//                    stackDepth += 1
//                    Pair(1, depth)
//                }
//
//                LineProgramOp.FinishMultipleInstructionsAndIncrementStackDepth -> {
//                    val count = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    val depth = stackDepth
//                    stackDepth += 1
//                    Pair(count, depth)
//                }
//
//                LineProgramOp.FinishInstructionAndDecrementStackDepth -> {
//                    val depth = stackDepth
//                    stackDepth -= 1
//                    Pair(1, depth)
//                }
//
//                LineProgramOp.FinishMultipleInstructionsAndDecrementStackDepth -> {
//                    val count = reader.readVarInt().getOrElse {
//                        return Result.Err(it)
//                    }
//                    val depth = stackDepth
//                    stackDepth -= 1
//                    Pair(count, depth)
//                }
//            }
//
//            val range = programCounter until (programCounter + count)
//            programCounter += count
//
//            val frames = stack.sliceArray(0 until kotlin.math.min(depth, stack.size))
//            markAsFinishedOnDrop.close()
//
//            val entryIndex = regionCounter++
//            return Result.Ok(RegionInfo(entryIndex, blob, range, frames.toList()))
//        }
//
//        return Result.Err(
//            ProgramParseError(ProgramParseErrorKind.Other("found a line program with too many instructions"))
//        )
//    }
//}
