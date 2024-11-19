package io.forge.jam.safrole.report

class ReportStateTransition {
    fun transition(
        input: ReportInput,
        preState: ReportState
    ): Pair<ReportState, ReportOutput> {
        val postState = preState.deepCopy()


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
