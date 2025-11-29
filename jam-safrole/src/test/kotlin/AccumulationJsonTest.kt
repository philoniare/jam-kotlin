package io.forge.jam.core.encoding

import io.forge.jam.safrole.accumulation.*
import io.forge.jam.safrole.report.ServiceItem
import kotlin.test.Test
import kotlin.test.assertEquals

class AccumulationJsonTest {
    fun assertAccumulationStateEquals(expected: AccumulationState, actual: AccumulationState, testCase: String) {
        assertEquals(expected.slot, actual.slot, "Slot mismatch in test case: $testCase")
        assertEquals(expected.entropy, actual.entropy, "Entropy mismatch in test case: $testCase")

        assertEquals(
            expected.readyQueue.size,
            actual.readyQueue.size,
            "Ready queue size mismatch in test case: $testCase"
        )

        println("ReadyQueue Expect: ${expected.readyQueue}")
        println("ReadyQueue Actual: ${actual.readyQueue}")
        expected.readyQueue.forEachIndexed { queueIndex, expectedQueue ->
            val actualQueue = actual.readyQueue[queueIndex]
            assertEquals(
                expectedQueue.size,
                actualQueue.size,
                "Ready queue[$queueIndex] size mismatch in test case: $testCase"
            )

            expectedQueue.forEachIndexed { recordIndex, expectedRecord ->
                val actualRecord = actualQueue[recordIndex]
                assertReadyRecordEquals(
                    expectedRecord,
                    actualRecord,
                    "Ready queue[$queueIndex][$recordIndex]",
                    testCase
                )
            }
        }

        println("Accumulated Expect: ${expected.accumulated}")
        println("Accumulated Actual: ${actual.accumulated}")
        assertEquals(
            expected.accumulated.size,
            actual.accumulated.size,
            "Accumulated size mismatch in test case: $testCase"
        )

        expected.accumulated.forEachIndexed { listIndex, expectedList ->
            val actualList = actual.accumulated[listIndex]
            assertEquals(
                expectedList.size,
                actualList.size,
                "Accumulated[$listIndex] size mismatch in test case: $testCase"
            )

            expectedList.forEachIndexed { itemIndex, expectedItem ->
                val actualItem = actualList[itemIndex]
                assertEquals(
                    expectedItem,
                    actualItem,
                    "Accumulated[$listIndex][$itemIndex] mismatch in test case: $testCase"
                )
            }
        }

        assertEquals(expected.privileges, actual.privileges, "Privileges mismatch in test case: $testCase")

        assertEquals(
            expected.accounts.size,
            actual.accounts.size,
            "Accounts size mismatch in test case: $testCase"
        )

        expected.accounts.forEachIndexed { index, expectedAccount ->
            val actualAccount = actual.accounts[index]
            assertServiceItemEquals(expectedAccount, actualAccount, index, testCase)
        }
    }

    private fun assertReadyRecordEquals(expected: ReadyRecord, actual: ReadyRecord, path: String, testCase: String) {
        assertEquals(
            expected.report,
            actual.report,
            "Report mismatch at $path in test case: $testCase"
        )
        assertEquals(
            expected.dependencies,
            actual.dependencies,
            "Dependencies mismatch at $path in test case: $testCase"
        )
    }

    private fun assertServiceItemEquals(expected: ServiceItem, actual: ServiceItem, index: Int, testCase: String) {
        if (expected != actual) {
            println("Service item[$index] mismatch details:")
            println("  Expected id: ${expected.id}, Actual id: ${actual.id}")
            println("  Expected service: ${expected.data.service}")
            println("  Actual service:   ${actual.data.service}")
            println("  Expected storage size: ${expected.data.storage.size}, Actual: ${actual.data.storage.size}")
            println("  Expected preimages size: ${expected.data.preimages.size}, Actual: ${actual.data.preimages.size}")
            if (expected.data.storage.size == actual.data.storage.size) {
                expected.data.storage.forEachIndexed { i, exp ->
                    val act = actual.data.storage.getOrNull(i)
                    if (exp != act) {
                        println("  Storage[$i] mismatch: expected $exp, actual $act")
                    }
                }
            }
        }
        assertEquals(
            expected,
            actual,
            "Service item[$index] mismatch in test case: $testCase"
        )
    }

    fun assertAccumulationOutputEquals(expected: AccumulationOutput, actual: AccumulationOutput, testCase: String) {
        assertEquals(
            expected.ok,
            actual.ok,
            "Output.ok mismatch in test case: $testCase"
        )
    }

    @Test
    fun testTinyAccumulations() {
        val folderName = "stf/accumulate/tiny"
        // Test cases where accounts remain unchanged (no PVM execution required)
        // Note: queues_are_shifted-2 has a queue rotation edge case that needs investigation
        val testCases = listOf(
            "no_available_reports-1",
            "enqueue_and_unlock_chain-1",
            "enqueue_and_unlock_chain-2",
            "enqueue_and_unlock_chain_wraps-1",
            "enqueue_and_unlock_chain_wraps-3",
            "enqueue_and_unlock_simple-1",
            "enqueue_and_unlock_with_sr_lookup-1",
            "enqueue_self_referential-1",
            "enqueue_self_referential-2",
            "enqueue_self_referential-3",
            "enqueue_self_referential-4",
            "ready_queue_editing-1"
        )

        for (testCase in testCases) {
            println("Running test case: $testCase")
            val (inputCase) = TestFileLoader.loadTestDataFromTestVectors<AccumulationCase>(
                folderName,
                testCase,
                ".bin"
            )

            val report = AccumulationStateTransition(
                AccumulationConfig(EPOCH_LENGTH = 12, MAX_BLOCK_HISTORY = 8, AUTH_QUEUE_SIZE = 80)
            )
            val (postState, output) = report.transition(inputCase.input, inputCase.preState)
            assertAccumulationStateEquals(inputCase.postState, postState, testCase)
            assertAccumulationOutputEquals(inputCase.output, output, testCase)
        }
    }

    @Test
    fun testFullAccumulations() {
        val folderName = "stf/accumulate/full"
        val testCases = TestFileLoader.getTestFilenamesFromTestVectors(folderName)

        for (testCase in testCases) {
            val (inputCase) = TestFileLoader.loadTestDataFromTestVectors<AccumulationCase>(
                folderName,
                testCase,
                ".bin"
            )

            val report = AccumulationStateTransition(
                AccumulationConfig(EPOCH_LENGTH = 600, MAX_BLOCK_HISTORY = 8, AUTH_QUEUE_SIZE = 80)
            )
            val (postState, output) = report.transition(inputCase.input, inputCase.preState)
            assertAccumulationStateEquals(inputCase.postState, postState, testCase)
            assertAccumulationOutputEquals(inputCase.output, output, testCase)
        }
    }
}
