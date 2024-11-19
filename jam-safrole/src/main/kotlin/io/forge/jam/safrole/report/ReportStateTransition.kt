package io.forge.jam.safrole.report

import io.forge.jam.core.GuaranteeExtrinsic
import io.forge.jam.safrole.historical.HistoricalBeta


class ReportStateTransition(private val config: ReportStateConfig) {

    /**
     * Validates the Beefy root against the MMR peaks.
     * This would need to implement the actual MMR validation logic.
     */
    private fun validateBeefyRootAgainstMmrPeaks(
        beefyRoot: ByteArray,
        mmrPeaks: List<ByteArray?>
    ): Boolean {
        // TODO: Implement proper MMR peak validation logic
        return mmrPeaks.any { peak ->
            peak?.contentEquals(beefyRoot) == true
        }
    }

    /**
     * Validates that guarantee extrinsics have valid anchors that are recent enough.
     *
     *
     * @param guarantees List of guarantees to validate
     * @param recentBlocks Recent block history (β)
     * @param currentSlot Current timeslot
     * @return ReportErrorCode if validation fails, null if successful
     */
    fun validateAnchor(
        guarantees: List<GuaranteeExtrinsic>,
        recentBlocks: List<HistoricalBeta>,
        currentSlot: Long
    ): ReportErrorCode? {
        guarantees.forEach { guarantee ->
            val context = guarantee.report.context

            // Validate lookup anchor is not too old
            if (currentSlot - context.lookupAnchorSlot > config.MAX_LOOKUP_ANCHOR_AGE) {
                return ReportErrorCode.ANCHOR_NOT_RECENT
            }

            // Find anchor block and lookup anchor block
            val lookupAnchorBlock = recentBlocks.find { it.hash.contentEquals(context.lookupAnchor) }
            if (lookupAnchorBlock == null) {
                return ReportErrorCode.ANCHOR_NOT_RECENT
            }

            val anchorBlock = recentBlocks.find { it.hash.contentEquals(context.anchor) }
            if (anchorBlock == null) {
                return ReportErrorCode.ANCHOR_NOT_RECENT
            }

            // Verify state root matches
            if (!anchorBlock.stateRoot.contentEquals(context.stateRoot)) {
                return ReportErrorCode.BAD_STATE_ROOT
            }

            // Verify MMR peaks
            val beefyRoot = context.beefyRoot
            val mmrPeaks = anchorBlock.mmr.peaks

            if (!validateBeefyRootAgainstMmrPeaks(beefyRoot, mmrPeaks)) {
                return ReportErrorCode.BAD_BEEFY_MMR_ROOT
            }

            // Validate prerequisites exist in recent history
            if (context.prerequisites.isNotEmpty()) {
                val prerequisitesExist = context.prerequisites.all { prerequisite ->
                    recentBlocks.any { block ->
                        block.reported.any { reported ->
                            reported.hash.contentEquals(prerequisite.bytes)
                        }
                    }
                }
                if (!prerequisitesExist) {
                    return ReportErrorCode.DEPENDENCY_MISSING
                }
            }
        }

        return null
    }


    fun transition(
        input: ReportInput,
        preState: ReportState
    ): Pair<ReportState, ReportOutput> {
        val postState = preState.deepCopy()

        validateAnchor(input.guarantees, preState.recentBlocks, input.slot ?: 0L)?.let {
            return Pair(postState, ReportOutput(err = it))
        }


        return Pair(postState, ReportOutput(err = ReportErrorCode.ANCHOR_NOT_RECENT))

//        return Pair(
//            postState, ReportOutput(
//                ok = ReportOutputMarks(
//
//                )
//            )
//        )
    }
}
