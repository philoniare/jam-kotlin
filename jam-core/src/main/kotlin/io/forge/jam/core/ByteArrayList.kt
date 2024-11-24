package io.forge.jam.core

import io.forge.jam.core.serializers.ByteArrayListSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ByteArrayListSerializer::class)
class ByteArrayList : AbstractMutableList<EncodableByteArray>(), Encodable, Cloneable {
    private val items = mutableListOf<EncodableByteArray>()

    public override fun clone(): ByteArrayList {
        val clone = ByteArrayList()
        clone.items.addAll(items.map { it.clone() })
        return clone
    }

    override val size: Int get() = items.size

    override fun contains(element: EncodableByteArray): Boolean {
        return items.any { it.contentEquals(element) }
    }

    override fun add(element: EncodableByteArray): Boolean {
        return items.add(element.clone())
    }

    override fun add(index: Int, element: EncodableByteArray) {
        items.add(index, element.clone())
    }

    override fun removeAt(index: Int): EncodableByteArray {
        return items.removeAt(index)
    }

    override fun set(index: Int, element: EncodableByteArray): EncodableByteArray {
        return items.set(index, element.clone())
    }

    override fun get(index: Int): EncodableByteArray {
        return items[index].clone()
    }

    override fun clear() {
        items.clear()
    }

    override fun indexOf(element: EncodableByteArray): Int {
        return items.indexOfFirst { it.contentEquals(element) }
    }

    override fun lastIndexOf(element: EncodableByteArray): Int {
        return items.indexOfLast { it.contentEquals(element) }
    }

    fun toList(): List<EncodableByteArray> = items.map { it.clone() }

    override fun encode(): ByteArray {
        return items.fold(byteArrayOf()) { acc, bytes -> acc + bytes }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayList) return false
        if (size != other.size) return false
        return items.indices.all { items[it].contentEquals(other.items[it]) }
    }

    override fun hashCode(): Int {
        return items.fold(1) { acc, bytes -> 31 * acc + bytes.contentHashCode() }
    }

    override fun toString(): String {
        return items.joinToString(prefix = "[", postfix = "]") { it.toHex() }
    }
}
