package io.forge.jam.pvm.program

data class ParsedInstruction(val kind: Instruction, val offset: ProgramCounter, val nextOffset: ProgramCounter) {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Instruction = kind

    override fun toString(): String = "ParsedInstruction(kind=$kind, offset=$offset, nextOffset=$nextOffset)"

    fun asInstruction(): Instruction = kind
    
    companion object {
        val ParsedInstruction.instruction: Instruction get() = kind
    }
}
