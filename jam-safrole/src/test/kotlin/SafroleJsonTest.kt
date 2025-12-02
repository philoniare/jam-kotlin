package io.forge.jam.core.encoding

import io.forge.jam.safrole.safrole.*
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SafroleJsonTest {
    private fun assertSafroleOutputEquals(expected: SafroleOutput, actual: SafroleOutput, testCase: String) {
        if (expected.err != null) {
            assertEquals(expected.err, actual.err, "$testCase: Mismatch in error")
        }

        if (expected.ok != null) {
            assertEquals(expected.ok!!.epochMark, actual.ok?.epochMark, "$testCase: Mismatch in EpochMark")
            assertEquals(expected.ok!!.ticketsMark, actual.ok?.ticketsMark, "$testCase: Mismatch in TicketsMark")
        }
    }

    private fun assertSafroleStateEquals(expected: SafroleState, actual: SafroleState, testCase: String) {
        assertEquals(expected.tau, actual.tau, "Mismatch in tau. TestCase: $testCase")

        assertEquals(expected.eta.size, actual.eta.size, "Mismatch in eta size. TestCase: $testCase")
        for (i in expected.eta.indices) {
            assertEquals(
                expected.eta[i],
                actual.eta[i],
                "Mismatch in eta at index $i. Expected: ${expected.eta[i].toHex()}, Actual: ${actual.eta[i].toHex()}. TestCase: $testCase"
            )
        }

        assertEquals(expected.lambda, actual.lambda, "Mismatch in lambda. TestCase: $testCase")
        assertEquals(expected.kappa, actual.kappa, "Mismatch in kappa. TestCase: $testCase")
        assertEquals(expected.gammaK, actual.gammaK, "Mismatch in gammaK. TestCase: $testCase")
        assertEquals(expected.iota, actual.iota, "Mismatch in iota. TestCase: $testCase")
        assertEquals(expected.gammaA, actual.gammaA, "Mismatch in gammaA. TestCase: $testCase")
        assertEquals(expected.gammaS, actual.gammaS, "Mismatch in gammaS. TestCase: $testCase")
        assertEquals(expected.gammaZ, actual.gammaZ, "Mismatch in gammaZ. TestCase: $testCase")
    }

    @Test
    fun testTinySafrole() {
        val folderName = "stf/safrole/tiny"
        val testCaseNames = TestFileLoader.getTestFilenamesFromTestVectors(folderName)

        for (testCaseName in testCaseNames) {
            val (testCase, expectedBinaryData) = TestFileLoader.loadTestDataFromTestVectors<SafroleCase>(
                folderName,
                testCaseName,
                ".bin"
            )
            assertContentEquals(expectedBinaryData, testCase.encode(), "Encoding mismatch for $testCaseName")

            val safrole = SafroleStateTransition(
                SafroleConfig(
                    validatorsCount = 6,
                    epochLength = 12,
                    ticketCutoff = 10,
                    ringSize = 6,
                    maxTicketAttempts = 3,
                    coresCount = 2
                )
            )
            val (postState, output) = safrole.transition(testCase.input, testCase.preState)

            // Compare the expected and actual output
            assertSafroleOutputEquals(testCase.output, output, testCaseName)

            // Compare the expected and actual post_state
            assertSafroleStateEquals(
                testCase.postState,
                postState,
                testCaseName,
            )
        }
    }

    @Test
    fun testFullSafrole() {
        val folderName = "stf/safrole/full"
        val testCaseNames = TestFileLoader.getTestFilenamesFromTestVectors(folderName)

        for (testCaseName in testCaseNames) {
            val (testCase, expectedBinaryData) = TestFileLoader.loadTestDataFromTestVectors<SafroleCase>(
                folderName,
                testCaseName,
                ".bin"
            )
            assertContentEquals(expectedBinaryData, testCase.encode(), "Encoding mismatch for $testCaseName")

            val safrole = SafroleStateTransition(
                SafroleConfig(
                    validatorsCount = 1023,
                    epochLength = 600,
                    coresCount = 341,
                    ticketCutoff = 500,
                    ringSize = 1023,
                    maxTicketAttempts = 2,
                )
            )
            val (postState, output) = safrole.transition(testCase.input, testCase.preState)

            // Compare the expected and actual output
            assertSafroleOutputEquals(testCase.output, output, testCaseName)

            // Compare the expected and actual post_state
            assertSafroleStateEquals(
                testCase.postState,
                postState,
                testCaseName
            )
        }
    }

    @Test
    fun testTinySafroleDecoding() {
        val folderName = "stf/safrole/tiny"
        val testCaseNames = TestFileLoader.getTestFilenamesFromTestVectors(folderName)

        for (testCaseName in testCaseNames) {
            val (testCase, binaryData) = TestFileLoader.loadTestDataFromTestVectors<SafroleCase>(
                folderName,
                testCaseName,
                ".bin"
            )

            // Decode from binary
            val (decodedCase, bytesConsumed) = SafroleCase.fromBytes(binaryData, 0, 6, 12)

            assertEquals(binaryData.size, bytesConsumed, "Bytes consumed mismatch for $testCaseName")
            assertEquals(testCase.input.slot, decodedCase.input.slot, "Input slot mismatch for $testCaseName")
            assertEquals(testCase.preState.tau, decodedCase.preState.tau, "PreState tau mismatch for $testCaseName")
            assertEquals(testCase.postState.tau, decodedCase.postState.tau, "PostState tau mismatch for $testCaseName")

            // Verify round-trip
            assertContentEquals(binaryData, decodedCase.encode(), "Round-trip encoding mismatch for $testCaseName")
        }
    }

    @Test
    fun testFullSafroleDecoding() {
        val folderName = "stf/safrole/full"
        val testCaseNames = TestFileLoader.getTestFilenamesFromTestVectors(folderName)

        for (testCaseName in testCaseNames) {
            val (testCase, binaryData) = TestFileLoader.loadTestDataFromTestVectors<SafroleCase>(
                folderName,
                testCaseName,
                ".bin"
            )

            // Decode from binary
            val (decodedCase, bytesConsumed) = SafroleCase.fromBytes(binaryData, 0, 1023, 600)

            assertEquals(binaryData.size, bytesConsumed, "Bytes consumed mismatch for $testCaseName")
            assertEquals(testCase.input.slot, decodedCase.input.slot, "Input slot mismatch for $testCaseName")
            assertEquals(testCase.preState.tau, decodedCase.preState.tau, "PreState tau mismatch for $testCaseName")
            assertEquals(testCase.postState.tau, decodedCase.postState.tau, "PostState tau mismatch for $testCaseName")

            // Verify round-trip
            assertContentEquals(binaryData, decodedCase.encode(), "Round-trip encoding mismatch for $testCaseName")
        }
    }
}
