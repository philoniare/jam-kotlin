package io.forge.jam.pvm

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class HashTests {
    @Test
    fun `test Hash creation and equality`() {
        val bytes1 = ByteArray(32) { 1 }
        val bytes2 = ByteArray(32) { 1 }
        val bytes3 = ByteArray(32) { 2 }

        val hash1 = Hash(bytes1)
        val hash2 = Hash(bytes2)
        val hash3 = Hash(bytes3)

        assertEquals(hash1, hash2, "Identical content should produce equal hashes")
        assertNotEquals(hash1, hash3, "Different content should produce different hashes")
    }

    @Test
    fun `test Hash toString format`() {
        val bytes = ByteArray(32) { it.toByte() }
        val hash = Hash(bytes)
        val hexString = hash.toString()

        // Should be 64 characters long (32 bytes * 2 hex chars per byte)
        assertEquals(64, hexString.length)
        // Should contain only valid hex characters
        assertTrue(hexString.all { it in "0123456789abcdef" })
    }

    @Test
    fun `test Hash comparison`() {
        val hash1 = Hash(ByteArray(32) { 1 })
        val hash2 = Hash(ByteArray(32) { 2 })
        val hash3 = Hash(ByteArray(32) { 3 })

        assertTrue(hash1 < hash2)
        assertTrue(hash2 < hash3)
        assertTrue(hash1 < hash3)
    }

    @Test
    fun `test invalid Hash size`() {
        assertThrows(IllegalArgumentException::class.java) {
            Hash(ByteArray(31)) // Too short
        }
        assertThrows(IllegalArgumentException::class.java) {
            Hash(ByteArray(33)) // Too long
        }
    }
}

class HasherTests {
    @Test
    fun `test empty input produces consistent hash`() {
        val hasher1 = Hasher.new()
        val hasher2 = Hasher.new()

        val hash1 = hasher1.finalize()
        val hash2 = hasher2.finalize()

        assertEquals(hash1, hash2, "Empty input should produce consistent hash")
    }

    @Test
    fun `test different inputs produce different hashes`() {
        val hasher1 = Hasher.new()
        val hasher2 = Hasher.new()

        hasher1.update("Hello".toByteArray())
        hasher2.update("World".toByteArray())

        val hash1 = hasher1.finalize()
        val hash2 = hasher2.finalize()

        assertNotEquals(hash1, hash2, "Different inputs should produce different hashes")
    }

    @Test
    fun `test incremental updates produce same hash as single update`() {
        val input = "Hello, World!".toByteArray()

        val hasher1 = Hasher.new()
        hasher1.update(input)
        val hash1 = hasher1.finalize()

        val hasher2 = Hasher.new()
        hasher2.update(input.sliceArray(0..4))
        hasher2.update(input.sliceArray(5 until input.size))
        val hash2 = hasher2.finalize()

        assertEquals(hash1, hash2, "Incremental updates should produce same hash as single update")
    }

    @Test
    fun `test updateU32Array produces consistent results`() {
        val values = intArrayOf(1, 2, 3, 4)

        val hasher1 = Hasher.new()
        hasher1.updateU32Array(values)
        val hash1 = hasher1.finalize()

        val hasher2 = Hasher.new()
        // Manually update with same values in little-endian format
        values.forEach { value ->
            hasher2.update(
                byteArrayOf(
                    value.toByte(),
                    (value shr 8).toByte(),
                    (value shr 16).toByte(),
                    (value shr 24).toByte()
                )
            )
        }
        val hash2 = hasher2.finalize()

        assertEquals(hash1, hash2, "updateU32Array should produce same result as manual byte updates")
    }

    @Test
    fun `test hash consistency with large random input`() {
        val random = SecureRandom()
        val largeInput = ByteArray(10000)
        random.nextBytes(largeInput)

        val hasher1 = Hasher.new()
        val hasher2 = Hasher.new()

        hasher1.update(largeInput)
        // Update in chunks
        largeInput.asList().chunked(1000).forEach { chunk ->
            hasher2.update(chunk.toByteArray())
        }

        val hash1 = hasher1.finalize()
        val hash2 = hasher2.finalize()

        assertEquals(hash1, hash2, "Large input should produce consistent results regardless of chunk size")
    }
}
