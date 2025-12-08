package io.forge.jam.pvm.program

data class BranchTarget(
    val pc: ProgramCounter,
    val target: Target
)
