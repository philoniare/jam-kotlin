package io.forge.jam.pvm

import io.forge.jam.pvm.engine.Visitor
import io.forge.jam.pvm.program.Compiler.Companion.trapImpl
import io.forge.jam.pvm.program.ProgramCounter

typealias Target = UInt
typealias Handler = (Visitor) -> Target?

object RawHandlers {
    val trap: Handler = { visitor ->
        val args = visitor.inner.compiledArgs[visitor.inner.compiledOffset.toInt()]
        val programCounter = ProgramCounter(args.a0)
        trapImpl(visitor, programCounter)
    }

}
