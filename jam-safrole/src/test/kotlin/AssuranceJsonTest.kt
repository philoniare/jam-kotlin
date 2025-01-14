package io.forge.jam.core.encoding

import io.forge.jam.safrole.assurance.AssuranceCase
import io.forge.jam.safrole.assurance.AssuranceStateTransition
import kotlin.test.Test

class AssuranceJsonTest {
    @Test
    fun testTinyStats() {
        val folderName = "assurances/tiny"
        val testCases = TestFileLoader.getTestFilenamesFromResources(folderName)

        for (testCase in testCases) {
            val (inputCase) = TestFileLoader.loadTestData<AssuranceCase>(
                "$folderName/$testCase",
                ".bin"
            )

            val stf = AssuranceStateTransition()
            val (postState, output) = stf.transition(inputCase.input, inputCase.preState)
            assertAssuranceStateEquals(inputCase.postState, postState, testCase)
        }
    }
}
