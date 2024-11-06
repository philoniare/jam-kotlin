package io.forge.jam.pvm.program

import io.forge.jam.pvm.engine.InstructionSet

data class Instructions<I : InstructionSet>(
    private val code: ByteArray,
    private val bitmask: ByteArray,
    private var offset: UInt = 0u,
    private var invalidOffset: UInt? = null,
    private var isBounded: Boolean = false,
    private var isDone: Boolean = false,
    private val instructionSet: I
) : Iterator<ParsedInstruction>, Cloneable {

    override fun hasNext(): Boolean = !isDone && offset.toInt() < code.size

    override fun next(): ParsedInstruction {
        invalidOffset?.let { invalid ->
            invalidOffset = null
            return ParsedInstruction(
                kind = Instruction.Invalid,
                offset = ProgramCounter(invalid),
                nextOffset = ProgramCounter(offset)
            )
        }

        if (isDone || offset.toInt() >= code.size) {
            throw NoSuchElementException("No more instructions")
        }

        val currentOffset = offset
        assert(Program.getBitForOffset(bitmask, code.size, currentOffset)) {
            "Invalid instruction offset"
        }


        val (nextOffset, instruction, isNextInstructionInvalid) =
            Program.parseInstruction(instructionSet, code, bitmask, currentOffset)
        assert(nextOffset > currentOffset)

        if (!isNextInstructionInvalid) {
            offset = nextOffset
            assert(
                offset.toInt() == code.size ||
                    Program.getBitForOffset(bitmask, code.size, offset)
            ) { "bit at $offset is zero" }
        } else {
            when {
                nextOffset.toInt() == code.size -> {
                    offset = (code.size + 1).toUInt()
                }

                isBounded -> {
                    isDone = true
                    offset = if (instruction.opcode().canFallthrough()) {
                        code.size.toUInt()
                    } else {
                        nextOffset
                    }
                }

                else -> {
                    offset = Program.findNextOffsetUnbounded(bitmask, code.size.toUInt(), nextOffset)
                    assert(
                        offset.toInt() == code.size ||
                            Program.getBitForOffset(bitmask, code.size, offset)
                    ) { "bit at $offset is zero" }
                }
            }

            if (instruction.opcode().canFallthrough()) {
                invalidOffset = nextOffset
            }
        }

        return ParsedInstruction(
            kind = instruction,
            offset = ProgramCounter(currentOffset),
            nextOffset = ProgramCounter(nextOffset)
        )
    }

    fun sizeHint(): Pair<Int, Int?> {
        val remaining = code.size - minOf(offset.toInt(), code.size)
        return 0 to remaining
    }


    companion object {
        fun <I : InstructionSet> new(
            instructionSet: I,
            code: ByteArray,
            bitmask: ByteArray,
            offset: UInt,
            isBounded: Boolean
        ): Instructions<I> {
            require(code.size <= UInt.MAX_VALUE.toInt()) {
                "Code size exceeds maximum value"
            }
            require(bitmask.size == (code.size + 7) / 8) {
                "Bitmask size does not match code size"
            }

            val isValid = Program.getBitForOffset(bitmask, code.size, offset)
            var isDone = false
            val (finalOffset, invalidOffset) = if (isValid) {
                offset to null
            } else if (isBounded) {
                isDone = true
                minOf(offset + 1u, code.size.toUInt()) to offset
            } else {
                val nextOffset = Program.findNextOffsetUnbounded(bitmask, code.size.toUInt(), offset)
                nextOffset to offset
            }

            return Instructions(
                code = code,
                bitmask = bitmask,
                offset = finalOffset,
                invalidOffset = invalidOffset,
                isBounded = isBounded,
                isDone = isDone,
                instructionSet = instructionSet
            )
        }
    }

    override fun clone(): Instructions<I> = Instructions(
        code = code.clone(),
        bitmask = bitmask.clone(),
        offset = offset,
        invalidOffset = invalidOffset,
        isBounded = isBounded,
        isDone = isDone,
        instructionSet = instructionSet
    )
}
