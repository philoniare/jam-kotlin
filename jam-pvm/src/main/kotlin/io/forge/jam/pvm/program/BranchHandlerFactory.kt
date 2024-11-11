package io.forge.jam.pvm.program

import io.forge.jam.pvm.Target
import io.forge.jam.pvm.engine.Args

typealias BranchCondition<T> = (T, T) -> Boolean

class BranchHandlerFactory<T>(
    val s1: RawReg,
    val s2: UInt,
    val targetTrue: ProgramCounter,
    val targetFalse: ProgramCounter,
    val branchOperator: String,
    val branchCondition: BranchCondition<ULong>,
    val handleArgs: (RawReg, UInt, Target, Target) -> Args,
    val handlerName: String
)
