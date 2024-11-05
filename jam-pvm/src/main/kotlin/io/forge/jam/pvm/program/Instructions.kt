package io.forge.jam.pvm.program

data class Instructions<I>(
    private val code: ByteArray,
    private val bitmask: ByteArray,
    private var offset: UInt = 0u,
    private var invalidOffset: UInt? = null,
    private var isBounded: Boolean = false,
    private var isDone: Boolean = false,
    private val instructionSet: I
) : Iterator<ParsedInstruction> {
    companion object {
        fun <I> new(
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
