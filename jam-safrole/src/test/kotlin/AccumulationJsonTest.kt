package io.forge.jam.core.encoding

import io.forge.jam.safrole.accumulation.AccumulationCase
import io.forge.jam.safrole.accumulation.AccumulationStateTransition
import kotlin.test.Test

class AccumulationJsonTest {

    @Test
    fun testTinyReports() {
        val folderName = "accumulation/tiny"
        val testCases = TestFileLoader.getTestFilenamesFromResources(folderName)

        for (testCase in testCases) {
            val (inputCase) = TestFileLoader.loadTestData<AccumulationCase>(
                "$folderName/$testCase",
                ".bin"
            )

            val report = AccumulationStateTransition(

            )
            val (postState, output) = report.transition(inputCase.input, inputCase.preState)
        }
    }
}
