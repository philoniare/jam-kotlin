package io.forge.jam.pvm.engine

/**
 * Represents different backend types for the VM
 */
sealed class InstanceBackend {
    data class Interpreted(val instance: InterpretedInstance) : InstanceBackend()
}
