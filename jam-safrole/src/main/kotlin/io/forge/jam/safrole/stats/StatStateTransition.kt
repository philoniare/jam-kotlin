package io.forge.jam.safrole.stats

private const val SLOTS_PER_EPOCH = 600

class StatStateTransition(private val statConfig: StatConfig) {
    fun transition(
        input: StatInput,
        preState: StatState
    ): Pair<StatState, StatOutput?> {
        val postState = preState.copy()
        val preEpoch = preState.tau / statConfig.EPOCH_LENGTH
        val postEpoch = input.slot / statConfig.EPOCH_LENGTH
        if (postEpoch > preEpoch) {
            // Rotate stats on epoch transition
            postState.pi.last = postState.pi.current
            postState.pi.current = List(postState.pi.current.size) { StatCount() }
        }
        println("slot: ${input.slot}, Tau: ${preState.tau}, Post: ${postEpoch}, pre: $preEpoch")

        with(postState.pi.current[input.authorIndex.toInt()]) {
            blocks++
            tickets += input.extrinsic.tickets.size
            preImages += input.extrinsic.preimages.size
            preImagesSize += input.extrinsic.preimages.sumOf { it.blob.size }
        }

        // Update guarantees
        input.extrinsic.guarantees.forEach { guarantee ->
            guarantee.signatures.forEach { sig ->
                postState.pi.current[sig.validatorIndex.toInt()].guarantees++
            }
        }

        // Update assurances
        input.extrinsic.assurances.forEach { assurance ->
            postState.pi.current[assurance.validatorIndex.toInt()].assurances++
        }

        return Pair(postState, null)
    }
}
