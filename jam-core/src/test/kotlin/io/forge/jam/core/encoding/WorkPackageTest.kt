package io.forge.jam.core.encoding

import io.forge.jam.core.WorkPackage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WorkPackageTest {

    private fun testEncodeWorkPackage(configPath: String) {
        val (inputWorkPackage, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<WorkPackage>(configPath, "work_package")

        assertContentEquals(
            expectedOutputBytes,
            inputWorkPackage.encode(),
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodeWorkPackage(configPath: String) {
        val (inputWorkPackage, binaryData) = TestFileLoader.loadTestDataFromTestVectors<WorkPackage>(configPath, "work_package")

        val (decodedWorkPackage, bytesConsumed) = WorkPackage.fromBytes(binaryData, 0)

        assertEquals(binaryData.size, bytesConsumed, "Bytes consumed should match input size")
        assertEquals(inputWorkPackage.authCodeHost, decodedWorkPackage.authCodeHost, "Auth code host mismatch")
        assertEquals(inputWorkPackage.authCodeHash.toHex(), decodedWorkPackage.authCodeHash.toHex(), "Auth code hash mismatch")
        assertEquals(inputWorkPackage.authorization.toHex(), decodedWorkPackage.authorization.toHex(), "Authorization mismatch")
        assertEquals(inputWorkPackage.authorizerConfig.toHex(), decodedWorkPackage.authorizerConfig.toHex(), "Authorizer config mismatch")
        assertEquals(inputWorkPackage.context.anchor.toHex(), decodedWorkPackage.context.anchor.toHex(), "Context anchor mismatch")
        assertEquals(inputWorkPackage.items.size, decodedWorkPackage.items.size, "Items count mismatch")

        assertContentEquals(binaryData, decodedWorkPackage.encode(), "Round-trip encoding mismatch")
    }

    @Test
    fun testEncodeWorkPackageTiny() {
        testEncodeWorkPackage("codec/tiny")
    }

    @Test
    fun testEncodeWorkPackageFull() {
        testEncodeWorkPackage("codec/full")
    }

    @Test
    fun testDecodeWorkPackageTiny() {
        testDecodeWorkPackage("codec/tiny")
    }

    @Test
    fun testDecodeWorkPackageFull() {
        testDecodeWorkPackage("codec/full")
    }
}
