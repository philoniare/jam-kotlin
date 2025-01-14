package io.forge.jam.safrole.assurance

class AssuranceStateTransition {
    fun transition(
        input: AssuranceInput,
        preState: AssuranceState
    ): Pair<AssuranceState, AssuranceOutput> {
        val postState = preState.copy()

        return Pair(postState, AssuranceOutput(ok = null))
    }
}
