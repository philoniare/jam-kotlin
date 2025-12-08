package io.forge.jam.pvm.engine

import io.forge.jam.pvm.PvmLogger

data class Engine(
    val selectedBackend: BackendKind,
    val selectedSandbox: SandboxKind?,
    val interpreterEnabled: Boolean,
    val crosscheck: Boolean,
    val state: EngineState,
    val allowDynamicPaging: Boolean
) {
    companion object {
        private val logger = PvmLogger(Engine::class.java)
        fun new(config: Config): Result<Engine> = runCatching {
            val crosscheck = config.crosscheck

            // TODO: Add module caching
            val engineState = EngineState(
                sandboxGlobal = null,
                moduleCache = null,
            )
            val selectedBackend = BackendKind.Interpreter
            logger.debug("Selected backend: $selectedBackend")

            Engine(
                selectedSandbox = SandboxKind.Generic,
                interpreterEnabled = true,
                crosscheck = crosscheck,
                state = EngineState.new(moduleCache = null),
                allowDynamicPaging = config.allowDynamicPaging,
                selectedBackend = selectedBackend
            )
        }
    }
}
