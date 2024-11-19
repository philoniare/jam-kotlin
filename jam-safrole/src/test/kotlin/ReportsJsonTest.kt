package io.forge.jam.core.encoding

import io.forge.jam.safrole.report.ReportCase
import io.forge.jam.safrole.report.ReportOutput
import io.forge.jam.safrole.report.ReportState
import io.forge.jam.safrole.report.ReportStateTransition
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ReportsJsonTest {
    fun assertListOfByteArraysEqualsIgnoreOrder(
        expected: List<ByteArray>,
        actual: List<ByteArray>,
        message: String? = null
    ) {
        if (expected.size != actual.size) {
            fail(message ?: "Expected size ${expected.size} but got ${actual.size}")
        }

        val expectedCounts = expected.groupingBy { it.contentHashCode() }.eachCount()
        val actualCounts = actual.groupingBy { it.contentHashCode() }.eachCount()

        assertEquals(expectedCounts, actualCounts, message)
    }

    fun assertReportOutputEquals(expected: ReportOutput, actual: ReportOutput, testCase: String) {
        // Check error state
        if (expected.err != null) {
            assertEquals(expected.err, actual.err, "$testCase: Mismatch in error code")
        }

        // Check output marks if present
        if (expected.ok != null) {
            assertEquals(
                expected.ok != null,
                actual.ok != null,
                "$testCase: One output has null ReportOutputMarks while the other doesn't"
            )

            // If ok is present, verify all mark fields
            expected.ok?.let { expectedMarks ->
                actual.ok?.let { actualMarks ->
                    // Compare reported lists (nested ByteArrays)
                    assertEquals(
                        expectedMarks.reported.size,
                        actualMarks.reported.size,
                        "$testCase: Mismatch in reported list size"
                    )
                    for (i in expectedMarks.reported.indices) {
                        assertEquals(
                            expectedMarks.reported[i].size,
                            actualMarks.reported[i].size,
                            "$testCase: Mismatch in reported inner list size at index $i"
                        )
                        for (j in expectedMarks.reported[i].indices) {
                            assertArrayEquals(
                                expectedMarks.reported[i][j],
                                actualMarks.reported[i][j],
                                "$testCase: Mismatch in reported ByteArray at indices [$i][$j]"
                            )
                        }
                    }

                    // Compare reporters (List<ByteArray>)
                    assertEquals(
                        expectedMarks.reporters.size,
                        actualMarks.reporters.size,
                        "$testCase: Mismatch in reporters list size"
                    )
                    for (i in expectedMarks.reporters.indices) {
                        assertArrayEquals(
                            expectedMarks.reporters[i],
                            actualMarks.reporters[i],
                            "$testCase: Mismatch in reporters ByteArray at index $i"
                        )
                    }
                }
            }
        }
    }

    fun assertReportStateEquals(expected: ReportState, actual: ReportState, testCase: String) {
        // Assert availability assignments
        assertEquals(
            expected.availAssignments.size,
            actual.availAssignments.size,
            "$testCase: Mismatch in availAssignments size"
        )
        for (i in expected.availAssignments.indices) {
            assertEquals(
                expected.availAssignments[i],
                actual.availAssignments[i],
                "$testCase: Mismatch in availAssignments at index $i"
            )
        }

        // Assert current validators
        assertEquals(
            expected.currValidators.size,
            actual.currValidators.size,
            "$testCase: Mismatch in currValidators size"
        )
        for (i in expected.currValidators.indices) {
            assertEquals(
                expected.currValidators[i],
                actual.currValidators[i],
                "$testCase: Mismatch in currValidators at index $i"
            )
        }

        // Assert previous validators
        assertEquals(
            expected.prevValidators.size,
            actual.prevValidators.size,
            "$testCase: Mismatch in prevValidators size"
        )
        for (i in expected.prevValidators.indices) {
            assertEquals(
                expected.prevValidators[i],
                actual.prevValidators[i],
                "$testCase: Mismatch in prevValidators at index $i"
            )
        }

        // Assert recent blocks
        assertEquals(
            expected.recentBlocks.size,
            actual.recentBlocks.size,
            "$testCase: Mismatch in recentBlocks size"
        )
        for (i in expected.recentBlocks.indices) {
            assertEquals(
                expected.recentBlocks[i],
                actual.recentBlocks[i],
                "$testCase: Mismatch in recentBlocks at index $i"
            )
        }

        // Assert auth pools (nested ByteArray lists)
        assertEquals(
            expected.authPools.size,
            actual.authPools.size,
            "$testCase: Mismatch in authPools size"
        )
        for (i in expected.authPools.indices) {
            assertEquals(
                expected.authPools[i].size,
                actual.authPools[i].size,
                "$testCase: Mismatch in authPools inner list size at index $i"
            )
            for (j in expected.authPools[i].indices) {
                assertArrayEquals(
                    expected.authPools[i][j],
                    actual.authPools[i][j],
                    "$testCase: Mismatch in authPools at indices [$i][$j]"
                )
            }
        }

        // Assert services
        assertEquals(
            expected.services.size,
            actual.services.size,
            "$testCase: Mismatch in services size"
        )
        for (i in expected.services.indices) {
            assertEquals(
                expected.services[i].first,
                actual.services[i].first,
                "$testCase: Mismatch in service ID at index $i"
            )
            assertEquals(
                expected.services[i].second,
                actual.services[i].second,
                "$testCase: Mismatch in service data at index $i"
            )
        }
    }


    @Test
    fun testTinyReports() {
        val folderName = "reports/tiny"
        val testCases = TestFileLoader.getTestFilenamesFromResources(folderName)

        for (testCase in testCases) {
            val (inputCase) = TestFileLoader.loadTestData<ReportCase>(
                "$folderName/$testCase",
                ".bin"
            )

            val report = ReportStateTransition(
            )
            val (postState, output) = report.transition(inputCase.input, inputCase.preState)
            assertReportOutputEquals(inputCase.output, output, testCase)

            assertReportStateEquals(inputCase.postState, postState, testCase)
        }
    }

//    @Test
//    fun testFullDisputes() {
//        val folderName = "disputes/full"
//        val testCases = TestFileLoader.getTestFilenamesFromResources(folderName)
//
//        for (testCase in testCases) {
//            val (inputCase) = TestFileLoader.loadTestData<SafroleCase>(
//                "$folderName/$testCase",
//                ".bin"
//            )
//
//            val safrole = SafroleStateTransition(
//                SafroleConfig(
//                    epochLength = 600,
//                    ticketCutoff = 500,
//                    ringSize = 6,
//                    validatorCount = 1023
//                )
//            )
//            val (postState, output) = safrole.transition(inputCase.input, inputCase.preState)
//
//            assertDisputeOutputEquals(inputCase.output, output, testCase)
//
//            assertDisputeStateEquals(inputCase.postState, postState)
//        }
//    }
}
