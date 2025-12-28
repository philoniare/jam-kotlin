package io.forge.jam.core

import scala.collection.mutable.ArrayBuffer

/**
 * A mutable list of JamBytes items with value semantics for list operations.
 *
 * JamBytes wraps an immutable ByteVector.
 * - Content-based equality comparisons using JamBytes semantics
 * - JAM codec serialization with compact integer length prefix
 */
final class JamBytesList private (private val items: ArrayBuffer[JamBytes]) extends Cloneable:

  // ══════════════════════════════════════════════════════════════════════════
  // Size operations
  // ══════════════════════════════════════════════════════════════════════════

  /** Number of items in the list */
  def size: Int = items.size

  /** Alias for size */
  def length: Int = items.length

  /** Check if empty */
  def isEmpty: Boolean = items.isEmpty

  /** Check if non-empty */
  def nonEmpty: Boolean = items.nonEmpty

  // ══════════════════════════════════════════════════════════════════════════
  // Access operations
  // ══════════════════════════════════════════════════════════════════════════

  /** Get item at index */
  def apply(index: Int): JamBytes = items(index)

  /** Get item at index as Option */
  def get(index: Int): Option[JamBytes] =
    if index >= 0 && index < items.size then Some(items(index))
    else None

  /** Get first item */
  def head: JamBytes = items.head

  /** Get first item as Option */
  def headOption: Option[JamBytes] = items.headOption

  /** Get last item */
  def last: JamBytes = items.last

  /** Get last item as Option */
  def lastOption: Option[JamBytes] = items.lastOption

  // ══════════════════════════════════════════════════════════════════════════
  // Mutation operations
  // ══════════════════════════════════════════════════════════════════════════

  /** Add an item to the end */
  def add(element: JamBytes): Boolean =
    items += element
    true

  /** Append operator */
  def +=(element: JamBytes): this.type =
    items += element
    this

  /** Add an item at a specific index */
  def add(index: Int, element: JamBytes): Unit =
    items.insert(index, element)

  /** Add all items from another collection */
  def addAll(elements: IterableOnce[JamBytes]): Unit =
    items ++= elements

  /** Remove item at index */
  def removeAt(index: Int): JamBytes =
    items.remove(index)

  /** Set item at index, returns old value */
  def update(index: Int, element: JamBytes): JamBytes =
    val old = items(index)
    items(index) = element
    old

  /** Clear all items */
  def clear(): Unit = items.clear()

  // ══════════════════════════════════════════════════════════════════════════
  // Search operations (content-based comparison)
  // ══════════════════════════════════════════════════════════════════════════

  /** Check if contains element using content comparison */
  def contains(element: JamBytes): Boolean =
    items.exists(_ == element)

  /** Find index of element using content comparison */
  def indexOf(element: JamBytes): Int =
    items.indexWhere(_ == element)

  /** Find last index of element using content comparison */
  def lastIndexOf(element: JamBytes): Int =
    items.lastIndexWhere(_ == element)

  // ══════════════════════════════════════════════════════════════════════════
  // Iteration
  // ══════════════════════════════════════════════════════════════════════════

  /** Iterator over items */
  def iterator: Iterator[JamBytes] = items.iterator

  /** Apply function to each item */
  def foreach[U](f: JamBytes => U): Unit = items.foreach(f)

  /** Map over items */
  def map[B](f: JamBytes => B): Seq[B] = items.map(f).toSeq

  /** Fold left over items */
  def foldLeft[B](z: B)(op: (B, JamBytes) => B): B = items.foldLeft(z)(op)

  // ══════════════════════════════════════════════════════════════════════════
  // Conversion
  // ══════════════════════════════════════════════════════════════════════════

  /** Convert to immutable List */
  def toList: List[JamBytes] = items.toList

  /** Convert to immutable Seq */
  def toSeq: Seq[JamBytes] = items.toSeq

  /** Convert to Vector */
  def toVector: Vector[JamBytes] = items.toVector

  // ══════════════════════════════════════════════════════════════════════════
  // Cloning
  // ══════════════════════════════════════════════════════════════════════════

  /** Create a copy of this list (items are shared since JamBytes is immutable) */
  override def clone(): JamBytesList =
    new JamBytesList(ArrayBuffer.from(items))

  // ══════════════════════════════════════════════════════════════════════════
  // Encoding (JAM codec)
  // ══════════════════════════════════════════════════════════════════════════

  /**
   * Encode this list with compact integer length prefix.
   * Format: [compact-length][item0][item1]...
   */
  def encode(): Array[Byte] =
    import io.forge.jam.core.scodec.JamCodecs
    val builder = JamBytes.newBuilder
    builder ++= JamCodecs.encodeCompactInteger(size)
    items.foreach(item => builder ++= item.toArray)
    builder.result().toArray

  /**
   * Encode without length prefix (for fixed-size arrays).
   */
  def encodeWithoutLength(): Array[Byte] =
    val builder = JamBytes.newBuilder
    items.foreach(item => builder ++= item.toArray)
    builder.result().toArray

  // ══════════════════════════════════════════════════════════════════════════
  // Equality (content-based)
  // ══════════════════════════════════════════════════════════════════════════

  override def equals(obj: Any): Boolean = obj match
    case that: JamBytesList =>
      this.size == that.size &&
      items.indices.forall(i => items(i) == that.items(i))
    case _ => false

  override def hashCode(): Int =
    items.foldLeft(1)((acc, item) => 31 * acc + item.hashCode())

  override def toString: String =
    val itemsStr = items.take(5).map(_.toString).mkString(", ")
    val suffix = if items.size > 5 then s", ... (${items.size - 5} more)" else ""
    s"JamBytesList(size=$size, items=[$itemsStr$suffix])"

