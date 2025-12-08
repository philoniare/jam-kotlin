package io.forge.jam.safrole.stats

class StatStateTransition(private val statConfig: StatConfig) {
    fun transition(
        input: StatInput,
        preState: StatState
    ): Pair<StatState, StatOutput?> {
        val postState = preState.copy()
        val preEpoch = preState.slot / statConfig.EPOCH_LENGTH
        val postEpoch = input.slot / statConfig.EPOCH_LENGTH

        // Get mutable copies of stats lists
        val currStats = postState.valsCurrStats.map { it.copy() }.toMutableList()

        if (postEpoch > preEpoch) {
            // Rotate stats on epoch transition: current becomes last, reset current
            postState.valsLastStats = postState.valsCurrStats
            for (i in currStats.indices) {
                currStats[i] = StatCount()
            }
        }

        // Update author's stats
        with(currStats[input.authorIndex.toInt()]) {
            blocks++
            tickets += input.extrinsic.tickets.size
            preImages += input.extrinsic.preimages.size
            preImagesSize += input.extrinsic.preimages.sumOf { it.blob.size }
        }

        // Update guarantees - each unique validator who signed any guarantee gets +1 credit per block
        val reporters = mutableSetOf<Int>()
        input.extrinsic.guarantees.forEach { guarantee ->
            guarantee.signatures.forEach { sig ->
                reporters.add(sig.validatorIndex.toInt())
            }
        }
        reporters.forEach { validatorIndex ->
            currStats[validatorIndex].guarantees++
        }

        // Update assurances
        input.extrinsic.assurances.forEach { assurance ->
            currStats[assurance.validatorIndex.toInt()].assurances++
        }

        postState.valsCurrStats = currStats

        return Pair(postState, null)
    }
}
