package io.forge.jam.core

import io.forge.jam.core.encoding.TestFileLoader
import merkle
import kotlin.test.Test

class MerkleTreeTest {
    @Test
    fun testTrie() {
        val testVectors = TestFileLoader.loadJsonData<List<TrieTestVector>>("trie")
        for (vector in testVectors) {
            val actualOutput = merkle(vector.input)
            println("Test: ${vector.output.toHex()} Actual: ${actualOutput.toHex()}")
            assert(vector.output.contentEquals(actualOutput))
        }
    }
}
