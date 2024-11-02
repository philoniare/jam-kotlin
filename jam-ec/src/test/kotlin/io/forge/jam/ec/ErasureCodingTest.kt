package io.forge.jam.ec

import io.forge.jam.core.encoding.TestFileLoader
import io.forge.jam.core.toHex
import kotlin.test.Test

class ErasureCodingTest {
    @Test
    fun testSimpleEcData() {
        val folderName = "ec"
        val testCases = TestFileLoader.getTestFilenamesFromResources(folderName)

        for (testCase in testCases) {
            val inputCase = TestFileLoader.loadJsonData<EcData>(
                "$folderName/$testCase",
            )
            println("Loaded test case: ${inputCase.data.toHex()}")

//            val encodedChunks = JamErasureCoding.encode(inputCase.data)
//            assertEquals(inputCase.chunks.size, encodedChunks.size)
//            assertContentEquals(encodedChunks, inputCase.chunks)
        }
    }
}
