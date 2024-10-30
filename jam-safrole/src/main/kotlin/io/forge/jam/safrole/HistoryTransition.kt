package io.forge.jam.safrole

class HistoryTransition {
    companion object {
        const val BLOCK_HISTORY_LIMIT = 8
        const val MAX_CORES = 341
        val ZERO_HASH = ByteArray(32) { 0 }
    }

    fun stf(input: HistoricalInput, preState: HistoricalState): HistoricalState {
        validateInputs(input)
        val newMmr = appendToMmr(
            if (preState.beta.isEmpty()) {
                HistoricalMmr(peaks = listOf())
            } else {
                preState.beta.last().mmr
            },
            input.accumulateRoot
        )
        val newBlock = HistoricalBeta(
            headerHash = input.headerHash,
            mmr = newMmr,
            stateRoot = ZERO_HASH,
            reported = input.workPackages
        )

        val newBeta = (preState.beta + newBlock).takeLast(BLOCK_HISTORY_LIMIT)
        return HistoricalState(beta = newBeta)
    }

    private fun validateInputs(input: HistoricalInput) {
        require(input.headerHash.size == 32) { "Header hash must be 32 bytes" }
        require(input.parentStateRoot.size == 32) { "Parent state root must be 32 bytes" }
        require(input.accumulateRoot.size == 32) { "Accumulate root must be 32 bytes" }
        require(input.workPackages.size <= MAX_CORES) { "Too many work packages" }
        input.workPackages.forEachIndexed { index, pkg ->
            require(pkg.size == 32) { "Work package $index must be 32 bytes" }
        }
    }

    private fun appendToMmr(previousMmr: HistoricalMmr, newPeak: ByteArray): HistoricalMmr {
        return when {
            previousMmr.peaks.isEmpty() -> HistoricalMmr(peaks = listOf(newPeak))
            previousMmr.peaks.all { it == null } -> HistoricalMmr(peaks = listOf(newPeak))
            else -> {
                // Example logic - this should be replaced with proper MMR merge logic
                val lastNonNullIndex = previousMmr.peaks.indexOfLast { it != null }
                val newPeaks = previousMmr.peaks.take(lastNonNullIndex + 1) + newPeak
                HistoricalMmr(peaks = newPeaks)
            }
        }
    }
}
