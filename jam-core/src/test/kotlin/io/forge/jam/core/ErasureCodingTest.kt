package io.forge.jam.core

import io.forge.jam.core.encoding.TestFileLoader
import kotlin.test.Test

class ErasureCodingTest {
    @Test
    fun testSimpleEcData() {
        val folderName = "erasure_coding/ec"
        val testCases = TestFileLoader.getTestFilenamesFromResources(folderName)

        for (testCase in testCases) {
            val inputCase = TestFileLoader.loadJsonData<EcData>(
                "$folderName/$testCase",
            )

//            val encodedChunks = JamErasureCoding.encode(inputCase.data)
//            assertEquals(inputCase.chunks.size, encodedChunks.size)
//            assertContentEquals(encodedChunks, inputCase.chunks)
        }
    }
}
