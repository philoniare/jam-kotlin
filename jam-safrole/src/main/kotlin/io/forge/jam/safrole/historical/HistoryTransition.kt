package io.forge.jam.safrole.historical

import io.forge.jam.core.JamByteArray
import org.bouncycastle.crypto.digests.KeccakDigest

class HistoryTransition {
    companion object {
        const val BLOCK_HISTORY_LIMIT = 8
        const val MAX_CORES = 341
        val ZERO_HASH = ByteArray(32) { 0 }

        // MMR peak prefix: "peak" (without $)
        val PEAK_PREFIX = "peak".toByteArray(Charsets.US_ASCII)
    }

    fun stf(input: HistoricalInput, preState: HistoricalState): HistoricalState {
        validateInputs(input)

        val history = preState.beta.history
        val currentMmr = preState.beta.mmr

        val updatedHistory =
                if (history.isNotEmpty()) {
                    val lastBlock = history.last().copy(stateRoot = input.parentStateRoot)
                    history.dropLast(1) + lastBlock
                } else {
                    history
                }

        val newMmr = appendToMmr(currentMmr, input.accumulateRoot)

        // Calculate beefy root from the new MMR peaks
        val beefyRoot = calculateBeefyRoot(newMmr)

        val newBlock =
                HistoricalBeta(
                        headerHash = input.headerHash,
                        beefyRoot = beefyRoot,
                        stateRoot = JamByteArray(ZERO_HASH),
                        reported = input.workPackages
                )

        val newHistory = (updatedHistory + newBlock).takeLast(BLOCK_HISTORY_LIMIT)
        return HistoricalState(beta = HistoricalBetaContainer(history = newHistory, mmr = newMmr))
    }

    private fun validateInputs(input: HistoricalInput) {
        require(input.headerHash.size == 32) { "Header hash must be 32 bytes" }
        require(input.parentStateRoot.size == 32) { "Parent state root must be 32 bytes" }
        require(input.accumulateRoot.size == 32) { "Accumulate root must be 32 bytes" }
        require(input.workPackages.size <= MAX_CORES) { "Too many work packages" }
        input.workPackages.forEachIndexed { index, pkg ->
            require(pkg.hash.size == 32) { "Work package $index must be 32 bytes" }
        }
    }

    /**
     * Append a new peak to the MMR following the mountain range rules. Each peak represents a
     * perfect binary tree of size 2^i where i is the index. When merging, we combine adjacent peaks
     * of the same size and push the result up.
     */
    private fun appendToMmr(previousMmr: HistoricalMmr, newPeak: JamByteArray): HistoricalMmr {
        val peaks = previousMmr.peaks.toMutableList()
        appendRecursive(peaks, newPeak, 0)
        return HistoricalMmr(peaks = peaks)
    }

    private fun appendRecursive(peaks: MutableList<JamByteArray?>, data: JamByteArray, index: Int) {
        when {
            // If we're beyond current peaks size, add new peak
            index >= peaks.size -> {
                peaks.add(data)
            }
            // If current position has a peak, merge and go up
            peaks[index] != null -> {
                val current = peaks[index]!!
                peaks[index] = null
                // Simply concatenate and hash the peaks
                val merged = keccakHash(current + data)
                appendRecursive(peaks, merged, index + 1)
            }
            // If current position is empty (null), place peak here
            else -> {
                peaks[index] = data
            }
        }
    }

    private fun keccakHash(data: JamByteArray): JamByteArray {
        val digest = KeccakDigest(256)
        val output = ByteArray(32)
        digest.update(data.bytes, 0, data.size)
        digest.doFinal(output, 0)
        return JamByteArray(output)
    }

    /** Calculate the beefy root from MMR peaks using the mmrsuperpeak function. */
    private fun calculateBeefyRoot(mmr: HistoricalMmr): JamByteArray {
        // Filter non-null peaks, keeping them in order
        val h = mmr.peaks.filterNotNull()
        return mmrSuperPeak(h)
    }

    private fun mmrSuperPeak(h: List<JamByteArray>): JamByteArray {
        return when {
            h.isEmpty() -> JamByteArray(ZERO_HASH)
            h.size == 1 -> h[0]
            else -> {
                // keccak($peak || superpeak(h[...len-1]) || h[len-1])
                val rest = h.dropLast(1)
                val last = h.last()
                val superPeakOfRest = mmrSuperPeak(rest)
                keccakHashWithPrefix(PEAK_PREFIX, superPeakOfRest, last)
            }
        }
    }

    private fun keccakHashWithPrefix(
            prefix: ByteArray,
            left: JamByteArray,
            right: JamByteArray
    ): JamByteArray {
        val digest = KeccakDigest(256)
        val output = ByteArray(32)
        digest.update(prefix, 0, prefix.size)
        digest.update(left.bytes, 0, left.size)
        digest.update(right.bytes, 0, right.size)
        digest.doFinal(output, 0)
        return JamByteArray(output)
    }
}
