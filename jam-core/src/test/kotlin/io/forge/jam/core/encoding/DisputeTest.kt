package io.forge.jam.core.encoding

import io.forge.jam.core.Dispute
import kotlin.test.Test
import kotlin.test.assertContentEquals

class DisputeTest {
    @Test
    fun testEncodeDispute() {
        // Load JSON data from resources using the class loader
        val (inputDispute, expectedOutputBytes) = TestFileLoader.loadTestData<Dispute>("disputes_extrinsic")

        // Process each assurance
        val versionByte = byteArrayOf(0x02)
        val encodedDispute = versionByte + inputDispute.encode()

        // Compare the concatenated encoded bytes with the expected output bytes
        assertContentEquals(
            expectedOutputBytes,
            encodedDispute,
            "Encoded bytes do not match expected output"
        )
    }
}
