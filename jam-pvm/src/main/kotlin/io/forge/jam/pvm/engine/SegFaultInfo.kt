package io.forge.jam.pvm.engine

/**
 * Represents a segmentation fault in memory access.
 *
 * @property pageAddress The address of the page which was accessed
 * @property pageSize The size of the page
 */
data class SegfaultInfo(
    val pageAddress: UInt,
    val pageSize: UInt
) {
    override fun toString(): String =
        "Segfault(pageAddress=0x${pageAddress.toString(16)}, pageSize=${pageSize})"
}
