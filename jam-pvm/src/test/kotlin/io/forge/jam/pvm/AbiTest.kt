package io.forge.jam.pvm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AbiTest {

    companion object {
        private const val PAGE_SIZE: UInt = 0x4000U
        private val MAX_REGION_SIZE = ((1UL shl 32) - (Abi.VM_MAX_PAGE_SIZE.toULong() * 4UL)).toUInt()
        val maxSize = (Abi.ADDRESS_SPACE_SIZE - Abi.VM_MAX_PAGE_SIZE.toULong() * 5UL).toUInt()
    }

    @Test
    fun testConstantRequirements() {
        assertDoesNotThrow {
            require(Abi.VM_MIN_PAGE_SIZE <= Abi.VM_MAX_PAGE_SIZE) { "io.forge.jam.VM_MIN_PAGE_SIZE must be less than or equal to io.forge.jam.VM_MAX_PAGE_SIZE" }
        }
        assertDoesNotThrow {
            require(Abi.VM_ADDR_RETURN_TO_HOST and 0b11U == 0U) { "io.forge.jam.VM_ADDR_RETURN_TO_HOST must be aligned to 4 bytes" }
        }
    }

    @Test
    fun testMemoryMapBasicCase() {
        val map = Abi.MemoryMapBuilder.new(PAGE_SIZE).roDataSize(1U).rwDataSize(1U).stackSize(1U).build().getOrThrow()
        assertEquals(0x10000U, map.roDataAddress())
        assertEquals(0x4000U, map.roDataSize)
        assertEquals(0x30000U, map.rwDataAddress)
        assertEquals(0x4000U, map.rwDataSize)
        assertEquals(0x4000U, map.stackSize)
        assertEquals(0xfffe0000U, map.stackAddressHigh)
        assertEquals(0xfffdc000U, map.stackAddressLow())
        assertEquals(0x30001U, map.heapBase)
        assertEquals(
            (Abi.ADDRESS_SPACE_SIZE - Abi.VM_MAX_PAGE_SIZE.toULong() * 4UL - map.heapBase.toULong()).toUInt(),
            map.maxHeapSize
        )
    }

    @Test
    fun testMemoryMapMaxReadOnlyData() {
        val map = Abi.MemoryMapBuilder.new(PAGE_SIZE)
            .roDataSize(maxSize)
            .build().getOrThrow()
        assertEquals(0x10000U, map.roDataAddress())
        assertEquals(maxSize, map.roDataSize)
        assertEquals(map.rwDataAddress, map.roDataAddress() + Abi.VM_MAX_PAGE_SIZE + maxSize)
        assertEquals(map.rwDataSize, 0u)
        assertEquals(map.stackAddressHigh, Abi.VM_ADDRESS_SPACE_TOP - Abi.VM_MAX_PAGE_SIZE)
        assertEquals(map.stackAddressLow(), Abi.VM_ADDRESS_SPACE_TOP - Abi.VM_MAX_PAGE_SIZE)
        assertEquals(map.stackSize, 0U)
        assertEquals(map.heapBase, map.rwDataAddress)
        assertEquals(map.maxHeapSize, 0U)
    }

    @Test
    fun testMemoryMapMaxReadWriteData() {
        val map = Abi.MemoryMapBuilder.new(PAGE_SIZE)
            .rwDataSize(maxSize).build().getOrThrow()
        assertEquals(map.roDataAddress(), Abi.VM_MAX_PAGE_SIZE)
        assertEquals(map.roDataSize, 0U)
        assertEquals(map.rwDataAddress, Abi.VM_MAX_PAGE_SIZE * 2U)
        assertEquals(map.rwDataSize, maxSize)
        assertEquals(map.stackAddressHigh, Abi.VM_ADDRESS_SPACE_TOP - Abi.VM_MAX_PAGE_SIZE)
        assertEquals(map.stackAddressLow(), Abi.VM_ADDRESS_SPACE_TOP - Abi.VM_MAX_PAGE_SIZE)
        assertEquals(map.stackSize, 0U)
        assertEquals(map.heapBase, map.rwDataAddress + map.rwDataSize)
        assertEquals(map.maxHeapSize, 0U)
    }

    @Test
    fun testMemoryMapMaxStack() {
        val map = Abi.MemoryMapBuilder.new(PAGE_SIZE)
            .stackSize(maxSize).build().getOrThrow()
        assertEquals(map.roDataAddress(), Abi.VM_MAX_PAGE_SIZE)
        assertEquals(map.roDataSize, 0U)
        assertEquals(map.rwDataAddress, Abi.VM_MAX_PAGE_SIZE * 2U)
        assertEquals(map.rwDataSize, 0U)
        assertEquals(map.stackAddressHigh, Abi.VM_ADDRESS_SPACE_TOP - Abi.VM_MAX_PAGE_SIZE)
        assertEquals(map.stackAddressLow(), Abi.VM_ADDRESS_SPACE_TOP - Abi.VM_MAX_PAGE_SIZE - maxSize)
        assertEquals(map.stackSize, maxSize)
        assertEquals(map.heapBase, map.rwDataAddress)
        assertEquals(map.maxHeapSize, 0U)
    }

    @Test
    fun testMemoryMapErrorCases() {
        assertFailsWith<IllegalStateException> {
            Abi.MemoryMapBuilder.new(PAGE_SIZE).roDataSize(maxSize + 1U).build().getOrThrow()
        }
        assertFailsWith<IllegalStateException> {
            Abi.MemoryMapBuilder.new(PAGE_SIZE).roDataSize(maxSize).rwDataSize(1U).build().getOrThrow()
        }
        assertFailsWith<IllegalStateException> {
            Abi.MemoryMapBuilder.new(PAGE_SIZE).roDataSize(maxSize).stackSize(1U).build().getOrThrow()
        }
    }
}
