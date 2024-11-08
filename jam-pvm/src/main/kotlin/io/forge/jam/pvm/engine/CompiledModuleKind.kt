package io.forge.jam.pvm.engine

sealed class CompiledModuleKind {
    object Unavailable : CompiledModuleKind()
}
