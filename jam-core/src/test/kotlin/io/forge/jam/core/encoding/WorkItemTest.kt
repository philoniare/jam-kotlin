package io.forge.jam.core.encoding

import io.forge.jam.core.WorkItem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WorkItemTest {

    private fun testEncodeWorkItem(configPath: String) {
        val (inputWorkItem, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<WorkItem>(configPath, "work_item")

        assertContentEquals(
            expectedOutputBytes,
            inputWorkItem.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeWorkItem(configPath: String) {
        val (inputWorkItem, binaryData) = TestFileLoader.loadTestDataFromTestVectors<WorkItem>(configPath, "work_item")

        val (decodedWorkItem, bytesConsumed) = WorkItem.fromBytes(binaryData, 0)

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputWorkItem.service, decodedWorkItem.service, "Service mismatch")
        assertEquals(inputWorkItem.codeHash.toHex(), decodedWorkItem.codeHash.toHex(), "Code hash mismatch")
        assertEquals(inputWorkItem.payload.toHex(), decodedWorkItem.payload.toHex(), "Payload mismatch")
        assertEquals(inputWorkItem.refineGasLimit, decodedWorkItem.refineGasLimit, "Refine gas limit mismatch")
        assertEquals(inputWorkItem.accumulateGasLimit, decodedWorkItem.accumulateGasLimit, "Accumulate gas limit mismatch")
        assertEquals(inputWorkItem.exportCount, decodedWorkItem.exportCount, "Export count mismatch")
        assertEquals(inputWorkItem.importSegments.size, decodedWorkItem.importSegments.size, "Import segments count mismatch")
        assertEquals(inputWorkItem.extrinsic.size, decodedWorkItem.extrinsic.size, "Extrinsic count mismatch")

        assertContentEquals(binaryData, decodedWorkItem.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeWorkItemTiny() {
        testEncodeWorkItem("codec/tiny")
    }

    @Test
    fun testEncodeWorkItemFull() {
        testEncodeWorkItem("codec/full")
    }

    @Test
    fun testDecodeWorkItemTiny() {
        testDecodeWorkItem("codec/tiny")
    }

    @Test
    fun testDecodeWorkItemFull() {
        testDecodeWorkItem("codec/full")
    }
}
