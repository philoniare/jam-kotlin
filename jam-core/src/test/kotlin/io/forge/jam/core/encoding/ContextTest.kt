package io.forge.jam.core.encoding

import io.forge.jam.core.Context
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ContextTest {

    private fun testEncodeContext(configPath: String) {
        val (inputContext, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<Context>(configPath, "refine_context")

        assertContentEquals(
            expectedOutputBytes,
            inputContext.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeContext(configPath: String) {
        val (inputContext, binaryData) = TestFileLoader.loadTestDataFromTestVectors<Context>(configPath, "refine_context")

        val (decodedContext, bytesConsumed) = Context.fromBytes(binaryData, 0)

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputContext.anchor.toHex(), decodedContext.anchor.toHex(), "Anchor mismatch")
        assertEquals(inputContext.stateRoot.toHex(), decodedContext.stateRoot.toHex(), "State root mismatch")
        assertEquals(inputContext.beefyRoot.toHex(), decodedContext.beefyRoot.toHex(), "Beefy root mismatch")
        assertEquals(inputContext.lookupAnchor.toHex(), decodedContext.lookupAnchor.toHex(), "Lookup anchor mismatch")
        assertEquals(inputContext.prerequisites.size, decodedContext.prerequisites.size, "Prerequisites count mismatch")

        assertContentEquals(binaryData, decodedContext.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeContextTiny() {
        testEncodeContext("codec/tiny")
    }

    @Test
    fun testEncodeContextFull() {
        testEncodeContext("codec/full")
    }

    @Test
    fun testDecodeContextTiny() {
        testDecodeContext("codec/tiny")
    }

    @Test
    fun testDecodeContextFull() {
        testDecodeContext("codec/full")
    }
}
