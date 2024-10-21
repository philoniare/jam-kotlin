package io.forge.jam.core

import kotlin.test.Test
import kotlin.test.assertContentEquals

class AssuranceExtrinsicTest {

    @Test
    fun testEncodeAssuranceExtrinsics() {
        // Input assurances
        val inputAssurances = listOf(
            mapOf(
                "anchor" to "0x0cffbf67aae50aeed3c6f8f0d9bf7d854ffd87cef8358cbbaa587a9e3bd1a776",
                "bitfield" to "0x01",
                "validator_index" to 0,
                "signature" to "0x2d8ec7b235be3b3cbe9be3d5ff36f082942102d64a0dc5953709a95cca55b58b1af297f534d464264be77477b547f3c596b947edbca33f6631f1aa188d25a38b"
            ),
            mapOf(
                "anchor" to "0x2398ce69c3585e1b1b574a5a7185a2a086350abd4606d15aace8b4610b494772",
                "bitfield" to "0x01",
                "validator_index" to 1,
                "signature" to "0xdda7a577f150ee83afedc9d3b50a4f00fcf21248e6f73097abcc4bb634f854aedc53769838d294b09c0184fb0e66f09bae8cc243f842a6cc401488591e9ffdb1"
            )
        )

        // Expected output hex dump
        val expectedOutputHexDump = """
            020c ffbf 67aa e50a eed3 c6f8 f0d9 bf7d
            854f fd87 cef8 358c bbaa 587a 9e3b d1a7
            7601 0000 2d8e c7b2 35be 3b3c be9b e3d5
            ff36 f082 9421 02d6 4a0d c595 3709 a95c
            ca55 b58b 1af2 97f5 34d4 6426 4be7 7477
            b547 f3c5 96b9 47ed bca3 3f66 31f1 aa18
            8d25 a38b 2398 ce69 c358 5e1b 1b57 4a5a
            7185 a2a0 8635 0abd 4606 d15a ace8 b461
            0b49 4772 0101 00dd a7a5 77f1 50ee 83af
            edc9 d3b5 0a4f 00fc f212 48e6 f730 97ab
            cc4b b634 f854 aedc 5376 9838 d294 b09c
            0184 fb0e 66f0 9bae 8cc2 43f8 42a6 cc40
            1488 591e 9ffd b1
        """.trimIndent()

        // Parse the expected output hex dump into a byte array
        val expectedOutputBytes = parseHexDumpToByteArray(expectedOutputHexDump)

        // Process each assurance
        val encodedAssurances = inputAssurances.map { assurance ->
            val anchor = (assurance["anchor"] as String).hexToBytes()
            val bitfield = (assurance["bitfield"] as String).hexToBytes()
            val validatorIndex = assurance["validator_index"] as Int
            val signature = (assurance["signature"] as String).hexToBytes()

            val extrinsic = AssuranceExtrinsic(anchor, bitfield, validatorIndex, signature)
            extrinsic.encode()
        }

        // Version byte
        val versionByte = byteArrayOf(0x02.toByte())

        // Concatenate all encoded assurances
        val concatenatedEncodedAssurances = versionByte + encodedAssurances.reduce { acc, bytes -> acc + bytes }

        // Compare the concatenated encoded bytes with the expected output bytes
        assertContentEquals(
            expectedOutputBytes,
            concatenatedEncodedAssurances,
            "Encoded bytes do not match expected output"
        )
    }

    // Function to parse the hex dump into a byte array
    private fun parseHexDumpToByteArray(hexDump: String): ByteArray {
        // Remove all whitespaces and newlines
        val hexString = hexDump.replace("\\s".toRegex(), "")
        // Ensure the hex string has an even length
        require(hexString.length % 2 == 0) { "Hex string must have even length" }
        // Convert the hex string to a byte array
        return ByteArray(hexString.length / 2) { i ->
            val byteHex = hexString.substring(2 * i, 2 * i + 2)
            byteHex.toInt(16).toByte()
        }
    }
}
