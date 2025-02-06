package io.forge.jam.safrole.accumulation

import io.forge.jam.core.JamByteArray

class AccumulationStateTransition {
    fun transition(
        input: AccumulationInput,
        preState: AccumulationState
    ): Pair<AccumulationState, AccumulationOutput> {
        val postState = preState.deepCopy()

        return Pair(
            postState,
            AccumulationOutput(
                ok = JamByteArray(byteArrayOf(0))
            )
        )
    }
}
