package io.forge.jam.core.encoding.encoding

import io.forge.jam.core.encoding.TestFileLoader
import io.forge.jam.safrole.safrole.SafroleCase
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestCaseEncoding {
    @Test
    fun testEncodeWorkReport() {
        // Load JSON data from resources using the class loader
        val (testCase, expectedOutputBytes) = TestFileLoader.loadTestData<SafroleCase>("disputes/tiny/progress_invalidates_avail_assignments-1")

        // Compare the concatenated encoded bytes with the expected output bytes
        assertContentEquals(
            expectedOutputBytes,
            testCase.encode(),
            "Encoded bytes do not match expected output"
        )
    }
}

