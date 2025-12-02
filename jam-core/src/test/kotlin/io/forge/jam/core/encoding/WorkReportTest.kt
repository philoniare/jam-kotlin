package io.forge.jam.core.encoding

import io.forge.jam.core.WorkReport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WorkReportTest {

    private fun testEncodeWorkReport(configPath: String) {
        val (inputWorkReport, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<WorkReport>(configPath, "work_report")

        assertContentEquals(
            expectedOutputBytes,
            inputWorkReport.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeWorkReport(configPath: String) {
        val (inputWorkReport, binaryData) = TestFileLoader.loadTestDataFromTestVectors<WorkReport>(configPath, "work_report")

        val (decodedWorkReport, bytesConsumed) = WorkReport.fromBytes(binaryData, 0)

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputWorkReport.coreIndex, decodedWorkReport.coreIndex, "Core index mismatch")
        assertEquals(inputWorkReport.results.size, decodedWorkReport.results.size, "Results count mismatch")
        assertEquals(
            inputWorkReport.packageSpec.hash.toHex(),
            decodedWorkReport.packageSpec.hash.toHex(),
            "Package spec hash mismatch"
        )

        assertContentEquals(binaryData, decodedWorkReport.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeWorkReportTiny() {
        testEncodeWorkReport("codec/tiny")
    }

    @Test
    fun testEncodeWorkReportFull() {
        testEncodeWorkReport("codec/full")
    }

    @Test
    fun testDecodeWorkReportTiny() {
        testDecodeWorkReport("codec/tiny")
    }

    @Test
    fun testDecodeWorkReportFull() {
        testDecodeWorkReport("codec/full")
    }
}
