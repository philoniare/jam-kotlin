package io.forge.jam.ec

import io.forge.jam.core.encoding.TestFileLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErasureCodingTest {

    companion object {
        const val TINY_VALIDATOR_COUNT = 6
        const val FULL_VALIDATOR_COUNT = 1023
    }

    @Test
    fun testTinyErasureCoding() {
        val subPath = "erasure/tiny"
        val testCases = TestFileLoader.getTestFilenamesFromTestVectors(subPath)

        assertTrue(testCases.isNotEmpty(), "No test cases found in $subPath")

        for (testCase in testCases) {
            val inputCase = TestFileLoader.loadJsonFromTestVectors<EcData>(subPath, testCase)

            // Verify data was loaded
            assertTrue(inputCase.data.bytes.isNotEmpty(), "$testCase: data should not be empty")

            // Verify shard count matches tiny validator count
            assertEquals(
                TINY_VALIDATOR_COUNT,
                inputCase.shards.size,
                "$testCase: expected $TINY_VALIDATOR_COUNT shards for tiny config"
            )
        }
    }

    @Test
    fun testFullErasureCoding() {
        val subPath = "erasure/full"
        val testCases = TestFileLoader.getTestFilenamesFromTestVectors(subPath)

        assertTrue(testCases.isNotEmpty(), "No test cases found in $subPath")

        for (testCase in testCases) {
            val inputCase = TestFileLoader.loadJsonFromTestVectors<EcData>(subPath, testCase)

            // Verify data was loaded
            assertTrue(inputCase.data.bytes.isNotEmpty(), "$testCase: data should not be empty")

            // Verify shard count matches full validator count
            assertEquals(
                FULL_VALIDATOR_COUNT,
                inputCase.shards.size,
                "$testCase: expected $FULL_VALIDATOR_COUNT shards for full config"
            )
        }
    }
}
