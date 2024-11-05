package io.forge.jam.pvm.engine

import io.forge.jam.pvm.program.ProgramCounter

typealias Handler = (visitor: Visitor) -> Target?

/**
 * Internal implementation of an interpreted VM instance
 */
class InterpretedInstance private constructor(
    val module: Module,
    private val basicMemory: BasicMemory,
    private val dynamicMemory: DynamicMemory,
    val regs: ULongArray,
    var programCounter: ProgramCounter,
    var programCounterValid: Boolean,
    var nextProgramCounter: ProgramCounter?,
    private var nextProgramCounterChanged: Boolean,
    private var cycleCounter: ULong,
    private var gas: Long,
    private val compiledOffsetForBlock: FlatMap<NonZeroUInt>,
    private val compiledHandlers: MutableList<Handler>,
    private val compiledArgs: MutableList<Args>,
    var compiledOffset: UInt,
    var interrupt: InterruptKind,
    private val stepTracing: Boolean
) {
    companion object {
        /**
         * Creates a new instance from a module
         */
        fun newFromModule(module: Module, forceStepTracing: Boolean): InterpretedInstance {
            val stepTracing = module.isStepTracing() || forceStepTracing

            return InterpretedInstance(
                module = module,
                basicMemory = BasicMemory.new(),
                dynamicMemory = DynamicMemory.new(),
                regs = ULongArray(Reg.ALL.size) { 0u },
                programCounter = ProgramCounter(UInt.MAX_VALUE),
                programCounterValid = false,
                nextProgramCounter = null,
                nextProgramCounterChanged = true,
                cycleCounter = 0u,
                gas = 0L,
                // +1 for one implicit out-of-bounds trap
                compiledOffsetForBlock = FlatMap.new(module.codeLen() + 1u),
                compiledHandlers = mutableListOf(),
                compiledArgs = mutableListOf(),
                compiledOffset = 0u,
                interrupt = InterruptKind.Finished,
                stepTracing = stepTracing
            ).apply {
                initializeModule()
            }
        }
    }
}
