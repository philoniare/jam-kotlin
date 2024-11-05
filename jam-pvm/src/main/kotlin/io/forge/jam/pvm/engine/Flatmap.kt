package io.forge.jam.pvm.engine

/**
 * A specialized map implementation optimized for flat, dense numeric keys.
 * @param T The type of values stored in the map
 */
internal class FlatMap<T : Any> private constructor(
    private var inner: MutableList<T?>
) {
    companion object {
        /**
         * Creates a new FlatMap with the specified capacity
         */
        @JvmStatic
        fun <T : Any> new(capacity: UInt): FlatMap<T> = FlatMap(
            inner = MutableList(capacity.toInt()) { null }
        )

        /**
         * Creates a new FlatMap reusing the existing memory
         */
        @JvmStatic
        fun <T : Any> newReusingMemory(memory: FlatMap<T>, capacity: UInt): FlatMap<T> {
            memory.inner.clear()
            memory.inner.addAll(List(capacity.toInt()) { null })
            return memory
        }
    }

    /**
     * Gets a value for the given key
     */
    fun get(key: UInt): T? =
        inner.getOrNull(key.toInt())

    /**
     * Returns the number of elements the map can hold
     */
    fun len(): UInt = inner.size.toUInt()

    /**
     * Inserts a value at the specified key
     */
    fun insert(key: UInt, value: T) {
        inner[key.toInt()] = value
    }

    /**
     * Clears all elements from the map
     */
    fun clear() {
        inner.clear()
    }
}
