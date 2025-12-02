package io.forge.jam.core.encoding

import io.forge.jam.core.WorkResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WorkResultTest {

    private fun testEncodeWorkResultOk(configPath: String) {
        val (inputWorkResult, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<WorkResult>(configPath, "work_result_0")

        assertContentEquals(
            expectedOutputBytes,
            inputWorkResult.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testEncodeWorkResultPanic(configPath: String) {
        val (inputWorkResult, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<WorkResult>(configPath, "work_result_1")

        assertContentEquals(
            expectedOutputBytes,
            inputWorkResult.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeWorkResultOk(configPath: String) {
        val (inputWorkResult, binaryData) = TestFileLoader.loadTestDataFromTestVectors<WorkResult>(configPath, "work_result_0")

        val (decodedWorkResult, bytesConsumed) = WorkResult.fromBytes(binaryData, 0)

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputWorkResult.serviceId, decodedWorkResult.serviceId, "Service ID mismatch")
        assertEquals(inputWorkResult.codeHash.toHex(), decodedWorkResult.codeHash.toHex(), "Code hash mismatch")
        assertEquals(inputWorkResult.result != null, decodedWorkResult.result != null, "Result presence mismatch")

        assertContentEquals(binaryData, decodedWorkResult.encode(), "Round-trip encoding mismatch")
    }

    private fun testDecodeWorkResultPanic(configPath: String) {
        val (inputWorkResult, binaryData) = TestFileLoader.loadTestDataFromTestVectors<WorkResult>(configPath, "work_result_1")

        val (decodedWorkResult, bytesConsumed) = WorkResult.fromBytes(binaryData, 0)

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputWorkResult.serviceId, decodedWorkResult.serviceId, "Service ID mismatch")

        assertContentEquals(binaryData, decodedWorkResult.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeWorkResultOkTiny() {
        testEncodeWorkResultOk("codec/tiny")
    }

    @Test
    fun testEncodeWorkResultOkFull() {
        testEncodeWorkResultOk("codec/full")
    }

    @Test
    fun testEncodeWorkResultPanicTiny() {
        testEncodeWorkResultPanic("codec/tiny")
    }

    @Test
    fun testEncodeWorkResultPanicFull() {
        testEncodeWorkResultPanic("codec/full")
    }

    @Test
    fun testDecodeWorkResultOkTiny() {
        testDecodeWorkResultOk("codec/tiny")
    }

    @Test
    fun testDecodeWorkResultOkFull() {
        testDecodeWorkResultOk("codec/full")
    }

    @Test
    fun testDecodeWorkResultPanicTiny() {
        testDecodeWorkResultPanic("codec/tiny")
    }

    @Test
    fun testDecodeWorkResultPanicFull() {
        testDecodeWorkResultPanic("codec/full")
    }
}
