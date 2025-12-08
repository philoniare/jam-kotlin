package io.forge.jam.core.encoding

import io.forge.jam.core.Preimage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PreimageTest {

    private fun testEncodePreimage(configPath: String) {
        val (inputPreimages, expectedOutputBytes) = TestFileLoader.loadTestDataFromTestVectors<List<Preimage>>(configPath, "preimages_extrinsic")

        val encodedPreimages = inputPreimages.map { preimage ->
            val extrinsic =
                Preimage(preimage.requester, preimage.blob)
            extrinsic.encode()
        }

        val versionByte = byteArrayOf(0x03)
        val concatenatedEncodedPreimages = versionByte + encodedPreimages.reduce { acc, bytes -> acc + bytes }

        assertContentEquals(
            expectedOutputBytes,
            concatenatedEncodedPreimages,
            "Encoded bytes do not match expected output"
        )
    }

    private fun testDecodePreimage(configPath: String) {
        val (inputPreimages, _) = TestFileLoader.loadTestDataFromTestVectors<List<Preimage>>(configPath, "preimages_extrinsic")

        // Test each preimage individually
        for (inputPreimage in inputPreimages) {
            val encoded = inputPreimage.encode()
            val (decodedPreimage, bytesConsumed) = Preimage.fromBytes(encoded, 0)

            assertEquals(encoded.size, bytesConsumed, "Bytes consumed should match encoded size")
            assertEquals(inputPreimage.requester, decodedPreimage.requester, "Requester mismatch")
            assertEquals(inputPreimage.blob.toHex(), decodedPreimage.blob.toHex(), "Blob mismatch")

            assertContentEquals(encoded, decodedPreimage.encode(), "Round-trip encoding mismatch")
        }
    }

    @Test
    fun testEncodePreimageTiny() {
        testEncodePreimage("codec/tiny")
    }

    @Test
    fun testEncodePreimageFull() {
        testEncodePreimage("codec/full")
    }

    @Test
    fun testDecodePreimageTiny() {
        testDecodePreimage("codec/tiny")
    }

    @Test
    fun testDecodePreimageFull() {
        testDecodePreimage("codec/full")
    }
}
