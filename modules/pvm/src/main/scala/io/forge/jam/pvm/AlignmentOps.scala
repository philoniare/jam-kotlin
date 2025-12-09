package io.forge.jam.pvm

import spire.math.UInt
import scala.annotation.targetName

/**
 * Page alignment utilities for memory operations.
 */
object AlignmentOps:

  /**
   * Aligns a size up to the next page boundary.
   *
   * @param size The size to align
   * @param pageSize The page size (must be a power of 2)
   * @return The aligned size (always >= size)
   */
  def alignUp(size: UInt, pageSize: UInt): UInt =
    val ps = pageSize.toLong
    val sz = size.toLong
    UInt((((sz + ps - 1) / ps) * ps).toInt)

  /**
   * Aligns a size up to the next page boundary (Int version).
   */
  @targetName("alignUpInt")
  def alignUp(size: Int, pageSize: Int): Int =
    ((size + pageSize - 1) / pageSize) * pageSize

  /**
   * Aligns a size up to the next page boundary (Long version).
   */
  @targetName("alignUpLong")
  def alignUp(size: Long, pageSize: Long): Long =
    ((size + pageSize - 1) / pageSize) * pageSize

  /**
   * Aligns an address down to the page boundary.
   *
   * @param address The address to align
   * @param pageSize The page size (must be a power of 2)
   * @return The aligned address (always <= address)
   */
  def alignDown(address: UInt, pageSize: UInt): UInt =
    UInt(address.signed & ~(pageSize.signed - 1))

  /**
   * Aligns an address down to the page boundary (Int version).
   */
  @targetName("alignDownInt")
  def alignDown(address: Int, pageSize: Int): Int =
    address & ~(pageSize - 1)

  /**
   * Checks if an address is aligned to a page boundary.
   *
   * @param address The address to check
   * @param pageSize The page size (must be a power of 2)
   * @return true if aligned
   */
  def isAligned(address: UInt, pageSize: UInt): Boolean =
    (address.signed & (pageSize.signed - 1)) == 0

  /**
   * Checks if an address is aligned (Int version).
   */
  @targetName("isAlignedInt")
  def isAligned(address: Int, pageSize: Int): Boolean =
    (address & (pageSize - 1)) == 0

  /**
   * Gets the page number for an address.
   *
   * @param address The address
   * @param pageSize The page size
   * @return The page number (address / pageSize)
   */
  def pageNumber(address: UInt, pageSize: UInt): UInt =
    UInt((address.toLong / pageSize.toLong).toInt)

  /**
   * Gets the offset within a page for an address.
   *
   * @param address The address
   * @param pageSize The page size (must be a power of 2)
   * @return The offset within the page
   */
  def pageOffset(address: UInt, pageSize: UInt): Int =
    address.signed & (pageSize.signed - 1)

  /**
   * Calculates the number of pages needed to cover a size.
   *
   * @param size The size in bytes
   * @param pageSize The page size
   * @return Number of pages required
   */
  def pagesRequired(size: UInt, pageSize: UInt): Int =
    ((size.toLong + pageSize.toLong - 1) / pageSize.toLong).toInt