object JamBytesList:

  /** Create an empty list */
  def empty: JamBytesList = new JamBytesList(ArrayBuffer.empty)

  /** Create from varargs */
  def apply(items: JamBytes*): JamBytesList =
    new JamBytesList(ArrayBuffer.from(items))

  /** Create from a sequence */
  def fromSeq(items: Seq[JamBytes]): JamBytesList =
    new JamBytesList(ArrayBuffer.from(items))

  /** Create from an iterable */
  def from(items: IterableOnce[JamBytes]): JamBytesList =
    new JamBytesList(ArrayBuffer.from(items))

  /**
   * Decode a JamBytesList from bytes.
   *
   * @param data     The byte array to decode from
   * @param offset   Starting offset in the byte array
   * @param itemSize Size of each item in bytes (default 32 for hashes)
   * @return Tuple of (decoded list, bytes consumed)
   * @throws IllegalArgumentException if offset is negative, out of bounds, or data is insufficient
   */
  def fromBytes(data: Array[Byte], offset: Int = 0, itemSize: Int = 32): (JamBytesList, Int) =
    import io.forge.jam.core.scodec.JamCodecs
    require(offset >= 0, s"Offset must be non-negative: $offset")
    require(offset <= data.length, s"Offset $offset exceeds data length ${data.length}")
    require(itemSize >= 0, s"Item size must be non-negative: $itemSize")

    var currentOffset = offset
    val (length, lengthBytes) = JamCodecs.decodeCompactInteger(data, currentOffset)
    currentOffset += lengthBytes

    val requiredBytes = length.toInt * itemSize
    val availableBytes = data.length - currentOffset
    require(
      availableBytes >= requiredBytes,
      s"Insufficient data: need $requiredBytes bytes for $length items of size $itemSize, but only $availableBytes available"
    )

    val list = JamBytesList.empty
    for _ <- 0 until length.toInt do
      val bytes = java.util.Arrays.copyOfRange(data, currentOffset, currentOffset + itemSize)
      list.add(JamBytes(bytes))
      currentOffset += itemSize

    (list, currentOffset - offset)

  /**
   * Decode from bytes using a custom item decoder.
   *
   * @param data    The byte array to decode from
   * @param offset  Starting offset in the byte array
   * @param decoder Function that decodes one item, returns (JamBytes, bytesConsumed)
   * @return Tuple of (decoded list, bytes consumed)
   * @throws IllegalArgumentException if offset is negative or out of bounds
   */
  def fromBytesWithDecoder(
    data: Array[Byte],
    offset: Int,
    decoder: (Array[Byte], Int) => (JamBytes, Int)
  ): (JamBytesList, Int) =
    import io.forge.jam.core.scodec.JamCodecs
    require(offset >= 0, s"Offset must be non-negative: $offset")
    require(offset <= data.length, s"Offset $offset exceeds data length ${data.length}")

    var currentOffset = offset
    val (length, lengthBytes) = JamCodecs.decodeCompactInteger(data, currentOffset)
    currentOffset += lengthBytes

    val list = JamBytesList.empty
    for i <- 0 until length.toInt do
      require(
        currentOffset < data.length,
        s"Insufficient data at item $i: offset $currentOffset >= data length ${data.length}"
      )
      val (item, itemBytes) = decoder(data, currentOffset)
      require(itemBytes >= 0, s"Decoder returned negative bytes consumed: $itemBytes")
      list.add(item)
      currentOffset += itemBytes

    (list, currentOffset - offset)
